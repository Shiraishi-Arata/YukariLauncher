package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent
import com.arata.yukarilauncher.Tools

/** タップ操作を検出するクラス。シングルタップ、ダブルタップを時間・距離で判定します。 */
class TapDetector(tapNumberToDetect: Int, private val mDetectionMethod: Int) {
    companion object {
        /** ダウンイベントで検出。 */
        const val DETECTION_METHOD_DOWN = 0x1
        /** アップイベントで検出。 */
        const val DETECTION_METHOD_UP = 0x2
        /** ダウンとアップの両方で検出。 */
        const val DETECTION_METHOD_BOTH = 0x3
        private const val TAP_MIN_DELTA_MS = -1
        private const val TAP_MAX_DELTA_MS = 300
        private val TAP_SLOP_SQUARE_PX = Tools.dpToPx(2500f).toInt()
    }

    private val mTapNumberToDetect: Int
    private var mCurrentTapNumber = 0
    private var mLastEventTime: Long = 0
    private var mLastX = 9999f
    private var mLastY = 9999f

    init {
        mTapNumberToDetect = if (detectBothTouch()) 2 * tapNumberToDetect else tapNumberToDetect
    }

    /**
     * タッチイベントを処理してタップを検出します。
     * @param e 処理するタッチイベント
     * @return タップが検出された場合はtrue
     */
    fun onTouchEvent(e: MotionEvent): Boolean {
        val eventAction = e.actionMasked
        var pointerIndex = -1

        if (detectDownTouch()) {
            if (eventAction == ACTION_DOWN) pointerIndex = 0
            else if (eventAction == ACTION_POINTER_DOWN) pointerIndex = e.actionIndex
        }
        if (detectUpTouch()) {
            if (eventAction == ACTION_UP) pointerIndex = 0
            else if (eventAction == ACTION_POINTER_UP) pointerIndex = e.actionIndex
        }

        if (pointerIndex == -1) return false

        val eventX = e.getX(pointerIndex)
        val eventY = e.getY(pointerIndex)
        val eventTime = e.eventTime

        val deltaTime = eventTime - mLastEventTime
        val deltaX = (mLastX - eventX).toInt()
        val deltaY = (mLastY - eventY).toInt()

        mLastEventTime = eventTime
        mLastX = eventX
        mLastY = eventY

        if (mCurrentTapNumber > 0) {
            if ((deltaTime < TAP_MIN_DELTA_MS || deltaTime > TAP_MAX_DELTA_MS) ||
                (deltaX * deltaX + deltaY * deltaY > TAP_SLOP_SQUARE_PX)) {
                if (mDetectionMethod == DETECTION_METHOD_BOTH && (eventAction == ACTION_UP || eventAction == ACTION_POINTER_UP)) {
                    resetTapDetectionState()
                    return false
                } else {
                    mCurrentTapNumber = 0
                }
            }
        }

        mCurrentTapNumber += 1
        if (mCurrentTapNumber >= mTapNumberToDetect) {
            resetTapDetectionState()
            return true
        }

        return false
    }

    /** タップ検出状態をリセットします。 */
    private fun resetTapDetectionState() {
        mCurrentTapNumber = 0
        mLastEventTime = 0
        mLastX = 9999f
        mLastY = 9999f
    }

    /** @return ダウンイベントを検出するかどうか */
    private fun detectDownTouch(): Boolean {
        return (mDetectionMethod and DETECTION_METHOD_DOWN) == DETECTION_METHOD_DOWN
    }

    /** @return アップイベントを検出するかどうか */
    private fun detectUpTouch(): Boolean {
        return (mDetectionMethod and DETECTION_METHOD_UP) == DETECTION_METHOD_UP
    }

    /** @return 両方のイベントを検出するかどうか */
    private fun detectBothTouch(): Boolean {
        return mDetectionMethod == DETECTION_METHOD_BOTH
    }
}