package com.arata.yukarilauncher.feature.download.enums

import com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper
import com.arata.yukarilauncher.feature.download.platform.curseforge.CurseForgeHelper
import com.arata.yukarilauncher.feature.download.platform.modrinth.ModrinthHelper

enum class Platform(val pName: String, val helper: AbstractPlatformHelper) {
    MODRINTH("Modrinth", ModrinthHelper()),
    CURSEFORGE("CurseForge", CurseForgeHelper())
}