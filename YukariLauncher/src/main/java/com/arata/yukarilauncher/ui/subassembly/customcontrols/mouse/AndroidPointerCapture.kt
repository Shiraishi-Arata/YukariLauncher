package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.MinecraftGLSurface
import com.arata.yukarilauncher.Tools
import org.lwjgl.glfw.CallbackBridge

/**
 * Androidのポインタキャプチャ（マウスキャプチャ）機能を管理するクラス。
 * マウス/トラックボールの入力をタッチパッド操作やMinecraftのカメラ移動に変換します。
 */
class AndroidPointerCapture : ViewTreeObserver.OnWindowFocusChangeListener, View.OnCapturedPointerListener {
    companion object {
        private const val TOUCHPAD_SCROLL_THRESHOLD = 1f
    }

    private val mTouchpad: AbstractTouchpad
    private val mHostView: View
    private val mMousePrescale = Tools.dpToPx(1f)
    private val mPointerTracker = PointerTracker()
    private val mScroller = Scroller(TOUCHPAD_SCROLL_THRESHOLD)
    private val mVector = mPointerTracker.motionVector
    private var mInputDeviceIdentifier = 0
    private var mDeviceSupportsRelativeAxis = false

    /**
     * @param touchpad 関連付けるタッチパッド
     * @param hostView ホストとなるビュー
     */
    constructor(touchpad: AbstractTouchpad, hostView: View) {
        this.mTouchpad = touchpad
        this.mHostView = hostView
        hostView.setOnCapturedPointerListener(this)
        hostView.viewTreeObserver.addOnWindowFocusChangeListener(this)
    }

    /** 必要に応じてタッチパッドを有効にします。 */
    private fun enableTouchpadIfNecessary() {
        if (!mTouchpad.displayState) mTouchpad.enable(true)
    }

    /** 自動キャプチャを処理します。 */
    fun handleAutomaticCapture() {
        if (!mHostView.hasWindowFocus()) {
            mHostView.requestFocus()
        } else {
            mHostView.requestPointerCapture()
        }
    }

    override fun onCapturedPointer(view: View, event: MotionEvent): Boolean {
        checkSameDevice(event.device)

        if ((event.source and InputDevice.SOURCE_CLASS_TRACKBALL) != 0) {
            if (mDeviceSupportsRelativeAxis) {
                mVector[0] = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
                mVector[1] = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
            } else {
                mVector[0] = event.x
                mVector[1] = event.y
            }
        } else {
            mPointerTracker.trackEvent(event)
        }

        if (!CallbackBridge.isGrabbing()) {
            enableTouchpadIfNecessary()
            mVector[0] *= mMousePrescale
            mVector[1] *= mMousePrescale
            if (event.pointerCount < 2) {
                mTouchpad.applyMotionVector(mVector)
                mScroller.resetScrollOvershoot()
            } else {
                mScroller.performScroll(mVector)
            }
        } else {
            CallbackBridge.mouseX += (mVector[0] * AllStaticSettings.scaleFactor)
            CallbackBridge.mouseY += (mVector[1] * AllStaticSettings.scaleFactor)
            CallbackBridge.sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
        }

        return when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> true
            MotionEvent.ACTION_BUTTON_PRESS -> MinecraftGLSurface.sendMouseButtonUnconverted(event.actionButton, true)
            MotionEvent.ACTION_BUTTON_RELEASE -> MinecraftGLSurface.sendMouseButtonUnconverted(event.actionButton, false)
            MotionEvent.ACTION_SCROLL -> {
                CallbackBridge.sendScroll(
                    event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(),
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble()
                )
                true
            }
            MotionEvent.ACTION_UP -> {
                mPointerTracker.cancelTracking()
                true
            }
            else -> false
        }
    }

    /**
     * 同じ入力デバイスかチェックし、デバイス固有のプロパティを再初期化します。
     * @param inputDevice チェックする入力デバイス
     */
    private fun checkSameDevice(inputDevice: InputDevice?) {
        if (inputDevice == null) return
        val newIdentifier = inputDevice.id
        if (mInputDeviceIdentifier != newIdentifier) {
            reinitializeDeviceSpecificProperties(inputDevice)
            mInputDeviceIdentifier = newIdentifier
        }
    }

    /**
     * デバイス固有のプロパティを再初期化します。
     * @param inputDevice 入力デバイス
     */
    private fun reinitializeDeviceSpecificProperties(inputDevice: InputDevice) {
        mPointerTracker.cancelTracking()
        val relativeXSupported = inputDevice.getMotionRange(MotionEvent.AXIS_RELATIVE_X) != null
        val relativeYSupported = inputDevice.getMotionRange(MotionEvent.AXIS_RELATIVE_Y) != null
        mDeviceSupportsRelativeAxis = relativeXSupported && relativeYSupported
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) mHostView.requestPointerCapture()
    }

    /** リスナーをデタッチします。 */
    fun detach() {
        mHostView.setOnCapturedPointerListener(null)
        mHostView.viewTreeObserver.removeOnWindowFocusChangeListener(this)
    }
}
