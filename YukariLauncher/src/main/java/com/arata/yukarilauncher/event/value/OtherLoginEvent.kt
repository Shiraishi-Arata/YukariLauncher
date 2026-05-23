package com.arata.yukarilauncher.event.value

import net.kdt.pojavlaunch.value.MinecraftAccount

/**
 * 外部アカウントでのログインが行われたことを通知するイベント
 * @param account ログインしたMinecraftアカウント
 */
data class OtherLoginEvent(val account: MinecraftAccount)
