package com.arata.yukarilauncher.feature.download.utils

import com.arata.yukarilauncher.feature.download.enums.ModLoader

class ModLoaderUtils {
    companion object {
/**
 * checkForModLoaderする
 */
        private fun checkForModLoader(func: (modloader: ModLoader) -> Boolean): ModLoader? {
            ModLoader.entries.forEach { modLoader ->
                if (func.invoke(modLoader)) return modLoader
            }
            return null
        }

/**
 * getModLoaderByModrinthする
 */
        fun getModLoaderByModrinth(modLoaderName: String): ModLoader? {
            return checkForModLoader { modLoader -> modLoader.modrinthName == modLoaderName }
        }

/**
 * getModLoaderByCurseForgeする
 */
        fun getModLoaderByCurseForge(modLoaderId: String): ModLoader? {
            return checkForModLoader { modloader -> modloader.curseforgeId == modLoaderId }
        }

/**
 * getModLoaderする
 */
        fun getModLoader(name: String): ModLoader? {
            return if (name.equals("fabric", true)) ModLoader.FABRIC
            else if (name.equals("forge", true)) ModLoader.FORGE
            else if (name.equals("quilt", true)) ModLoader.QUILT
            else if (name.equals("neoforge", true)) ModLoader.NEOFORGE
            else null
        }

/**
 * addModLoaderToListする
 */
        fun addModLoaderToList(list: MutableCollection<ModLoader>, name: String) {
            getModLoader(name)?.let { list.add(it) }
        }
    }
}