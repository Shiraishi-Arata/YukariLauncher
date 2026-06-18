package com.arata.yukarilauncher.feature.download.platform.update

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

/**
 * ファイルダウンロード機能を提供するオブジェクト
 */
object ModDownloader {
    private val client = OkHttpClient()

    /**
     * 進捗コールバック付きでファイルをダウンロードする
     * @param url ダウンロードURL
     * @param outputFile 出力先ファイル
     * @param onProgress バックグラウンドスレッドで呼び出される進捗コールバック（0〜100のパーセンテージ）
     */
/**
 * downloadWithProgressする
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
                // 長さが不明な場合は、進捗なしでコピーするだけ
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