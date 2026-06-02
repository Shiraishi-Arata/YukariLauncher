package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import org.lwjgl.glfw.CallbackBridge

/** スクロール入力を処理するクラス。閾値を超えた移動をスクロールイベントに変換します。 */
class Scroller(private val mScrollThreshold: Float) {
    private var mScrollOvershootH = 0f
    private var mScrollOvershootV = 0f

    /**
     * スクロールを実行します。
     * @param dx X方向の移動量
     * @param dy Y方向の移動量
     */
    fun performScroll(dx: Float, dy: Float) {
        val hScroll = dx / mScrollThreshold + mScrollOvershootH
        val vScroll = dy / mScrollThreshold + mScrollOvershootV
        val hScrollRound = hScroll.toInt()
        val vScrollRound = vScroll.toInt()
        if (hScrollRound != 0 || vScrollRound != 0) CallbackBridge.sendScroll(hScroll.toDouble(), vScroll.toDouble())
        mScrollOvershootH = hScroll - hScrollRound
        mScrollOvershootV = vScroll - vScrollRound
    }

    /**
     * ベクトル配列からスクロールを実行します。
     * @param vector [x, y]の移動ベクトル
     */
    fun performScroll(vector: FloatArray) {
        performScroll(vector[0], vector[1])
    }

    /** スクロールのオーバーシュート値をリセットします。 */
    fun resetScrollOvershoot() {
        mScrollOvershootH = 0f
        mScrollOvershootV = 0f
    }
}