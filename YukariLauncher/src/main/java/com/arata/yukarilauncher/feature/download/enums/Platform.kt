package com.arata.yukarilauncher.feature.download.enums

import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper
import com.arata.yukarilauncher.feature.download.platform.curseforge.CurseForgeHelper
import com.arata.yukarilauncher.feature.download.platform.modrinth.ModrinthHelper

/**
 * ダウンロードプラットフォームを定義する列挙型。
 * 各プラットフォームに固有のヘルパーインスタンスを保持する。
 */
enum class Platform(
    val pName: String,
    val helper: AbstractPlatformHelper,
    val iconResId: Int,
    val accentColor: Int
) {
    MODRINTH("Modrinth", ModrinthHelper(), R.drawable.ic_modrinth, 0xFF30D27B.toInt()),
    CURSEFORGE("CurseForge", CurseForgeHelper(), R.drawable.ic_curseforge, 0xFFF16436.toInt())
}