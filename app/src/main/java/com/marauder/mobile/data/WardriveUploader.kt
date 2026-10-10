package com.marauder.mobile.data

import android.util.Base64
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object WardriveUploader {

    private const val BOUNDARY = "----ESP32BOUNDARY"

    sealed interface Result {
        val message: String
        data class Success(override val message: String) : Result
        data class Failure(override val message: String) : Result
    }

    fun uploadWigle(file: File, name: String, token: String, onProgress: (Int) -> Unit): Result {
        val auth = "Basic " + Base64.encodeToString("$name:$token".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val head = "--$BOUNDARY\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n" +
            "Content-Type: application/octet-stream\r\n\r\n"
        val tail = "\r\n--$BOUNDARY\r\n" +
            "Content-Disposition: form-data; name=\"donate\"\r\n\r\non\r\n" +
            "--$BOUNDARY--\r\n"
        return post(
            urlStr = "https://api.wigle.net/api/v2/file/upload",
            headers = mapOf("Authorization" to auth),
            file = file, head = head, tail = tail, onProgress = onProgress,
            service = "WiGLE", successCode = 200, successTokens = listOf("200 OK"),
        )
    }

    fun uploadWdg(file: File, key: String, onProgress: (Int) -> Unit): Result {
        val head = "--$BOUNDARY\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n" +
            "Content-Type: application/octet-stream\r\n\r\n"
        val tail = "\r\n--$BOUNDARY--\r\n"
        return post(
            urlStr = "https://wdgwars.pl/api/v2/upload-csv",
            headers = mapOf("X-API-Key" to key),
            file = file, head = head, tail = tail, onProgress = onProgress,
            service = "WDG Wars", successCode = 202, successTokens = listOf("202 Accepted", "\"ok\":true"),
        )
    }

    private fun post(
        urlStr: String,
        headers: Map<String, String>,
        file: File,
        head: String,
        tail: String,
        onProgress: (Int) -> Unit,
        service: String,
        successCode: Int,
        successTokens: List<String>,
    ): Result {
        if (!file.isFile) return Result.Failure("$service: no wardrive log to upload")
        val headBytes = head.toByteArray(Charsets.US_ASCII)
        val tailBytes = tail.toByteArray(Charsets.US_ASCII)
        val total: Long = headBytes.size.toLong() + file.length() + tailBytes.size.toLong()
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "ESP32Uploader/1.0")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                setFixedLengthStreamingMode(total)
            }
            BufferedOutputStream(conn.outputStream).use { out ->
                out.write(headBytes)
                var sent = headBytes.size.toLong()
                file.inputStream().use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        sent += n
                        onProgress(((sent * 100) / total).toInt().coerceIn(0, 99))
                    }
                }
                out.write(tailBytes)
                out.flush()
            }
            val code = conn.responseCode
            val statusLine = "$code ${conn.responseMessage}"
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            onProgress(100)
            val ok = code == successCode ||
                successTokens.any { statusLine.contains(it) || body.contains(it) }
            if (ok) Result.Success("$service: uploaded")
            else Result.Failure("$service: ${statusLine.trim()}")
        } catch (e: Exception) {
            Result.Failure("$service: ${e.message ?: "upload failed"}")
        } finally {
            conn?.disconnect()
        }
    }
}
