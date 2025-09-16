package org.godotengine.godot_gradle_build_environment

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.godotengine.godot_gradle_build_environment.ui.theme.GodotGradleBuildEnvironmentTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Extract the rootfs
        val debianRootfs = File(filesDir, "rootfs/alpine-android-35-jdk17")
        if (!debianRootfs.exists()) {
            debianRootfs.mkdirs()
            TarXzExtractor.extractAssetTarXz(this, "linux-rootfs/alpine-android-35-jdk17.tar.xz", debianRootfs)
        }

        // Debug code:
        val libDir = applicationInfo.nativeLibraryDir
        Log.i("Check", "nativeLibraryDir = $libDir")
        File(libDir).listFiles()?.forEach { f ->
            Log.i("Check", " - ${f.name} size=${f.length()} exec=${f.canExecute()}")
        }

        var el = EmbeddedLinux(this, debianRootfs.absolutePath)
        
        setContent {
            GodotGradleBuildEnvironmentTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        //name = "Android",
                        name = el.exec(listOf("/bin/ls", "-l", "/")),
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
    GodotGradleBuildEnvironmentTheme {
        Greeting("Android")
    }
}