package com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad

import android.view.InputDevice.KEYBOARD_TYPE_ALPHABETIC
import android.view.InputDevice.SOURCE_GAMEPAD
import android.view.KeyEvent

/** ゲームパッドのDパッド（方向パッド）イベントを判別するユーティリティオブジェクト。 */
object GamepadDpad {
    /**
     * KeyEventがDパッドイベントかどうかを判定します。
     * @param event 判定するKeyEvent
     * @return Dパッドイベントの場合はtrue
     */
    fun isDpadEvent(event: KeyEvent): Boolean {
        return event.isFromSource(SOURCE_GAMEPAD) && (event.device == null || event.device!!.keyboardType != KEYBOARD_TYPE_ALPHABETIC)
    }
}
