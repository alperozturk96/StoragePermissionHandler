package com.liontech.storage_permission_handler

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class StoragePermissionHandler(
    private val activity: ComponentActivity,
    private val onResult: (allGranted: Boolean, denied: Set<String>) -> Unit
) {

    companion object {
        private const val TAG = "StoragePermissionHandler"
    }

    private val storagePermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionResult(permissions)
        }

    private val fullDiskAccessPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->

    }

    /**
     * Requests the appropriate storage permissions at runtime.
     *
     * Make sure your AndroidManifest.xml includes:
     *  - `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="30"/>`
     *  - `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"  android:maxSdkVersion="32"/>`
     *  - `<uses-permission android:name="android.permission.READ_MEDIA_AUDIO"      android:minSdkVersion="33"/>`
     *  - `<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"      android:minSdkVersion="33"/>`
     *  - `<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"     android:minSdkVersion="33"/>`
     *  - `<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" android:minSdkVersion="34"/>`
     */
    fun requestPermissionIfNeeded() {
        val permissions = getStoragePermission()

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            Log.d(TAG, "All permissions are already granted")
            onResult(true, emptySet())
            return
        }

        storagePermissionLauncher.launch(notGranted.toTypedArray())
    }

    /**
     * Sends the user to the “All files access” settings screen for MANAGE_EXTERNAL_STORAGE.
     *
     * This permission can lead to Play Store rejection—only request it for file managers,
     * antivirus apps, document management apps, etc. For most cases, prefer SAF (Storage Access Framework).
     *
     * Ensure your AndroidManifest.xml declares:
     * <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun requestFullDiskAccess() {
        if (Environment.isExternalStorageManager()) {
            Log.d(TAG, "Full disk access permissions is already granted")
            return
        }

        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = "package:${activity.packageName}".toUri()
        }

        val safeIntent = if (intent.resolveActivity(activity.packageManager) != null) {
            intent
        } else {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }

        fullDiskAccessPermissionLauncher.launch(safeIntent)
    }

    /**
     * Requests the ACCESS_MEDIA_LOCATION permission on Android 10 (API level 29) and above.
     *
     * This permission allows the app to access the location metadata embedded in media files
     * such as photos and videos (e.g., GPS coordinates). If the permission is already granted,
     * the method logs a message and exits. Otherwise, it prompts the user to grant the permission.
     *
     * Don't forget to declare the following permission in your AndroidManifest.xml:
     * `<uses-permission android:name="android.permission.ACCESS_MEDIA_LOCATION" />`
     *
     * @param requestCode The request code to use when requesting the permission.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestAccessMediaLocation(requestCode: Int) {
        val permission = Manifest.permission.ACCESS_MEDIA_LOCATION

        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Media location permissions is already granted")
            return
        }

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            requestCode
        )
    }

    private fun getStoragePermission(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= 34 -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            }

            Build.VERSION.SDK_INT >= 33 -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            }

            Build.VERSION.SDK_INT >= 30 -> {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

            else -> {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val denied = permissions.filterValues { granted -> !granted }.keys
        if (denied.isEmpty()) {
            Log.d(TAG, "All permissions are granted")
            onResult(true, emptySet())
        } else {
            Log.w(TAG, "Permissions denied: $denied")
            onResult(false, denied)
        }
    }
}
