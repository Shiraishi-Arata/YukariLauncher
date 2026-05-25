package com.arata.yukarilauncher.event.sticky

import com.arata.yukarilauncher.value.JMinecraftVersionList

/**
 * Minecraftバージョンリストが更新されたことを通知するイベント
 * @param list バージョンリスト（nullの場合は空）
 */
data class MinecraftVersionValueEvent(val list: JMinecraftVersionList?)
