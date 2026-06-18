package com.arata.yukarilauncher.feature.unpack

import android.content.Context
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.http.DownloadUtils
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils
import com.arata.yukarilauncher.Tools
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * JREランタイムをGitHub Releasesからダウンロードしてインストールするタスク。
 * ZIPからuniversal.tar.xzとデバイスアーキテクチャに合ったbin-*.tar.xzを抽出する。
 */
class DownloadJreTask(val context: Context, val jre: Jre) : AbstractUnpackTask() {
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            // アセットの初期化は不要
        }.getOrElse {
            isCheckFailed = true
        }
    }

/**
 * チェックが失敗したかどうかを返す
 */
    fun isCheckFailed() = isCheckFailed

/**
 * インストールが必要かどうかを判定する
 */
    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false
        val installedVersion = MultiRTUtils.readInternalRuntimeVersion(jre.jreName)
        if (installedVersion == null) {
            Logging.i("DownloadJre", "${jre.jreName}: not installed")
            return true
        }
        val runtimeDir = File(PathManager.DIR_MULTIRT_HOME, jre.jreName)
        val urlStampFile = File(runtimeDir, "download_url")
        val stampedUrl = try { urlStampFile.readText() } catch (_: Exception) { null }
        if (stampedUrl != jre.downloadUrl) {
            Logging.i("DownloadJre", "${jre.jreName}: version changed")
            return true
        }
        Logging.i("DownloadJre", "${jre.jreName}: up-to-date")
        return false
    }

/**
 * ダウンロードとインストールを実行する
 */
    override fun run() {
        listener?.onTaskStart()
        val cacheDir = PathManager.DIR_CACHE
        cacheDir.mkdirs()
        val tempZip = File(cacheDir, "${jre.jrePath}.zip")

        runCatching {
            DownloadUtils.downloadFileMonitored(jre.downloadUrl, tempZip, null, object : Tools.DownloaderFeedback {
                override fun updateProgress(curr: Long, max: Long) {
                    listener?.onProgress(curr, max)
                }
            })
            val (universalFile, binFile) = extractJreArchives(tempZip, cacheDir)
            MultiRTUtils.installRuntimeNamedBinpack(
                FileInputStream(universalFile),
                FileInputStream(binFile),
                jre.jreName,
                java.lang.Long.toString(System.currentTimeMillis())
            )
            MultiRTUtils.postPrepare(jre.jreName)
            val runtimeDir = File(PathManager.DIR_MULTIRT_HOME, jre.jreName)
            runtimeDir.mkdirs()
            File(runtimeDir, "download_url").writeText(jre.downloadUrl)
            universalFile.delete()
            binFile.delete()
            tempZip.delete()
        }.getOrElse { e ->
            Logging.e("DownloadJre", "Failed to download/extract ${jre.jreName}", e)
            tempZip.delete()
        }
        listener?.onTaskEnd()
    }

/**
 * ZIPファイルからuniversal.tar.xzとbin-{arch}.tar.xzを抽出する
 */
    private fun extractJreArchives(zipFile: File, cacheDir: File): Pair<File, File> {
        val archName = Architecture.archAsString(Tools.DEVICE_ARCHITECTURE)
        val binName = "bin-$archName.tar.xz"
        val universalFile = File(cacheDir, "${jre.jreName}_universal.tar.xz")
        val binFile = File(cacheDir, "${jre.jreName}_$binName")

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val relativePath = entry.name.removePrefix("${jre.jrePath}/")
                    if (relativePath != entry.name) {
                        when (relativePath) {
                            "universal.tar.xz" -> FileOutputStream(universalFile).use { zis.copyTo(it) }
                            binName -> FileOutputStream(binFile).use { zis.copyTo(it) }
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
        return Pair(universalFile, binFile)
    }
}
