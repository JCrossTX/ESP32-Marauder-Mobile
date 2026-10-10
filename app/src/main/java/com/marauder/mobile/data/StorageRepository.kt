package com.marauder.mobile.data

import android.content.Context
import java.io.File

class StorageRepository(context: Context) {

    private val appContext = context.applicationContext

    val root: File
        get() = File(appContext.getExternalFilesDir(null), "captures").apply { mkdirs() }

    data class StoredFile(
        val name: String,
        val path: String,
        val size: Long,
        val modified: Long,
        val kind: String,
    )

    fun files(): List<StoredFile> =
        root.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { StoredFile(it.name, it.absolutePath, it.length(), it.lastModified(), kindOf(it.name)) }
            ?: emptyList()

    fun totalBytes(): Long =
        root.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

    fun delete(path: String): Boolean = runCatching {
        val f = File(path)
        if (f.parentFile?.canonicalFile == root.canonicalFile && f.isFile) f.delete() else false
    }.getOrDefault(false)

    private fun kindOf(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".pcap") -> "pcap"
            lower.endsWith(".gpx") -> "gpx"
            lower.endsWith(".html") || lower.endsWith(".htm") -> "html"
            lower.endsWith(".log") || lower.startsWith("wardrive") || lower.startsWith("wigle") -> "wardrive"
            else -> "file"
        }
    }
}
