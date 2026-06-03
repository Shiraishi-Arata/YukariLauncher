package com.arata.yukarilauncher.feature.download.enums

import com.arata.yukarilauncher.R

/**
 * Modローダーの種類を定義する列挙型。
 * 各プラットフォーム（CurseForge、Modrinth）における識別子と名前を保持する。
 */
enum class ModLoader(val type: Int, val loaderName: String, val curseforgeId: String, val modrinthName: String, val tagColor: Int) {
    ALL(-1, "", "", "", R.color.mod_category_fill),
    FORGE(0, "Forge", "1", "forge", R.color.modloader_forge),
    NEOFORGE(1, "NeoForge", "6", "neoforge", R.color.modloader_neoforge),
    FABRIC(2, "Fabric", "4", "fabric", R.color.modloader_fabric),
    QUILT(3, "Quilt", "5", "quilt", R.color.modloader_quilt)
}