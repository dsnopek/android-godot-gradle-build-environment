package org.godotengine.godot_gradle_build_environment

import android.content.Context
import android.util.Log
import java.io.File

class EmbeddedLinux(
    private val context: Context,
    private val rootfs: String
) {
    fun getGreeting(): String {
        return "Hey from lib"
    }

    fun exec(args: List<String>): String {
        val libDir = context.applicationInfo.nativeLibraryDir
        val proot = File(libDir, "libproot.so").absolutePath
        //val rootfs = File(context.filesDir, rootfs).absolutePath

        var prootTmpDir = File(context.filesDir, "proot-tmp")
        prootTmpDir.mkdirs()

        val env = HashMap(System.getenv())
        env["PROOT_TMP_DIR"] = prootTmpDir.absolutePath
        env["PROOT_LOADER"] = File(libDir, "libproot-loader.so").absolutePath
        env["PROOT_LOADER_32"] = File(libDir, "libproot-loader32.so").absolutePath

        val cmd = listOf(
            proot,
            // Do we really want `-0`?
            //"-0",
            // Should we do capital -R?
            "-r", rootfs,
            "-w", "/",
            //"-b", "/dev",
            //"-b", "/proc",
            //"-b", "/sys",
            //"/usr/bin/env", "-i",
            //"HOME=/root",
            //"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
        ) + args

        Log.i("DRS", "Cmd: " + cmd.toString())

        val process = ProcessBuilder(cmd).apply {
            directory(context.filesDir)
            environment().putAll(env)
            //redirectErrorStream()
        }.start()
        //val process = pb.start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        var error = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        Log.i("DRS", "Output: " + output)
        Log.i("DRS", "Error: " + error)
        Log.i("DRS", "ExitCode: " + exitCode.toString())

        return output
    }
}