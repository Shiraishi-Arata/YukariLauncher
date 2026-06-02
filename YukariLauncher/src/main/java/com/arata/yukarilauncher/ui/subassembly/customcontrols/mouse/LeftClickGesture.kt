package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.os.Handler
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.MathUtils
import org.lwjgl.glfw.CallbackBridge

/** 左クリックの長押しジェスチャーを検出・処理するクラス。 */
class LeftClickGesture(handler: Handler) : ValidatorGesture(handler) {
    companion object {
        /** 指が静止していると判定する閾値（px）。 */
        val FINGER_STILL_THRESHOLD = Tools.dpToPx(9f).toInt()

        /**
         * 指が静止しているか判定します（現在位置と開始位置を比較）。
         * @param startX 開始X座標
         * @param startY 開始Y座標
         * @param threshold 静止判定の閾値
         * @return 静止している場合はtrue
         */
        fun isFingerStill(startX: Float, startY: Float, threshold: Float): Boolean {
            return MathUtils.dist(
                CallbackBridge.mouseX,
                CallbackBridge.mouseY,
                startX,
                startY
            ) <= threshold
        }

        /**
         * 指が静止しているか判定します（2点間の距離）。
         * @param startX 開始X座標
         * @param startY 開始Y座標
         * @param endX 終了X座標
         * @param endY 終了Y座標
         * @param threshold 静止判定の閾値
         * @return 静止している場合はtrue
         */
        fun isFingerStill(startX: Float, startY: Float, endX: Float, endY: Float, threshold: Float): Boolean {
            return MathUtils.dist(
                endX,
                endY,
                startX,
                startY
            ) <= threshold
        }
    }

    private var mGestureStartX = 0f
    private var mGestureStartY = 0f
    private var mGestureEndX = 0f
    private var mGestureEndY = 0f
    private var mMouseActivated = false

    /** 左クリックの入力イベントを送信します。 */
    fun inputEvent() {
        if (submit()) {
            mGestureStartX = CallbackBridge.mouseX
            mGestureStartY = CallbackBridge.mouseY
            mGestureEndX = CallbackBridge.mouseX
            mGestureEndY = CallbackBridge.mouseY
        }
    }

    override fun getGestureDelay(): Int = AllStaticSettings.timeLongPressTrigger

    override fun checkAndTrigger(): Boolean {
        val fingerStill = isFingerStill(mGestureStartX, mGestureStartY, mGestureEndX, mGestureEndY, FINGER_STILL_THRESHOLD.toFloat())
        if (fingerStill) {
            CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), true)
            mMouseActivated = true
        }
        return true
    }

    override fun onGestureCancelled(isSwitching: Boolean) {
        if (mMouseActivated) {
            CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), false)
            mMouseActivated = false
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
}