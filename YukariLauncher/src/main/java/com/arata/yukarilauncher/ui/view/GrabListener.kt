package com.arata.yukarilauncher.ui.view

/**
 * カーソルグラブ（捕捉）状態の変更を監視するための関数型インターフェース。
 * マウス操作がViewに捕捉されたときや解放されたときに通知を受け取ります。
 */
fun interface GrabListener {
    /**
     * グラブ状態が変更されたときに呼び出されます。
     * @param isGrabbing 現在グラブ中であればtrue
     */
    fun onGrabState(isGrabbing: Boolean)
}
