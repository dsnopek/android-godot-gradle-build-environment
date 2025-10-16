package org.godotengine.godot_gradle_build_environment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.godotengine.godot_gradle_build_environment.ui.theme.GodotGradleBuildEnvironmentTheme
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_MANAGE_EXTERNAL_STORAGE_REQ_CODE = 2002
    }

    private fun extractRootfs() {
        val rootfs = AppPaths.getRootfs(this)
        rootfs.mkdirs()
        TarXzExtractor.extractAssetTarXz(this, "linux-rootfs/alpine-android-35-jdk17.tar.xz", rootfs)

        // Docker doesn't let us write resolv.conf and so we take this extra unpacking step.
        val resolveConf = File(rootfs, "etc/resolv.conf")
        val resolveConfOverride = File(rootfs, "etc/resolv.conf.override")
        if (resolveConfOverride.exists()) {
            if (FileUtils.tryCopyFile(resolveConfOverride, resolveConf)) {
                resolveConfOverride.delete()
            }
        }

        AppPaths.getRootfsReadyFile(this).createNewFile()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE_REQ_CODE)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE_REQ_CODE)
                }
            }
        }

        setContent {
            GodotGradleBuildEnvironmentTheme {
                RootfsSetupScreen(
                    AppPaths.getRootfs(this),
                    AppPaths.getRootfsReadyFile(this),
                    { extractRootfs() },
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission NOT granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootfsSetupScreen(
    rootfs: File,
    rootfsReadyFile: File,
    extractRootfs: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            RootfsInstallOrDeleteButton(
                rootfs,
                rootfsReadyFile,
                extractRootfs,
            )
        }
    }
}

@Composable
fun RootfsInstallOrDeleteButton(
    rootfs: File,
    rootfsReadyFile: File,
    extractRootfs: () -> Unit,
) {
    var fileExists by remember { mutableStateOf(rootfsReadyFile.exists()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    when {
        isLoading -> {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(20.dp))
            if (fileExists) {
                Text(stringResource(R.string.deleting_rootfs_message))
            } else {
                Text(stringResource(R.string.installing_rootfs_message))
            }
        }

        !fileExists -> {
            Text(stringResource(R.string.missing_rootfs_message))
            Button(onClick = {
                isLoading = true
                scope.launch(Dispatchers.IO) {
                    try {
                        extractRootfs()

                        // Update UI state on main thread
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            fileExists = true
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { isLoading = false }
                    }
                }
            }) {
                Text(stringResource(R.string.install_rootfs_button))
            }
        }

        else -> {
            Text(stringResource(R.string.rootfs_ready_message))
            Button(onClick = {
                isLoading = true
                scope.launch(Dispatchers.IO) {
                    try {
                        rootfs.deleteRecursively()

                        withContext(Dispatchers.Main) {
                            isLoading = false
                            fileExists = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { isLoading = false }
                    }
                }
            }) {
                Text(stringResource(R.string.delete_rootfs_button))
            }
        }
    }
}
