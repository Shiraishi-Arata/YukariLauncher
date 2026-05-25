package com.arata.yukarilauncher.utils.http

import androidx.annotation.Nullable
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.utils.file.FileUtils
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.path.UrlManager
import com.arata.yukarilauncher.Tools
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable

/** HTTPダウンロード関連のユーティリティを提供するオブジェクト。 */
object DownloadUtils {

    /**
     * URLからデータをダウンロードしてOutputStreamに書き込む。
     * @param url ダウンロード元URL
     * @param os 出力ストリーム
     */
    fun download(url: String, os: OutputStream) {
        download(URL(url), os)
    }

    /**
     * URLオブジェクトからデータをダウンロードしてOutputStreamに書き込む。
     * @param url ダウンロード元URL
     * @param os 出力ストリーム
     */
    fun download(url: URL, os: OutputStream) {
        var inputStream: InputStream? = null
        try {
            val conn = UrlManager.createHttpConnection(url)
            conn.doInput = true
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP " + conn.responseCode
                        + ": " + conn.responseMessage)
            }
            inputStream = conn.inputStream
            IOUtils.copy(inputStream, os)
        } catch (e: SocketTimeoutException) {
            throw IOException("Download timed out: $url", e)
        } catch (e: IOException) {
            throw IOException("Unable to download from $url", e)
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    Logging.e("DownloadUtils", Tools.printToString(e))
                }
            }
        }
    }

    /**
     * URLから文字列をダウンロードする。
     * @param url ダウンロード元URL
     * @return ダウンロードした文字列
     */
    @JvmStatic fun downloadString(url: String): String {
        val bos = ByteArrayOutputStream()
        download(url, bos)
        bos.close()
        return String(bos.toByteArray(), StandardCharsets.UTF_8)
    }

    /**
     * URLからファイルをダウンロードする。
     * @param url ダウンロード元URL
     * @param out 出力ファイル
     */
    fun downloadFile(url: String, out: File) {
        FileUtils.ensureParentDirectory(out)
        FileOutputStream(out).use { fileOutputStream ->
            download(url, fileOutputStream)
        }
    }

    /**
     * ファイルをダウンロードし、進捗を通知する。
     * @param urlInput ダウンロード元URL
     * @param outputFile 出力ファイル
     * @param buffer バッファ
     * @param monitor 進捗フィードバック
     */
    @JvmStatic fun downloadFileMonitored(urlInput: String, outputFile: File, @Nullable buffer: ByteArray?,
                              monitor: Tools.DownloaderFeedback) {
        FileUtils.ensureParentDirectory(outputFile)

        val conn = URL(urlInput).openConnection() as HttpURLConnection
        conn.readTimeout = UrlManager.TIME_OUT.first
        val readStr = conn.inputStream
        FileOutputStream(outputFile).use { fos ->
            var current: Int
            var overall = 0
            val length = conn.contentLength

            var buf = buffer
            if (buf == null) buf = ByteArray(65535)

            while (readStr.read(buf).also { current = it } != -1) {
                overall += current
                fos.write(buf, 0, current)
                monitor.updateProgress(overall.toLong(), length.toLong())
            }
            conn.disconnect()
        }
    }

    /**
     * 文字列をキャッシュしてダウンロードする。
     * @param url ダウンロード元URL
     * @param cacheName キャッシュファイル名
     * @param force 強制再ダウンロードフラグ
     * @param parseCallback パースコールバック
     * @return パース結果
     */
    fun <T> downloadStringCached(url: String, cacheName: String, force: Boolean, parseCallback: ParseCallback<T>): T {
        val cacheDestination = File(PathManager.DIR_CACHE_STRING, cacheName)
        if (force && cacheDestination.exists()) org.apache.commons.io.FileUtils.deleteQuietly(cacheDestination)
        if (cacheDestination.isFile && cacheDestination.canRead() &&
                YLTools.getCurrentTimeMillis() < cacheDestination.lastModified() + 86400000) {
            try {
                val cachedString = Tools.read(FileInputStream(cacheDestination))
                return parseCallback.process(cachedString)
            } catch (e: IOException) {
                Logging.i("DownloadUtils", "Failed to read the cached file", e)
            } catch (e: ParseException) {
                Logging.i("DownloadUtils", "Failed to parse the cached file", e)
            }
        }
        val urlContent = downloadString(url)
        val parseResult = parseCallback.process(urlContent)

        val tryWriteCache: Boolean
        if (cacheDestination.exists()) {
            tryWriteCache = cacheDestination.canWrite()
        } else {
            tryWriteCache = FileUtils.ensureParentDirectorySilently(cacheDestination)
        }

        if (tryWriteCache) try {
            Tools.write(cacheDestination.absolutePath, urlContent)
        } catch (e: IOException) {
            Logging.i("DownloadUtils", "Failed to cache the string", e)
        }
        return parseResult
    }

    /**
     * Callableを実行してダウンロードする内部メソッド。
     * @param downloadFunction ダウンロード処理
     * @return ダウンロード結果
     */
    private fun <T> downloadFile(downloadFunction: Callable<T>): T {
        try {
            return downloadFunction.call()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * ファイルのSHA1を検証する。
     * @param file 検証対象ファイル
     * @param sha1 期待されるSHA1ハッシュ
     * @return 一致する場合は true
     */
    private fun verifyFile(file: File, sha1: String): Boolean {
        return file.exists() && Tools.compareSHA1(file, sha1)
    }

    /**
     * SHA1検証付きでファイルをダウンロードする。検証失敗時は最大5回リトライする。
     * @param outputFile 出力ファイル
     * @param sha1 SHA1ハッシュ（nullの場合は検証スキップ）
     * @param downloadFunction ダウンロード処理
     * @return ダウンロード結果
     */
    fun <T> ensureSha1(outputFile: File, @Nullable sha1: String?, downloadFunction: Callable<T>): T? {
        if (sha1 == null) {
            if (outputFile.exists()) return null
            else return downloadFile(downloadFunction)
        }

        var attempts = 0
        var fileOkay = verifyFile(outputFile, sha1)
        var result: T? = null
        while (attempts < 5 && !fileOkay) {
            attempts++
            downloadFile(downloadFunction)
            fileOkay = verifyFile(outputFile, sha1)
        }
        if (!fileOkay) throw SHA1VerificationException("SHA1 verifcation failed after 5 download attempts")
        return result
    }

    /**
     * URLのコンテンツ長をHEADリクエストで取得する。
     * @param url 対象URL
     * @return コンテンツ長（失敗時は -1）
     */
    fun getContentLength(url: String): Long {
        val urlConnection = URL(url).openConnection() as HttpURLConnection
        urlConnection.requestMethod = "HEAD"
        urlConnection.doInput = false
        urlConnection.doOutput = false
        urlConnection.connect()
        val responseCode = urlConnection.responseCode
        if (responseCode in 200..299) return urlConnection.contentLength.toLong()
        return -1
    }

    /** ダウンロードした文字列をパースするためのコールバックインターフェース。 */
    fun interface ParseCallback<T> {
        /** @param input 入力文字列 @return パース結果 */
        fun process(input: String): T
    }

    /** パース処理中の例外。 @param e 元の例外 */
    class ParseException(e: Exception) : Exception(e)

    /** SHA1検証失敗例外。 @param message エラーメッセージ */
    class SHA1VerificationException(message: String) : IOException(message)
}
