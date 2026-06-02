package com.arata.yukarilauncher.feature.unpack

import android.content.Context
import android.os.Build
import com.arata.yukarilauncher.feature.log.Logging.i
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.http.DownloadUtils
import com.arata.yukarilauncher.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * LWJGLコンポーネントをGitHub Releasesからダウンロードして展開するタスク。
 * デバイスのABIに合ったネイティブライブラリのみを抽出する。
 */
class DownloadComponentsTask(val context: Context, val component: Components) : AbstractUnpackTask() {
    private lateinit var rootDir: String
    private lateinit var targetDir: File
    private lateinit var versionFile: File
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            rootDir = if (component.privateDirectory) PathManager.DIR_DATA else PathManager.DIR_GAME_HOME
            targetDir = File(rootDir, component.component)
            versionFile = File(targetDir, "version")
        }.getOrElse {
            isCheckFailed = true
        }
    }

/**
 * チェックが失敗したかどうかを返す
 */
    fun isCheckFailed() = isCheckFailed

/**
 * アンパックが必要かどうかを判定する
 */
    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false
        if (!versionFile.exists()) {
            i("DownloadComponents", "${component.component}: not installed")
            return true
        }
        val installedUrl = try { Tools.read(versionFile) } catch (_: Exception) { null }
        if (installedUrl != component.downloadUrl) {
            i("DownloadComponents", "${component.component}: version changed")
            return true
        }
        if (isLwjglComponentMissingNative()) {
            i("DownloadComponents", "${component.component}: missing native libs")
            return true
        }
        i("DownloadComponents", "${component.component}: up-to-date")
        return false
    }

/**
 * LWJGLコンポーネントのネイティブライブラリが不足しているかチェックする
 */
    private fun isLwjglComponentMissingNative(): Boolean {
        if (!component.component.startsWith("lwjgl/")) return false
        val supportedAbis = Build.SUPPORTED_ABIS.takeIf { it.isNotEmpty() } ?: arrayOf("arm64-v8a")
        return !supportedAbis.any { abi ->
            File(targetDir, "native/$abi/liblwjgl.so").exists()
        }
    }

/**
 * ダウンロードと展開を実行する
 */
    override fun run() {
        listener?.onTaskStart()
        val downloadUrl = component.downloadUrl ?: run {
            i("DownloadComponents", "${component.component}: no download URL")
            listener?.onTaskEnd()
            return
        }
        val cacheDir = PathManager.DIR_CACHE
        cacheDir.mkdirs()
        val tempZip = File(cacheDir, "${component.component.replace("/", "_")}.zip")

        runCatching {
            DownloadUtils.downloadFileMonitored(downloadUrl, tempZip, null, object : Tools.DownloaderFeedback {
                override fun updateProgress(curr: Long, max: Long) {
                    listener?.onProgress(curr, max)
                }
            })
            requestEmptyParentDir(versionFile)
            extractZip(tempZip, targetDir)
            versionFile.writeText(downloadUrl)
            tempZip.delete()
        }.getOrElse { e ->
            i("DownloadComponents", "Failed to download/extract ${component.component}", e)
            tempZip.delete()
        }
        listener?.onTaskEnd()
    }

/**
 * ZIPファイルを展開し、デバイスのABIに合ったファイルのみを抽出する
 */
    private fun extractZip(zipFile: File, targetDir: File) {
        val zipRoot = component.component.substringAfterLast("/")
        val supportedAbis = Build.SUPPORTED_ABIS.toSet()

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                val relativePath = entryName.removePrefix("$zipRoot/")
                if (!entry.isDirectory && relativePath != entryName && relativePath.isNotEmpty()) {
                    val shouldExtract = if (relativePath.startsWith("native/")) {
                        val abi = relativePath.removePrefix("native/").substringBefore("/")
                        abi in supportedAbis
                    } else true
                    if (shouldExtract) {
                        val outputFile = File(targetDir, relativePath)
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

/**
 * 親ディレクトリを空にして再作成する
 */
    private fun requestEmptyParentDir(file: File) {
        file.parentFile?.apply {
            if (exists() && isDirectory) {
                FileUtils.deleteDirectory(this)
            }
            mkdirs()
        }
    }
}
