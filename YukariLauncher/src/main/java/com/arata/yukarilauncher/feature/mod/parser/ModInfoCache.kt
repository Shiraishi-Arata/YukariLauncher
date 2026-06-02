package com.arata.yukarilauncher.feature.mod.parser

/**
 * Mod情報のデータキャッシュを保持するデータクラス。
 * ファイルハッシュとMod情報をペアで格納する。
 */
data class ModInfoCache(val fileHash: String, val modInfo: ModInfo)