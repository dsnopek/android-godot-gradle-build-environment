package org.godotengine.godot_gradle_build_environment

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FileUtils {

    private const val TAG = "FileUtils"

    fun tryCopyFile(source: File, dest: File): Boolean {
        try {
            FileInputStream(source).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy ${source.absolutePath} to ${dest.absolutePath}: ${e.message}")
            return false
        }
        return true
    }

    fun tryCopyDirectory(sourceDir: File, destDir: File): Boolean {
        if (!sourceDir.isDirectory) {
            Log.e(TAG, "Source directory ${sourceDir.absolutePath} not found")
            return false
        }

        var success = true
        sourceDir.walkTopDown().forEach { source ->
            val relativePath = source.relativeTo(sourceDir)
            val target = File(destDir, relativePath.path)

            try {
                if (source.isDirectory) {
                    if (!target.exists()) {
                        target.mkdirs()
                    }
                } else {
                    if (!tryCopyFile(source, target)) {
                        success = false
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy ${source.absolutePath} -> ${target.absolutePath}: ${e.message}")
                success = false
            }
        }

        return success
    }

}
