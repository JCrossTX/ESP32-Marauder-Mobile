package com.marauder.mobile.data

import android.content.Context
import java.io.File

class SavedWifiStore(context: Context) {

    private val appContext = context.applicationContext
    private val file: File get() = File(appContext.filesDir, "saved_wifi.tsv")

    data class SavedNetwork(val ssid: String, val password: String)

    fun list(): List<SavedNetwork> =
        runCatching {
            if (!file.exists()) emptyList()
            else file.readLines().mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                val parts = line.split('\t', limit = 2)
                SavedNetwork(parts[0], parts.getOrElse(1) { "" })
            }
        }.getOrDefault(emptyList())

    fun add(ssid: String, password: String): List<SavedNetwork> {
        val trimmed = ssid.trim()
        if (trimmed.isEmpty()) return list()
        val current = list().filterNot { it.ssid == trimmed }.toMutableList()
        current.add(SavedNetwork(trimmed, password))
        write(current)
        return current
    }

    fun remove(ssid: String): List<SavedNetwork> {
        val current = list().filterNot { it.ssid == ssid }
        write(current)
        return current
    }

    private fun write(entries: List<SavedNetwork>) {
        runCatching {
            file.writeText(entries.joinToString("\n") { "${it.ssid}\t${it.password}" })
        }
    }
}
