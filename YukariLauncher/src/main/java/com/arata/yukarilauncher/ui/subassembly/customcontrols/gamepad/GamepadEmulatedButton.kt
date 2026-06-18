package com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad

import android.view.KeyEvent

/**
 * ゲームパッドのエミュレートされたボタンを表すクラス。
 * 物理ボタン入力をMinecraftのキーコードに変換して送信します。
 */
open class GamepadEmulatedButton {
    /** このボタンに割り当てられたキーコード配列。 */
    var keycodes: ShortArray = shortArrayOf()
    protected var mIsDown = false

    /**
     * KeyEventからボタン状態を更新します。
     * @param event 入力イベント
     */
    fun update(event: KeyEvent) {
        val isKeyDown = event.action == KeyEvent.ACTION_DOWN
        update(isKeyDown)
    }

    /**
     * 押下状態を更新します。
     * @param isKeyDown 押下されているかどうか
     */
    fun update(isKeyDown: Boolean) {
        if (isKeyDown != mIsDown) {
            mIsDown = isKeyDown
            onDownStateChanged(mIsDown)
        }
    }

    /** ボタン状態をリセットします。 */
    open fun resetButtonState() {
        if (mIsDown) Gamepad.sendInput(keycodes, false)
        mIsDown = false
    }

    /**
     * 押下状態が変更されたときに呼ばれます。
     * @param isDown 現在の押下状態
     */
    protected open fun onDownStateChanged(isDown: Boolean) {
        Gamepad.sendInput(keycodes, mIsDown)
    }
}