package com.arata.yukarilauncher.feature.download.platform.curseforge

import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.download.enums.ModLoader
import com.arata.yukarilauncher.feature.download.install.InstallHelper
import com.arata.yukarilauncher.feature.download.item.ModLoaderWrapper
import com.arata.yukarilauncher.feature.download.item.VersionItem
import com.arata.yukarilauncher.feature.download.platform.curseforge.CurseForgeCommonUtils.Companion.getDownloadSha1
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.mod.modpack.api.ApiHandler
import com.arata.yukarilauncher.feature.mod.modpack.api.ModDownloader
import com.arata.yukarilauncher.feature.mod.modpack.install.ModPackUtils
import com.arata.yukarilauncher.feature.mod.modpack.models.CurseManifest
import com.arata.yukarilauncher.feature.mod.modpack.models.CurseManifest.CurseMinecraft
import com.arata.yukarilauncher.feature.mod.modpack.models.CurseManifest.CurseModLoader
import com.arata.yukarilauncher.task.ProgressKeeper
import com.arata.yukarilauncher.task.SpeedCalculator
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.utils.file.FileUtils
import com.arata.yukarilauncher.utils.file.ZipUtils
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kdt.mcgui.ProgressLayout

import java.io.File
import java.io.IOException
import java.nio.charset.Charset
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
            ZipFile(zipFile, ZipFile.OPEN_READ, Charset.forName("CP437")).use { modpackZipFile ->
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
                curseManifest.image?.let { imagePath ->
                    val entry = modpackZipFile.getEntry(imagePath)
                    if (entry != null) {
                        val iconFile = File(File(targetPath, com.arata.yukarilauncher.InfoDistributor.LAUNCHER_NAME), "VersionIcon.png")
                        iconFile.parentFile?.mkdirs()
                        modpackZipFile.getInputStream(entry).use { input ->
                            iconFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        Logging.i("CurseForgeModPackInstallHelper", "Installed profile image from manifest: $imagePath")
                    }
                }
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
        /** プロジェクトClassId→サブディレクトリ名のマッピング */
        private fun classIdToDir(classId: Int): String = when (classId) {
            12 -> "resourcepacks"
            6552 -> "shaderpacks"
            else -> "mods"
        }

        /** マニフェストに含まれる全プロジェクトのClassIdをバッチ取得し、{projectID → サブディレクトリ} のMapを返す */
        private fun fetchProjectDirs(api: ApiHandler, files: Array<CurseManifest.CurseFile>): Map<Long, String> {
            val projectIds = files.map { it.projectID }.distinct()
            if (projectIds.isEmpty()) return emptyMap()
            val result = mutableMapOf<Long, String>()
            try {
                val body = JsonObject().apply {
                    add("modIds", JsonArray().apply { projectIds.forEach { add(it) } })
                }
                val headers = api.additionalHeaders
                val response = ApiHandler.postRaw(headers, "${api.baseUrl}/mods", body.toString())
                if (response != null) {
                    val json = Tools.GLOBAL_GSON.fromJson(response, JsonObject::class.java)
                    json["data"]?.asJsonArray?.forEach { element ->
                        val obj = element.asJsonObject
                        val id = obj["id"]?.asLong ?: return@forEach
                        val classId = obj["classId"]?.asInt ?: 6
                        result[id] = classIdToDir(classId)
                    }
                }
            } catch (e: Exception) {
                Logging.e("CurseForgeModPackInstallHelper", "fetchProjectDirs failed: ${Tools.printToString(e)}")
            }
            // フェッチできなかったプロジェクトはデフォルトで mods に
            projectIds.forEach { result.putIfAbsent(it, "mods") }
            return result
        }

        private fun getModDownloader(
            api: ApiHandler,
            instanceDestination: File,
            curseManifest: CurseManifest
        ): ModDownloader {
            val files = curseManifest.files ?: return ModDownloader(instanceDestination, true)
            val projectDirs = fetchProjectDirs(api, files)
            val modDownloader = ModDownloader(instanceDestination, true)
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
                        val fileName = FileUtils.getFileName(url) ?: ""
                        val subDir = projectDirs[curseFile.projectID] ?: "mods"
                        return ModDownloader.FileInfo(url, "$subDir/$fileName", getDownloadSha1(api, curseFile.projectID, curseFile.fileID))
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