package com.arata.yukarilauncher.feature.download.enums

import com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper
import com.arata.yukarilauncher.feature.download.platform.curseforge.CurseForgeHelper
import com.arata.yukarilauncher.feature.download.platform.modrinth.ModrinthHelper

/**
 * ダウンロードプラットフォームを定義する列挙型。
 * 各プラットフォームに固有のヘルパーインスタンスを保持する。
 */
enum class Platform(val pName: String, val helper: AbstractPlatformHelper) {
    MODRINTH("Modrinth", ModrinthHelper()),
    CURSEFORGE("CurseForge", CurseForgeHelper())
}
