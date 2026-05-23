package com.arata.yukarilauncher.feature.download.enums

/**
 * Modローダーの種類を定義する列挙型。
 * 各プラットフォーム（CurseForge、Modrinth）における識別子と名前を保持する。
 */
enum class ModLoader(val type: Int, val loaderName: String, val curseforgeId: String, val modrinthName: String) {
    ALL(-1, "", "", ""),
    FORGE(0, "Forge", "1", "forge"),
    NEOFORGE(1, "NeoForge", "6", "neoforge"),
    FABRIC(2, "Fabric", "4", "fabric"),
    QUILT(3, "Quilt", "5", "quilt")
}
