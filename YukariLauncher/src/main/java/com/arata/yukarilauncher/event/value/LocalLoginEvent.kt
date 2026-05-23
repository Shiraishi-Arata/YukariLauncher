package com.arata.yukarilauncher.event.value

/**
 * ローカルログインが行われたことを通知するイベント
 * @param userName ログインしたユーザー名
 */
data class LocalLoginEvent(val userName: String)
