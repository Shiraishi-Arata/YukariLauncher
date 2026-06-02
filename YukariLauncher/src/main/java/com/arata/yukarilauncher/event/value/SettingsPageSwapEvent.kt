package com.arata.yukarilauncher.event.value

/**
 * 設定ページの切り替え時に、Fragmentにアニメーション再生を通知するイベント
 * @param index Fragmentのカテゴリインデックス
 */
class SettingsPageSwapEvent(val index: Int)