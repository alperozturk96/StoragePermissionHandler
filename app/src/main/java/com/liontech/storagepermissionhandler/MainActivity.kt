package com.liontech.storagepermissionhandler

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.liontech.storage_permission_handler.DirectoryPicker
import com.liontech.storage_permission_handler.StoragePermissionHandler
import com.liontech.storagepermissionhandler.ui.theme.StoragePermissionHandlerTheme

class MainActivity : ComponentActivity() {
    private val storageHandler by lazy {
        StoragePermissionHandler(this) { allGranted, denied ->
            if (allGranted) {
                // proceed with reading/writing storage
            } else {
                Toast.makeText(
                    this,
                    "Storage permission are denied please go to app settings to enable it",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dp = DirectoryPicker(this, this)  {
            Log.d("", "asdasd" + it.second)
        }.selectDirectory()
        enableEdgeToEdge()
        setContent {
            StoragePermissionHandlerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LaunchedEffect(Unit) {

                    }
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    StoragePermissionHandlerTheme {
        Greeting("Android")
    }
}