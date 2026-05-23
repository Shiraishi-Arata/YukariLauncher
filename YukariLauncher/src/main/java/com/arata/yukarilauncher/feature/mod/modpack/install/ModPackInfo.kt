package com.arata.yukarilauncher.feature.mod.modpack.install

/**
 * 整合包の基本情報を保持するデータクラス。
 * @param name 整合包の名称。新しいバージョン作成時に使用される。
 * @param type 整合包の種類（CurseForge、Modrinth、MCBBS）
 */
data class ModPackInfo(val name: String?, val type: ModPackUtils.ModPackEnum)
