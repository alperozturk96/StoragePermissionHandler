package com.liontech.storage_permission_handler

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile

/**
 * A helper class for launching the Android Storage Access Framework (SAF) directory picker
 * and handling the selected directory with persistable read/write permissions.
 *
 * This class uses the Activity Result API to safely request and receive a user-selected
 * directory from the system's file picker (`ACTION_OPEN_DOCUMENT_TREE`).
 *
 * The selected directory is returned as a [DocumentFile] along with its [Uri],
 * allowing your app to perform read/write operations on files within that directory,
 * even after the app restarts (as long as permissions are persisted).
 *
 * SAF is designed to provide apps secure, user-mediated access to files outside
 * their private storage, especially useful under scoped storage (Android 10+).
 *
 * @param caller The component (Activity or Fragment) that owns the ActivityResultLauncher.
 * @param context Context used for accessing content resolver and file APIs.
 * @param onDirectorySelected Callback triggered when the user selects a directory,
 *                            providing a [Pair] of [DocumentFile] and its [Uri].
 */
class DirectoryPicker(
    caller: ActivityResultCaller,
    private val context: Context,
    private val onDirectorySelected: (Pair<DocumentFile, Uri>) -> Unit
) {
    private val launcher = caller.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult

            // Persist access
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            DocumentFile.fromTreeUri(context, uri)?.let { docFile ->
                if (docFile.isDirectory) {
                    val data = Pair(docFile, uri)
                    onDirectorySelected(data)
                }
            }
        }
    }

    /**
     * Launches the system file picker UI to allow the user to select a directory.
     * The result will be delivered asynchronously via the [onDirectorySelected] callback.
     */
    fun selectDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        }

        launcher.launch(intent)
    }
}
