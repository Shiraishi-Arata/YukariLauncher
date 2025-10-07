package com.arata.yukarilauncher.feature.download.platform.update

import java.io.File

object InstalledModsScanner {
    fun scan(modsDir: File): List<ModMetadataReader.ModInfo> {
        val mods = mutableListOf<ModMetadataReader.ModInfo>()
        if (!modsDir.exists()) return mods

        modsDir.listFiles { f -> f.extension == "jar" }?.forEach { jar ->
            val modInfo = ModMetadataReader.parseMod(jar)
            if (modInfo != null) mods.add(modInfo)
        }
        return mods
    }
}
