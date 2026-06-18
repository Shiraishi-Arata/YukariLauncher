package com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad

import com.arata.yukarilauncher.ui.view.GrabListener

/** ゲームパッドのマップデータを提供するインターフェース。 */
interface GamepadDataProvider {
    /** メニュー画面用のゲームパッドマップ。 */
    val menuMap: GamepadMap
    /** ゲーム内用のゲームパッドマップ。 */
    val gameMap: GamepadMap
    /** @return 現在グラブ中かどうか */
    fun isGrabbing(): Boolean
    /** @param grabListener グラブリスナーをアタッチします */
    fun attachGrabListener(grabListener: GrabListener)
}