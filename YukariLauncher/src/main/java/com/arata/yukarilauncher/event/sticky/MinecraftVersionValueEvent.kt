package com.arata.yukarilauncher.event.sticky

import net.kdt.pojavlaunch.JMinecraftVersionList

/**
 * Minecraftバージョンリストが更新されたことを通知するイベント
 * @param list バージョンリスト（nullの場合は空）
 */
data class MinecraftVersionValueEvent(val list: JMinecraftVersionList?)
