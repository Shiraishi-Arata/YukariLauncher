package com.arata.yukarilauncher.utils.http

import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.setting.AllSettings
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.MalformedURLException


/** ダウンロードミラー（BMCLAPI等）を利用したファイル取得処理を提供するオブジェクト。 */
object DownloadMirror {
    /** ライブラリダウンロードクラス。 */
    const val DOWNLOAD_CLASS_LIBRARIES = 0
    /** メタデータダウンロードクラス。 */
    const val DOWNLOAD_CLASS_METADATA = 1
    /** アセットダウンロードクラス。 */
    const val DOWNLOAD_CLASS_ASSETS = 2

    /** URLプロトコル区切り文字。 */
    private const val URL_PROTOCOL_TAIL = "://"
    /** BMCLAPIミラー設定の配列。 */
    private val MIRROR_BMCLAPI = arrayOf(
        "https://bmclapi2.bangbang93.com/maven",
        "https://bmclapi2.bangbang93.com",
        "https://bmclapi2.bangbang93.com/assets"
    )

    /**
     * ミラー経由でファイルをダウンロードする（進捗通知付き）。
     * @param downloadClass ダウンロードクラス
     * @param urlInput 元のURL
     * @param outputFile 出力ファイル
     * @param buffer バッファ
     * @param monitor ダウンロード進捗フィードバック
     * @throws IOException ダウンロード失敗時
     */
    @JvmStatic
    @Throws(IOException::class)
    fun downloadFileMirrored(downloadClass: Int, urlInput: String, outputFile: File,
                              buffer: ByteArray?, monitor: Tools.DownloaderFeedback) {
        try {
            DownloadUtils.downloadFileMonitored(getMirrorMapping(downloadClass, urlInput),
                outputFile, buffer, monitor)
            return
        } catch (e: Exception) {
            Logging.w("DownloadMirror", "Cannot find the file on the mirror", e)
            Logging.i("DownloadMirror", "Falling back to default source")
        }
        DownloadUtils.downloadFileMonitored(urlInput, outputFile, buffer, monitor)
    }

    /**
     * ミラー経由でファイルをダウンロードする（単純版）。
     * @param downloadClass ダウンロードクラス
     * @param urlInput 元のURL
     * @param outputFile 出力ファイル
     * @throws IOException ダウンロード失敗時
     */
    @JvmStatic
    @Throws(IOException::class)
    fun downloadFileMirrored(downloadClass: Int, urlInput: String, outputFile: File) {
        try {
            DownloadUtils.downloadFile(getMirrorMapping(downloadClass, urlInput), outputFile)
            return
        } catch (e: Exception) {
            Logging.w("DownloadMirror", "Cannot find the file on the mirror", e)
            Logging.i("DownloadMirror", "Falling back to default source")
        }
        DownloadUtils.downloadFile(urlInput, outputFile)
    }

    /**
     * ミラー経由でコンテンツ長を取得する。
     * @param downloadClass ダウンロードクラス
     * @param urlInput 元のURL
     * @return コンテンツ長（取得不可の場合は -1）
     * @throws IOException 通信失敗時
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getContentLengthMirrored(downloadClass: Int, urlInput: String): Long {
        val length = DownloadUtils.getContentLength(getMirrorMapping(downloadClass, urlInput))
        return if (length < 1) {
            Logging.w("DownloadMirror", "Unable to get content length from mirror")
            Logging.i("DownloadMirror", "Falling back to default source")
            DownloadUtils.getContentLength(urlInput)
        } else {
            length
        }
    }

    /**
     * ミラー経由で文字列をダウンロードする。
     * @param downloadClass ダウンロードクラス
     * @param urlInput 元のURL
     * @return ダウンロードした文字列
     * @throws IOException ダウンロード失敗時
     */
    @JvmStatic
    @Throws(IOException::class)
    fun downloadStringMirrored(downloadClass: Int, urlInput: String): String {
        var resultString: String? = null
        try {
            resultString = DownloadUtils.downloadString(getMirrorMapping(downloadClass, urlInput))
        } catch (e: FileNotFoundException) {
            Logging.w("DownloadMirror", "Failed to download string from mirror", e)
        }
        if (Tools.isValidString(resultString)) {
            return resultString!!
        } else {
            Logging.w("DownloadMirror", "Downloaded string is invalid, falling back to default")
        }
        return DownloadUtils.downloadString(urlInput)
    }

    /** ミラー設定が有効かどうかを返す。 @return ミラーが有効な場合は true */
    @JvmStatic
    fun isMirrored(): Boolean {
        return AllSettings.downloadSource.getValue() != "default"
    }

    /**
     * ダウンロードクラスとMojang URLに対応するミラーURLを生成する。
     * @param downloadClass ダウンロードクラス
     * @param mojangUrl 元のMojang URL
     * @return ミラー変換後のURL
     * @throws MalformedURLException URL形式が不正な場合
     */
    @JvmStatic
    @Throws(MalformedURLException::class)
    private fun getMirrorMapping(downloadClass: Int, mojangUrl: String): String {
        val mirrorSettings = getMirrorSettings()
        if (mirrorSettings == null) return mojangUrl
        val urlTail = getBaseUrlTail(mojangUrl)
        var baseUrl = mojangUrl.substring(0, urlTail)
        val path = mojangUrl.substring(urlTail)
        when (downloadClass) {
            DOWNLOAD_CLASS_ASSETS, DOWNLOAD_CLASS_METADATA -> {
                baseUrl = mirrorSettings[downloadClass]
            }
            DOWNLOAD_CLASS_LIBRARIES -> {
                if (baseUrl.endsWith("libraries.minecraft.net")) {
                    baseUrl = mirrorSettings[downloadClass]
                }
            }
        }
        return baseUrl + path
    }

    /** 現在の設定に基づくミラー設定配列を返す。 @return ミラー設定、デフォルトの場合は null */
    private fun getMirrorSettings(): Array<String>? {
        when (AllSettings.downloadSource.getValue()) {
            "bmclapi" -> return MIRROR_BMCLAPI
            "default" -> return null
        }
        return null
    }

    /**
     * URLからベースURLの末尾インデックスを取得する。
     * @param wholeUrl 完全なURL
     * @return ベースURL末尾のインデックス
     * @throws MalformedURLException プロトコルまたはホスト名が欠けている場合
     */
    @Throws(MalformedURLException::class)
    private fun getBaseUrlTail(wholeUrl: String): Int {
        val protocolNameEnd = wholeUrl.indexOf(URL_PROTOCOL_TAIL)
        if (protocolNameEnd == -1)
            throw MalformedURLException("No protocol, or non path-based URL")
        var protocolNameEndVar = protocolNameEnd + URL_PROTOCOL_TAIL.length
        val hostnameEnd = wholeUrl.indexOf('/', protocolNameEndVar)
        if (protocolNameEndVar >= wholeUrl.length || hostnameEnd == protocolNameEndVar)
            throw MalformedURLException("No hostname")
        var hostnameEndVar = hostnameEnd
        if (hostnameEndVar == -1) hostnameEndVar = wholeUrl.length
        return hostnameEndVar
    }
}
