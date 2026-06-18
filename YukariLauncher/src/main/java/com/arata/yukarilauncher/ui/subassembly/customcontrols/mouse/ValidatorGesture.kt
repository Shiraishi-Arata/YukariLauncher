package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.os.Handler

/**
 * ジェスチャーを検証する抽象クラス。
 * ジェスチャーの送信、遅延実行、キャンセル、トリガー判定の共通処理を定義します。
 */
abstract class ValidatorGesture(private val mHandler: Handler) : Runnable {
    private var mGestureActive = false

    /**
     * ジェスチャーを送信します。
     * @return 送信が受け付けられた場合はtrue（既にアクティブな場合はfalse）
     */
    fun submit(): Boolean {
        if (mGestureActive) return false
        mHandler.postDelayed(this, getGestureDelay().toLong())
        mGestureActive = true
        return true
    }

    /**
     * ジェスチャーをキャンセルします。
     * @param isSwitching 切り替えによるキャンセルかどうか
     */
    fun cancel(isSwitching: Boolean) {
        if (!mGestureActive) return
        mHandler.removeCallbacks(this)
        onGestureCancelled(isSwitching)
        mGestureActive = false
    }

    override fun run() {
        if (checkAndTrigger()) return
        mGestureActive = false
        onGestureCancelled(false)
    }

    /** @return ジェスチャーの遅延時間（ミリ秒） */
    protected abstract fun getGestureDelay(): Int
    /** @return ジェスチャーがトリガーされた場合はtrue */
    abstract fun checkAndTrigger(): Boolean
    /**
     * ジェスチャーがキャンセルされたときの処理。
     * @param isSwitching 切り替えによるキャンセルかどうか
     */
    abstract fun onGestureCancelled(isSwitching: Boolean)
}