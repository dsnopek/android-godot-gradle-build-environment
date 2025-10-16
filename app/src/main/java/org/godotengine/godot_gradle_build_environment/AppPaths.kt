package org.godotengine.godot_gradle_build_environment

import android.content.Context
import java.io.File

object AppPaths {

    const val ROOTFS_DIR = "rootfs/alpine-android-35-jdk17"
    const val ROOTFS_READY_FILENAME = ".ready"
    const val PROJECTS_DIR = "projects"

    fun getRootfs(context: Context): File =
        File(context.filesDir, ROOTFS_DIR)

    fun getRootfsReadyFile(rootfs: File): File =
        File(rootfs, ROOTFS_READY_FILENAME)

    fun getRootfsReadyFile(context: Context): File =
        getRootfsReadyFile(getRootfs(context))

    fun getProjectDir(context: Context): File =
        File(context.filesDir, PROJECTS_DIR)

}
