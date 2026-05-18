package com.arata.yukarilauncher.feature.download.platform.update

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

object ModDownloader {
    private val client = OkHttpClient()

    /**
     * Download a file with progress callback.
     * @param url          Download URL
     * @param outputFile   Destination file
     * @param onProgress   Called on the background thread with percentage (0‑100)
     */
    fun downloadWithProgress(url: String, outputFile: File, onProgress: (Int) -> Unit) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code} - ${response.message}")
            }
            val body = response.body ?: throw Exception("Empty response body")
            val contentLength = body.contentLength()
            if (contentLength <= 0) {
                // If length unknown, just copy without progress
                outputFile.parentFile?.mkdirs()
                body.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return
            }

            outputFile.parentFile?.mkdirs()
            var downloaded = 0L
            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        val percent = (downloaded * 100 / contentLength).toInt()
                        onProgress(percent)
                    }
                }
            }
        }
    }
}