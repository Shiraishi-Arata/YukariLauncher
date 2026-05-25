package com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad

/**
 * ゲームパッドのトグル可能なボタンを表すクラス。
 * GamepadEmulatedButtonを継承し、トグル動作（押すごとにON/OFF切り替え）をサポートします。
 */
open class GamepadButton : GamepadEmulatedButton() {
    /** トグル動作が有効かどうか。 */
    var isToggleable = false
    private var mIsToggled = false

    override fun onDownStateChanged(isDown: Boolean) {
        if (isToggleable) {
            if (!isDown) return
            mIsToggled = !mIsToggled
            Gamepad.sendInput(keycodes, mIsToggled)
            return
        }
        super.onDownStateChanged(isDown)
    }

    override fun resetButtonState() {
        if (!mIsDown && mIsToggled) {
            Gamepad.sendInput(keycodes, false)
            mIsToggled = false
        } else {
            super.resetButtonState()
        }
    }
}
