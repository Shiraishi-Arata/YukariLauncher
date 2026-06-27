package com.arata.yukarilauncher.feature.mod.modpack.install

import android.app.Activity
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.download.item.ModLoaderWrapper
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.mod.models.MCBBSPackMeta
import com.arata.yukarilauncher.feature.version.install.HeadlessInstaller
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.mod.modpack.models.CurseManifest
import com.arata.yukarilauncher.feature.mod.modpack.models.ModrinthIndex
import java.io.File
import java.util.zip.ZipFile

class ModPackUtils {
    companion object {
        @JvmStatic
/**
 * determineModpackする
 */
        fun determineModpack(modpack: File): ModPackInfo {
            val zipName = modpack.name
            val suffix = zipName.substring(zipName.lastIndexOf('.'))
            runCatching {
                ZipFile(modpack).use { modpackZipFile ->
                    if (suffix == ".zip") {
                        val mcbbsEntry = modpackZipFile.getEntry("mcbbs.packmeta")
                        val curseforgeEntry = modpackZipFile.getEntry("manifest.json")
                        if (mcbbsEntry == null && curseforgeEntry != null) {
                            val curseManifest = Tools.GLOBAL_GSON.fromJson(
                                Tools.read(modpackZipFile.getInputStream(curseforgeEntry)),
                                CurseManifest::class.java
                            )
                            if (verifyManifest(curseManifest)) return ModPackInfo(curseManifest.name, ModPackEnum.CURSEFORGE)
                        } else if (mcbbsEntry != null) {
                            val mcbbsPackMeta = Tools.GLOBAL_GSON.fromJson(
                                Tools.read(modpackZipFile.getInputStream(mcbbsEntry)),
                                MCBBSPackMeta::class.java
                            )
                            if (verifyMCBBSPackMeta(mcbbsPackMeta)) return ModPackInfo(mcbbsPackMeta.name, ModPackEnum.MCBBS)
                        }
                    } else if (suffix == ".mrpack") {
                        val entry = modpackZipFile.getEntry("modrinth.index.json")
                        if (entry != null) {
                            val modrinthIndex = Tools.GLOBAL_GSON.fromJson(
                                Tools.read(modpackZipFile.getInputStream(entry)),
                                ModrinthIndex::class.java
                            )
                            if (verifyModrinthIndex(modrinthIndex)) return ModPackInfo(modrinthIndex.name, ModPackEnum.MODRINTH)
                        }
                    }
                }
            }.onFailure { e ->
                Logging.e("determineModpack", "There was a problem checking the ModPack", e)
            }

            return ModPackInfo(null, ModPackEnum.UNKNOWN)
        }

        @JvmStatic
/**
 * verifyManifestする
 */
        fun verifyManifest(manifest: CurseManifest): Boolean { //检测是否为curseforge整合包(通过manifest.json内的数据进行判断)
            if ("minecraftModpack" != manifest.manifestType) return false
            if (manifest.manifestVersion != 1) return false
            val mc = manifest.minecraft ?: return false
            if (mc.version == null) return false
            val modLoaders = mc.modLoaders ?: return false
            return modLoaders.isNotEmpty()
        }

        @JvmStatic
/**
 * verifyModrinthIndexする
 */
        fun verifyModrinthIndex(modrinthIndex: ModrinthIndex): Boolean { //检测是否为modrinth整合包(通过modrinth.index.json内的数据进行判断)
            if ("minecraft" != modrinthIndex.game) return false
            if (modrinthIndex.formatVersion != 1) return false
            return modrinthIndex.dependencies != null
        }

/**
 * verifyMCBBSPackMetaする
 */
        fun verifyMCBBSPackMeta(mcbbsPackMeta: MCBBSPackMeta): Boolean { //检测是否为MCBBS整合包(通过mcbbs.packmeta内的数据进行判断)
            if ("minecraftModpack" != mcbbsPackMeta.manifestType) return false
            if (mcbbsPackMeta.manifestVersion != 2) return false
            if (mcbbsPackMeta.addons == null) return false
            if (mcbbsPackMeta.addons[0].id == null) return false
            return (mcbbsPackMeta.addons[0].version != null)
        }

        @JvmStatic
        @Throws(Throwable::class)
/**
 * startModLoaderInstallする
 */
        fun startModLoaderInstall(modLoader: ModLoaderWrapper, activity: Activity, modInstallFile: File, customName: String) {
            modLoader.getInstallationIntent(activity, modInstallFile, customName)?.let { installIntent ->
                val javaArgs = installIntent.getStringExtra("javaArgs") ?: return
                HeadlessInstaller.install(activity, javaArgs, null, modLoader.modLoader.loaderName)
            }
        }
    }

    enum class ModPackEnum {
        UNKNOWN, CURSEFORGE, MCBBS, MODRINTH
    }
}