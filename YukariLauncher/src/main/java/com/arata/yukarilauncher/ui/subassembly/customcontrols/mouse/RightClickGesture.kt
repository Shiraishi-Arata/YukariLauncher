package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.os.Handler
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import org.lwjgl.glfw.CallbackBridge

/** 右クリックの長押しジェスチャーを検出・処理するクラス。 */
class RightClickGesture(mHandler: Handler) : ValidatorGesture(mHandler) {
    private var mGestureEnabled = true
    private var mGestureValid = true
    private var mGestureStartX = 0f
    private var mGestureStartY = 0f
    private var mGestureEndX = 0f
    private var mGestureEndY = 0f

    /** 右クリックの入力イベントを送信します。 */
    fun inputEvent() {
        if (!mGestureEnabled) return
        if (submit()) {
            mGestureStartX = CallbackBridge.mouseX
            mGestureStartY = CallbackBridge.mouseY
            mGestureEndX = CallbackBridge.mouseX
            mGestureEndY = CallbackBridge.mouseY
            mGestureEnabled = false
            mGestureValid = true
        }
    }

    /**
     * マウスの移動量を設定します。
     * @param deltaX X方向の移動量
     * @param deltaY Y方向の移動量
     */
    fun setMotion(deltaX: Float, deltaY: Float) {
        mGestureEndX += deltaX
        mGestureEndY += deltaY
    }

    override fun getGestureDelay(): Int = 150

    override fun checkAndTrigger(): Boolean {
        mGestureValid = false
        return true
    }

    override fun onGestureCancelled(isSwitching: Boolean) {
        mGestureEnabled = true
        if (!mGestureValid || isSwitching) return
        val fingerStill = LeftClickGesture.isFingerStill(mGestureStartX, mGestureStartY, mGestureEndX, mGestureEndY, LeftClickGesture.FINGER_STILL_THRESHOLD.toFloat())
        if (!fingerStill) return
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), true)
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), false)
    }
}
