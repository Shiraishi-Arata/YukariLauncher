package com.arata.yukarilauncher.feature.download.platform.modrinth

import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.download.enums.ModLoader
import com.arata.yukarilauncher.feature.download.install.InstallHelper
import com.arata.yukarilauncher.feature.download.item.ModLoaderWrapper
import com.arata.yukarilauncher.feature.download.item.VersionItem
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.mod.modpack.install.ModPackUtils.Companion.verifyModrinthIndex
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.mod.modpack.api.ModDownloader
import com.arata.yukarilauncher.feature.mod.modpack.models.ModrinthIndex
import com.arata.yukarilauncher.task.DownloaderProgressWrapper
import com.arata.yukarilauncher.utils.file.ZipUtils
import java.io.File
import java.util.zip.ZipFile

class ModrinthModPackInstallHelper {
    companion object {
        /**
         * ModrinthのModパックインストールを開始する
         * @param versionItem インストールするバージョン情報
         * @param customName カスタムバージョン名
         * @return ModLoaderのラッパー情報
         */
        @Throws(Exception::class)
/**
 * startInstallする
 */
        fun startInstall(versionItem: VersionItem, customName: String): ModLoaderWrapper? {
            return InstallHelper.installModPack(versionItem, customName) { modpackFile, targetPath ->
                installZip(modpackFile, targetPath)
            }
        }

        /**
         * ModrinthのModパックZIPファイルを解析してインストールを実行する
         * @param packFile ModパックのZIPファイル
         * @param targetPath インストール先のパス
         * @return ModLoaderのラッパー情報
         */
        @Throws(Exception::class)
/**
 * installZipする
 */
        fun installZip(packFile: File, targetPath: File): ModLoaderWrapper? {
            val result = ZipFile(packFile).use { modpackZipFile ->
                val modrinthIndex = Tools.GLOBAL_GSON.fromJson(
                    Tools.read(ZipUtils.getEntryStream(modpackZipFile, "modrinth.index.json")),
                    ModrinthIndex::class.java
                )
                if (!verifyModrinthIndex(modrinthIndex)) {
                    Logging.i("ModrinthModPackInstallHelper", "manifest verification failed")
                    return@use null
                }
                val modDownloader = ModDownloader(targetPath)
                val files = modrinthIndex.files ?: return@use null
                for (indexFile in files) {
                    modDownloader.submitDownload(
                        indexFile.fileSize,
                        indexFile.path ?: continue,
                        indexFile.hashes?.sha1,
                        *(indexFile.downloads ?: arrayOf())
                    )
                }
                modDownloader.awaitFinish(
                    DownloaderProgressWrapper(
                        R.string.modpack_download_downloading_mods,
                        ProgressLayout.INSTALL_RESOURCE
                    )
                )
                ProgressLayout.setProgress(
                    ProgressLayout.INSTALL_RESOURCE,
                    0,
                    R.string.modpack_download_applying_overrides,
                    1,
                    2
                )
                ZipUtils.zipExtract(modpackZipFile, "overrides/", targetPath)
                ProgressLayout.setProgress(ProgressLayout.INSTALL_RESOURCE, 50, R.string.modpack_download_applying_overrides, 2, 2)
                ZipUtils.zipExtract(modpackZipFile, "client-overrides/", targetPath)
                createInfo(modrinthIndex)
            }
            return result
        }

        /**
         * ModrinthIndexからModLoader情報を生成する
         * @param modrinthIndex Modrinthのインデックス情報
         * @return ModLoaderのラッパー情報
         */
/**
 * createInfoする
 */
        private fun createInfo(modrinthIndex: ModrinthIndex?): ModLoaderWrapper? {
            if (modrinthIndex == null) return null
            val dependencies = modrinthIndex.dependencies ?: return null
            val mcVersion = dependencies["minecraft"] ?: return null
            dependencies["forge"]?.let {
                Logging.i("ModLoader", "Forge")
                return ModLoaderWrapper(ModLoader.FORGE, it, mcVersion)
            }
            dependencies["neoforge"]?.let {
                Logging.i("ModLoader", "NeoForge")
                return ModLoaderWrapper(ModLoader.NEOFORGE, it, mcVersion)
            }
            dependencies["fabric-loader"]?.let {
                Logging.i("ModLoader", "Fabric")
                return ModLoaderWrapper(ModLoader.FABRIC, it, mcVersion)
            }
            dependencies["quilt-loader"]?.let {
                Logging.i("ModLoader", "Quilt")
                return ModLoaderWrapper(ModLoader.QUILT, it, mcVersion)
            }
            return null
        }
    }
}
