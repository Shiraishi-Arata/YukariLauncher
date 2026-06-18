package com.arata.yukarilauncher.event.value

/**
 * ホットバーの判定枠が変更されたときに通知されるイベント
 * @param width 変更後の幅
 * @param height 変更後の高さ
 */
class HotbarChangeEvent(val width: Int, val height: Int)