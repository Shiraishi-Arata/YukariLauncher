package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.view.MotionEvent
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import com.arata.yukarilauncher.Tools
import org.lwjgl.glfw.CallbackBridge

/** メニュー/GUI画面でのタッチイベントを処理するプロセッサー。タップ、スクロール、マウス移動を処理します。 */
class InGUIEventProcessor : TouchEventProcessor {
    companion object {
        /** 指のスクロール判定閾値（px）。 */
        val FINGER_SCROLL_THRESHOLD = Tools.dpToPx(6f)
        /** 指の静止判定閾値（px）。 */
        val FINGER_STILL_THRESHOLD = Tools.dpToPx(5f)
    }

    private val mTracker = PointerTracker()
    private val mSingleTapDetector = TapDetector(1, TapDetector.DETECTION_METHOD_BOTH)
    private var mTouchpad: AbstractTouchpad? = null
    private var mIsMouseDown = false
    private var mStartX = 0f
    private var mStartY = 0f
    private val mScroller = Scroller(FINGER_SCROLL_THRESHOLD)

    override fun processTouchEvent(motionEvent: MotionEvent): Boolean {
        val singleTap = mSingleTapDetector.onTouchEvent(motionEvent)

        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTracker.startTracking(motionEvent)
                if (!touchpadDisplayed()) {
                    sendTouchCoordinates(motionEvent.x, motionEvent.y)
                    if (AllSettings.disableGestures.getValue()) enableMouse()
                    else setGestureStart(motionEvent)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerCount = motionEvent.pointerCount
                val pointerIndex = mTracker.trackEvent(motionEvent)
                if (pointerCount == 1 || AllSettings.disableGestures.getValue()) {
                    if (touchpadDisplayed()) {
                        mTouchpad!!.applyMotionVector(mTracker.motionVector)
                    } else {
                        val mainPointerX = motionEvent.getX(pointerIndex)
                        val mainPointerY = motionEvent.getY(pointerIndex)
                        sendTouchCoordinates(mainPointerX, mainPointerY)
                        if (!mIsMouseDown) {
                            if (!hasGestureStarted()) setGestureStart(motionEvent)
                            if (!LeftClickGesture.isFingerStill(mStartX, mStartY, FINGER_STILL_THRESHOLD))
                                enableMouse()
                        }
                    }
                } else mScroller.performScroll(mTracker.motionVector)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mScroller.resetScrollOvershoot()
                mTracker.cancelTracking()
                if ((!AllSettings.disableGestures.getValue() || touchpadDisplayed()) && !mIsMouseDown && singleTap) {
                    CallbackBridge.putMouseEventWithCoords(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), CallbackBridge.mouseX, CallbackBridge.mouseY)
                }
                if (mIsMouseDown) disableMouse()
                resetGesture()
            }
        }
        return true
    }

    /** @return タッチパッドが表示中かどうか */
    private fun touchpadDisplayed(): Boolean {
        return mTouchpad != null && mTouchpad!!.displayState
    }

    /** @param touchpad 使用するタッチパッドを設定 */
    fun setAbstractTouchpad(touchpad: AbstractTouchpad?) {
        mTouchpad = touchpad
    }

    /**
     * タッチ座標をMinecraftのカーソル位置に変換して送信します。
     * @param x タッチX座標
     * @param y タッチY座標
     */
    private fun sendTouchCoordinates(x: Float, y: Float) {
        CallbackBridge.sendCursorPos(x * AllStaticSettings.scaleFactor, y * AllStaticSettings.scaleFactor)
    }

    /** マウスを有効化（左ボタン押下）します。 */
    private fun enableMouse() {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), true)
        mIsMouseDown = true
    }

    /** マウスを無効化（左ボタン解放）します。 */
    private fun disableMouse() {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), false)
        mIsMouseDown = false
    }

    /** @param event ジェスチャーの開始座標を設定 */
    private fun setGestureStart(event: MotionEvent) {
        mStartX = event.x * AllStaticSettings.scaleFactor
        mStartY = event.y * AllStaticSettings.scaleFactor
    }

    /** ジェスチャー状態をリセットします。 */
    private fun resetGesture() {
        mStartX = -1f
        mStartY = -1f
    }

    /** @return ジェスチャーが開始されているかどうか */
    private fun hasGestureStarted(): Boolean {
        return mStartX != -1f || mStartY != -1f
    }

    override fun cancelPendingActions() {
        mScroller.resetScrollOvershoot()
        disableMouse()
    }
}