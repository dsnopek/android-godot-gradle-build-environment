package org.godotengine.godot_gradle_build_environment

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream

object TarXzExtractor {
    /**
     * Blocking extractor that handles dirs, files, symlinks, and hardlinks, and applies permissions.
     * - Symlinks/Hardlinks require a filesystem that supports them (usually fine under app's data dir).
     * - chmod via android.system.Os (API 21+). Fallback tries setReadable/Writable/Executable.
     */
    fun extractAssetTarXz(context: Context, assetTarXz: String, destDir: File) {
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw IllegalStateException("Could not create destination dir: ${destDir.absolutePath}")
        }

        val destRoot = destDir.canonicalFile

        context.assets.open(assetTarXz).use { raw ->
            BufferedInputStream(raw).use { buf ->
                XZCompressorInputStream(buf).use { xz ->
                    TarArchiveInputStream(xz).use { tar ->
                        var entry = tar.nextTarEntry
                        while (entry != null) {
                            val outFile = File(destDir, entry.name)
                            val outCanonical = outFile.canonicalFile

                            // Path traversal guard
                            if (!outCanonical.path.startsWith(destRoot.path + File.separator)) {
                                entry = tar.nextTarEntry
                                continue
                            }

                            when {
                                entry.isDirectory -> {
                                    if (!outCanonical.exists() && !outCanonical.mkdirs()) {
                                        throw IllegalStateException("Could not create dir: ${outCanonical.absolutePath}")
                                    }
                                    applyMode(outCanonical, entry.mode)
                                    applyMtime(outCanonical, entry.modTime.time)
                                }

                                entry.isSymbolicLink -> {
                                    // Ensure parent exists
                                    outCanonical.parentFile?.mkdirs()
                                    // Create symlink to linkName (as stored in the tar)
                                    try {
                                        // Uses Linux symlink(2). Available on API 21+.
                                        Os.symlink(entry.linkName, outCanonical.path)
                                    } catch (e: ErrnoException) {
                                        // Some filesystems/policies may forbid symlinks.
                                        // If you need an alternative, you'd have to copy or record intent.
                                        throw IllegalStateException("Failed to create symlink ${outCanonical.path} -> ${entry.linkName}: ${e.errno}", e)
                                    }
                                    // No chmod for symlink itself (POSIX typically applies mode to target).
                                    applyMtime(outCanonical, entry.modTime.time)
                                }

                                entry.isLink -> {
                                    // Hard link to another path already extracted within the archive.
                                    // Tar stores the target in linkName.
                                    outCanonical.parentFile?.mkdirs()
                                    val target = File(destDir, entry.linkName).canonicalPath
                                    try {
                                        Os.link(target, outCanonical.path)
                                    } catch (e: ErrnoException) {
                                        // Fallback: copy bytes if hard links aren’t supported or target missing.
                                        // (This loses “hardlink-ness” but preserves content.)
                                        copyFromFile(File(target), outCanonical)
                                    }
                                    applyMode(outCanonical, entry.mode)
                                    applyMtime(outCanonical, entry.modTime.time)
                                }

                                else -> {
                                    // Regular file
                                    outCanonical.parentFile?.let { if (!it.exists()) it.mkdirs() }
                                    FileOutputStream(outCanonical).use { fos ->
                                        copyStream(tar, fos)
                                    }
                                    applyMode(outCanonical, entry.mode)
                                    applyMtime(outCanonical, entry.modTime.time)
                                }
                            }

                            entry = tar.nextTarEntry
                        }
                    }
                }
            }
        }
    }

    private fun copyStream(input: TarArchiveInputStream, output: FileOutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
        }
        output.flush()
    }

    private fun copyFromFile(src: File, dst: File) {
        src.inputStream().use { `in` ->
            dst.outputStream().use { out ->
                val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val n = `in`.read(buf)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                }
            }
        }
    }

    private fun applyMtime(f: File, epochMillis: Long) {
        // Best effort: sets mtime for files/dirs/links (on links it sets the link’s mtime if supported).
        @Suppress("ResultOfMethodCallIgnored")
        f.setLastModified(epochMillis)
    }

    private fun applyMode(f: File, mode: Int) {
        // First try direct chmod (best fidelity).
        try {
            Os.chmod(f.path, mode)
            return
        } catch (_: Throwable) {
            // Fall through to a coarse fallback below.
        }

        // Fallback: approximate owner rwx only (others/group are spotty via java.io.File)
        val ownerRead = (mode and 0b100_000_000) != 0
        val ownerWrite = (mode and 0b010_000_000) != 0
        val ownerExec = (mode and 0b001_000_000) != 0

        f.setReadable(ownerRead, /*ownerOnly=*/true)
        f.setWritable(ownerWrite, /*ownerOnly=*/true)
        f.setExecutable(ownerExec, /*ownerOnly=*/true)
    }
}
