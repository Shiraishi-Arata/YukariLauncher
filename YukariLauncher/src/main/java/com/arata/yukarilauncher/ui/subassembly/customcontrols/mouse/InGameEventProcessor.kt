package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.support.touch_controller.ContactHandler
import org.lwjgl.glfw.CallbackBridge

/** ゲーム内でのタッチイベントを処理するプロセッサー。マウス移動、クリックジェスチャーを処理します。 */
class InGameEventProcessor(private val mSensitivity: Double) : TouchEventProcessor {
    private val mGestureHandler = Handler(Looper.getMainLooper())
    private var mEventTransitioned = true
    private val mTracker = PointerTracker()
    private val mLeftClickGesture = LeftClickGesture(mGestureHandler)
    private val mRightClickGesture = RightClickGesture(mGestureHandler)

    override fun processTouchEvent(motionEvent: MotionEvent): Boolean {
        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTracker.startTracking(motionEvent)
                if (!AllSettings.disableGestures.getValue()) {
                    mEventTransitioned = false
                    checkGestures()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                mTracker.trackEvent(motionEvent)
                val motionVector = mTracker.motionVector
                val deltaX = (motionVector[0] * mSensitivity).toFloat()
                val deltaY = (motionVector[1] * mSensitivity).toFloat()
                mLeftClickGesture.setMotion(deltaX, deltaY)
                mRightClickGesture.setMotion(deltaX, deltaY)
                CallbackBridge.mouseX += deltaX
                CallbackBridge.mouseY += deltaY
                CallbackBridge.sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
                if (!AllSettings.disableGestures.getValue()) {
                    checkGestures()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mTracker.cancelTracking()
                cancelGestures(false)
            }
        }
        return true
    }

    override fun cancelPendingActions() {
        cancelGestures(true)
    }

    override fun dispatchTouchEvent(event: MotionEvent, view: View) {
        if (AllStaticSettings.useControllerProxy) {
            ContactHandler.progressEvent(event, view)
        }
    }

    /** ジェスチャーのチェックを行います。 */
    private fun checkGestures() {
        mLeftClickGesture.inputEvent()
        if (!mEventTransitioned) mRightClickGesture.inputEvent()
    }

    /**
     * ジェスチャーをキャンセルします。
     * @param isSwitching 切り替えによるキャンセルかどうか
     */
    private fun cancelGestures(isSwitching: Boolean) {
        mEventTransitioned = true
        mLeftClickGesture.cancel(isSwitching)
        mRightClickGesture.cancel(isSwitching)
    }
}
