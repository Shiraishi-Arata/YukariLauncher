package com.arata.yukarilauncher.feature.mod.modloader

import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.mod.modloader.NeoForgeUtils.Companion.downloadNeoForgeVersions
import com.arata.yukarilauncher.feature.mod.modloader.NeoForgeUtils.Companion.downloadNeoForgedForgeVersions
import com.arata.yukarilauncher.feature.mod.modloader.NeoForgeUtils.Companion.getNeoForgeInstallerUrl
import com.arata.yukarilauncher.feature.mod.modloader.NeoForgeUtils.Companion.getNeoForgedForgeInstallerUrl
import com.arata.yukarilauncher.feature.version.install.InstallTask
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.Tools.DownloaderFeedback
import com.arata.yukarilauncher.task.ProgressKeeper
import com.arata.yukarilauncher.utils.http.DownloadUtils
import java.io.File
import java.io.IOException

class NeoForgeDownloadTask(neoforgeVersion: String) : InstallTask, DownloaderFeedback {
    private var mDownloadUrl: String? = null
    private var mLoaderVersion: String? = neoforgeVersion
    private var mGameVersion: String? = null

    /**
     * NeoForgeのダウンロードタスクを初期化する
     * バージョン情報とダウンロードURLを設定する
     */
    init {
        mGameVersion = NeoForgeUtils.formatGameVersion(neoforgeVersion)
        if (neoforgeVersion.contains("1.20.1")) {
            mDownloadUrl = getNeoForgedForgeInstallerUrl(neoforgeVersion)
        } else {
            mDownloadUrl = getNeoForgeInstallerUrl(neoforgeVersion)
        }
        Logging.i("NeoForgeDownloadTask", "Version: $mLoaderVersion, Game: $mGameVersion, URL: $mDownloadUrl")
    }

    @Throws(Exception::class)
/**
 * runする
 */
    override fun run(customName: String): File? {
        var outputFile: File? = null
        if (if (mLoaderVersion!!.contains("1.20.1")) determineNeoForgedForgeDownloadUrl() else determineNeoForgeDownloadUrl()) {
            outputFile = downloadNeoForge()
        }
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
        return outputFile
    }

/**
 * updateProgressする
 */
    override fun updateProgress(curr: Long, max: Long) {
        val progress100 = ((curr.toFloat() / max.toFloat()) * 100f).toInt()
        ProgressKeeper.submitProgress(
            ProgressLayout.INSTALL_RESOURCE,
            progress100,
            R.string.mod_download_progress,
            mLoaderVersion
        )
    }

    @Throws(Exception::class)
/**
 * downloadNeoForgeする
 */
    private fun downloadNeoForge(): File {
        ProgressKeeper.submitProgress(
            ProgressLayout.INSTALL_RESOURCE,
            0,
            R.string.mod_download_progress,
            mLoaderVersion
        )
        val destinationFile = File(PathManager.DIR_CACHE, "neoforge-installer.jar")
        val buffer = ByteArray(8192)
        DownloadUtils.downloadFileMonitored(mDownloadUrl!!, destinationFile, buffer, this)
        return destinationFile
    }

/**
 * determineDownloadUrlする
 */
    private fun determineDownloadUrl(findVersion: Boolean): Boolean {
        if (mDownloadUrl != null && mLoaderVersion != null) return true
        ProgressKeeper.submitProgress(
            ProgressLayout.INSTALL_RESOURCE,
            0,
            R.string.mod_neoforge_searching
        )
        if (!findVersion) {
            throw IOException("Version not found")
        }
        return true
    }

    @Throws(Exception::class)
/**
 * determineNeoForgeDownloadUrlする
 */
    fun determineNeoForgeDownloadUrl(): Boolean {
        return if (findNeoForgeVersion()) {
            true
        } else {
            // Fallback: keep the originally constructed URL (works for snapshots not yet in metadata)
            if (mDownloadUrl != null) true else false
        }
    }

    @Throws(Exception::class)
/**
 * determineNeoForgedForgeDownloadUrlする
 */
    fun determineNeoForgedForgeDownloadUrl(): Boolean {
        return if (findNeoForgedForgeVersion()) {
            true
        } else {
            if (mDownloadUrl != null) true else false
        }
    }

/**
 * findVersionする
 */
    private fun findVersion(neoForgeUtils: List<String>?, installerUrl: String): Boolean {
        if (neoForgeUtils == null) return false
        val versionStart = "$mGameVersion-$mLoaderVersion"
        for (versionName in neoForgeUtils) {
            // NeoForge 26.x maven entries have no Minecraft prefix (e.g. "26.1.0"),
            // while older entries do (e.g. "1.20.1-47.1.0"). Match both forms.
            if (versionName.startsWith(versionStart) || versionName.startsWith(mLoaderVersion!!)) {
                mLoaderVersion = versionName
                mDownloadUrl = installerUrl
                return true
            }
        }
        return false
    }

    @Throws(Exception::class)
/**
 * findNeoForgeVersionする
 */
    fun findNeoForgeVersion(): Boolean {
        return findVersion(downloadNeoForgeVersions(false), getNeoForgeInstallerUrl(mLoaderVersion))
    }

    @Throws(Exception::class)
/**
 * findNeoForgedForgeVersionする
 */
    fun findNeoForgedForgeVersion(): Boolean {
        return findVersion(
            downloadNeoForgedForgeVersions(false),
            getNeoForgedForgeInstallerUrl(mLoaderVersion)
        )
    }
}