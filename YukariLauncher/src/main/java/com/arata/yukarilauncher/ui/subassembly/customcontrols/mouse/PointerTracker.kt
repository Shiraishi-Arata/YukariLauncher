package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.view.MotionEvent

/**
 * タッチポインタの追跡クラス。
 * タッチイベントからポインタの移動ベクトルを計算します。
 */
class PointerTracker {
    private var mColdStart = true
    private var mTrackedPointerId = 0
    private var mPointerCount = 0
    private var mLastX = 0f
    private var mLastY = 0f
    val motionVector = FloatArray(2)

    /**
     * ポインタの追跡を開始します。
     * @param motionEvent 開始イベント
     */
    fun startTracking(motionEvent: MotionEvent) {
        mColdStart = false
        mTrackedPointerId = motionEvent.getPointerId(0)
        mPointerCount = motionEvent.pointerCount
        mLastX = motionEvent.x
        mLastY = motionEvent.y
    }

    /** 追跡をキャンセルします。 */
    fun cancelTracking() {
        mColdStart = true
    }

    /**
     * イベントを追跡し、移動ベクトルを更新します。
     * @param motionEvent 追跡するイベント
     * @return 追跡中のポインタインデックス
     */
    fun trackEvent(motionEvent: MotionEvent): Int {
        var trackedPointerIndex = motionEvent.findPointerIndex(mTrackedPointerId)
        val pointerCount = motionEvent.pointerCount
        if (trackedPointerIndex == -1 || mPointerCount != pointerCount || mColdStart) {
            startTracking(motionEvent)
            trackedPointerIndex = 0
        }
        val trackedX = motionEvent.getX(trackedPointerIndex)
        val trackedY = motionEvent.getY(trackedPointerIndex)
        motionVector[0] = trackedX - mLastX
        motionVector[1] = trackedY - mLastY
        mLastX = trackedX
        mLastY = trackedY
        return trackedPointerIndex
    }
}