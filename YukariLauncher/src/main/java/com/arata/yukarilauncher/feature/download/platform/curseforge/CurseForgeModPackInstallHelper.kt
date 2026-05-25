package com.arata.yukarilauncher.feature.download.platform.curseforge

import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.download.enums.ModLoader
import com.arata.yukarilauncher.feature.download.install.InstallHelper
import com.arata.yukarilauncher.feature.download.item.ModLoaderWrapper
import com.arata.yukarilauncher.feature.download.item.VersionItem
import com.arata.yukarilauncher.feature.download.platform.curseforge.CurseForgeCommonUtils.Companion.getDownloadSha1
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.mod.modpack.install.ModPackUtils
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.mod.modpack.api.ApiHandler
import com.arata.yukarilauncher.feature.mod.modpack.api.ModDownloader
import com.arata.yukarilauncher.feature.mod.modpack.models.CurseManifest
import com.arata.yukarilauncher.feature.mod.modpack.models.CurseManifest.CurseMinecraft
import com.arata.yukarilauncher.feature.mod.modpack.models.CurseManifest.CurseModLoader
import com.arata.yukarilauncher.task.ProgressKeeper
import com.arata.yukarilauncher.task.SpeedCalculator
import com.arata.yukarilauncher.utils.file.FileUtils
import com.arata.yukarilauncher.utils.file.ZipUtils
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import kotlin.math.max

class CurseForgeModPackInstallHelper {
    companion object {
        /**
         * CurseForgeのModパックインストールを開始する
         * @param api APIハンドラー
         * @param versionItem インストールするバージョン情報
         * @param customName カスタムバージョン名
         * @return ModLoaderのラッパー情報
         */
        @Throws(Exception::class)
/**
 * startInstallする
 */
        fun startInstall(api: ApiHandler, versionItem: VersionItem, customName: String): ModLoaderWrapper? {
            return InstallHelper.installModPack(versionItem, customName) { modpackFile, targetPath ->
                installZip(api, modpackFile, targetPath)
            }
        }

        /**
         * ModパックのZIPファイルを解析してインストールを実行する
         * @param api APIハンドラー
         * @param zipFile ModパックのZIPファイル
         * @param targetPath インストール先のパス
         * @return ModLoaderのラッパー情報
         */
        @Throws(Exception::class)
/**
 * installZipする
 */
        fun installZip(api: ApiHandler, zipFile: File, targetPath: File): ModLoaderWrapper? {
            ZipFile(zipFile).use { modpackZipFile ->
                val curseManifest = Tools.GLOBAL_GSON.fromJson(
                    Tools.read(ZipUtils.getEntryStream(modpackZipFile, "manifest.json")),
                    CurseManifest::class.java
                )
                if (!ModPackUtils.verifyManifest(curseManifest)) {
                    Logging.i("CurseForgeModPackInstallHelper", "manifest verification failed")
                    return null
                }
                var progressUpdateTime = 0L
                val speedCalculator = SpeedCalculator()
                val modDownloader: ModDownloader = getModDownloader(api, targetPath, curseManifest)
                modDownloader.awaitFinish(object : ModDownloader.DownloadProgressListener {
                    override fun feedback(count: Int, totalCount: Int, downloadedSize: Long) {
                        val currentTime = YLTools.getCurrentTimeMillis()
                        if (currentTime - progressUpdateTime < 150) return
                        progressUpdateTime = currentTime

                        ProgressKeeper.submitProgress(
                            ProgressLayout.INSTALL_RESOURCE,
                            max((count.toFloat() / totalCount * 100).toDouble(), 0.0).toInt(),
                            R.string.modpack_download_downloading_mods_fc,
                            count,
                            FileTools.formatFileSize(downloadedSize),
                            totalCount,
                            FileTools.formatFileSize(speedCalculator.feed(downloadedSize))
                        )
                    }
                })
                val overridesDir: String = curseManifest.overrides ?: "overrides"
                ZipUtils.zipExtract(modpackZipFile, overridesDir, targetPath)
                return createInfo(curseManifest.minecraft!!)
            }
        }

        /**
         * Modのダウンローダーを生成し、マニフェストに従ってダウンロードタスクを設定する
         * @param api APIハンドラー
         * @param instanceDestination インスタンスの出力先ディレクトリ
         * @param curseManifest CurseForgeのマニフェスト
         * @return 設定済みのModDownloader
         */
        @Throws(Exception::class)
/**
 * getModDownloaderする
 */
        private fun getModDownloader(
            api: ApiHandler,
            instanceDestination: File,
            curseManifest: CurseManifest
        ): ModDownloader {
            val modDownloader = ModDownloader(File(instanceDestination, "mods"), true)
            val files = curseManifest.files ?: return modDownloader
            val fileCount = files.size
            for (i in 0 until fileCount) {
                val curseFile = files[i]
                modDownloader.submitDownload(object : ModDownloader.FileInfoProvider {
                    override fun getFileInfo(): ModDownloader.FileInfo? {
                        val url = CurseForgeCommonUtils.getDownloadUrl(api, curseFile.projectID, curseFile.fileID)
                        if (url == null && curseFile.required) throw IOException(
                            "Failed to obtain download URL for ${StringUtils.insertSpace(curseFile.projectID, curseFile.fileID)}"
                        )
                        else if (url == null) return null
                        return ModDownloader.FileInfo(url, FileUtils.getFileName(url) ?: "", getDownloadSha1(api, curseFile.projectID, curseFile.fileID))
                    }
                })
            }
            return modDownloader
        }

        /**
         * CurseManifestのMinecraft情報からModLoaderWrapperを生成する
         * @param minecraft CurseManifestのMinecraft情報
         * @return ModLoaderのラッパー情報、またはnull
         */
/**
 * createInfoする
 */
        private fun createInfo(minecraft: CurseMinecraft): ModLoaderWrapper? {
            val modLoaders = minecraft.modLoaders ?: return null
            var primaryModLoader: CurseModLoader? = null
            for (modLoader in modLoaders) {
                if (modLoader.primary) {
                    primaryModLoader = modLoader
                    break
                }
            }
            if (primaryModLoader == null) primaryModLoader = modLoaders[0]
            val modLoaderId = primaryModLoader!!.id ?: return null
            val dashIndex = modLoaderId.indexOf('-')
            val modLoaderName = modLoaderId.substring(0, dashIndex)
            val modLoaderVersion = modLoaderId.substring(dashIndex + 1)
            Logging.i("CurseForgeModPackInstallHelper",
                StringUtils.insertSpace(modLoaderId, modLoaderName, modLoaderVersion)
            )
            val modloader: ModLoader
            when (modLoaderName) {
                "forge" -> {
                    Logging.i("ModLoader", "Forge, or Quilt? ...")
                    modloader = ModLoader.FORGE
                }
                "neoforge" -> {
                    Logging.i("ModLoader", "NeoForge")
                    modloader = ModLoader.NEOFORGE
                }
                "fabric" -> {
                    Logging.i("ModLoader", "Fabric")
                    modloader = ModLoader.FABRIC
                }
                else -> return null
            }
            return ModLoaderWrapper(modloader, modLoaderVersion, minecraft.version ?: return null)
        }
    }
}
