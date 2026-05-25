package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.os.Handler
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import org.lwjgl.glfw.CallbackBridge

/** アイテムドロップの長押しジェスチャーを検出・処理するクラス。一定時間長押しでQキーを繰り返し送信します。 */
class DropGesture(private val mHandler: Handler) : Runnable {
    private var mActive = false

    /** ドロップジェスチャーを開始します。 */
    fun submit() {
        if (!mActive) {
            mActive = true
            mHandler.postDelayed(this, AllStaticSettings.timeLongPressTrigger.toLong())
        }
    }

    /** ドロップジェスチャーをキャンセルします。 */
    fun cancel() {
        mActive = false
        mHandler.removeCallbacks(this)
    }

    override fun run() {
        if (!mActive) return
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_Q.toInt())
        mHandler.postDelayed(this, 250)
    }
}
