package com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons

import com.arata.yukarilauncher.utils.LwjglGlfwKeycode.GLFW_KEY_UNKNOWN
import org.lwjgl.glfw.CallbackBridge.sendKeyPress
import org.lwjgl.glfw.CallbackBridge.sendMouseButton
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import com.arata.yukarilauncher.ui.activity.MainActivity
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlLayout
import com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview.EditControlPopup
import org.lwjgl.glfw.CallbackBridge

/**
 * カスタムコントロールのボタンビュークラス。
 * TextViewをベースに、Minecraftへのキー入力送信、トグル動作、リピート機能を提供します。
 */
@SuppressLint("ViewConstructor", "AppCompatCustomView")
open class ControlButton : TextView, ControlInterface {
    private val mRectPaint = Paint()
    lateinit var mProperties: ControlData
    protected val mControlLayout: ControlLayout
    private var mComputedRadius = 0f
    protected var mIsToggled = false
    protected var mIsPointerOutOfBounds = false
    private val mRepeatHandler = Handler(Looper.getMainLooper())
    private val mRepeatRunnable = object : Runnable {
        override fun run() {
            if (!mProperties.repeatedlyEnabled) return
            sendKeyPressesWithoutActivation(true)
            sendKeyPressesWithoutActivation(false)
            mRepeatHandler.postDelayed(this, getRepeatIntervalMs().toLong())
        }
    }

    constructor(layout: ControlLayout, properties: ControlData) : super(layout.context) {
        mControlLayout = layout
        gravity = Gravity.CENTER
        setAllCaps(AllSettings.buttonAllCaps.getValue())
        setTextColor(Color.WHITE)
        setPadding(4, 4, 4, 4)
        setTextSize(14f)
        outlineProvider = null
        setProperties(preProcessProperties(properties, layout))
        injectBehaviors()
    }

    override val controlView: View
        get() = this

    override val properties: ControlData
        get() = mProperties

    override fun setProperties(properties: ControlData, changePos: Boolean) {
        mProperties = properties
        super<ControlInterface>.setProperties(properties, changePos)
        mComputedRadius = super<ControlInterface>.computeCornerRadius(mProperties.cornerRadius)

        if (mProperties.isToggle) {
            val value = TypedValue()
            context.theme.resolveAttribute(R.attr.colorAccent, value, true)
            mRectPaint.color = value.data
            mRectPaint.alpha = 128
        } else {
            mRectPaint.color = Color.WHITE
            mRectPaint.alpha = 60
        }
        text = properties.name
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mIsToggled || (!mProperties.isToggle && isActivated)) {
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), mComputedRadius, mComputedRadius, mRectPaint)
        }
    }

    override fun onDetachedFromWindow() {
        stopButtonRepeat()
        super.onDetachedFromWindow()
    }

    override fun loadEditValues(editControlPopup: EditControlPopup) {
        editControlPopup.loadValues(properties)
    }

    override fun cloneButton() {
        val cloneData = ControlData(properties)
        cloneData.dynamicX = "0.5 * \${screen_width}"
        cloneData.dynamicY = "0.5 * \${screen_height}"
        (parent as ControlLayout).addControlButton(cloneData)
    }

    override fun removeButton() {
        mControlLayout.layout!!.mControlDataList!!.remove(properties)
        mControlLayout.removeView(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (properties.passThruEnabled && CallbackBridge.isGrabbing()) {
                    val gameSurface = mControlLayout.getGameSurface()
                    gameSurface?.dispatchTouchEvent(event)
                }

                if (event.x < controlView.left.toFloat() || event.x > controlView.right.toFloat() ||
                    event.y < controlView.top.toFloat() || event.y > controlView.bottom.toFloat()) {
                    if (properties.isSwipeable && !mIsPointerOutOfBounds) {
                        if (!triggerToggle()) {
                            sendKeyPresses(false)
                        }
                    }
                    mIsPointerOutOfBounds = true
                    mControlLayout.onTouch(this, event)
                } else {
                    if (mIsPointerOutOfBounds) {
                        mControlLayout.onTouch(this, event)
                        if (properties.isSwipeable && !properties.isToggle) {
                            sendKeyPresses(true)
                        }
                    }
                    mIsPointerOutOfBounds = false
                }
            }

            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (!properties.isToggle) {
                    if (properties.repeatedlyEnabled) {
                        isActivated = true
                        sendKeyPressesWithoutActivation(true)
                        sendKeyPressesWithoutActivation(false)
                        startButtonRepeat()
                    } else {
                        sendKeyPresses(true)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                if (properties.passThruEnabled) {
                    val gameSurface = mControlLayout.getGameSurface()
                    gameSurface?.dispatchTouchEvent(event)
                }
                if (mIsPointerOutOfBounds) mControlLayout.onTouch(this, event)
                mIsPointerOutOfBounds = false
                if (!triggerToggle()) {
                    if (properties.repeatedlyEnabled) {
                        stopButtonRepeat()
                        isActivated = false
                    } else {
                        sendKeyPresses(false)
                    }
                }
            }

            else -> return false
        }
        return super.onTouchEvent(event)
    }

    /**
     * トグル状態を切り替えます。
     * @return トグル動作を行った場合はtrue
     */
    fun triggerToggle(): Boolean {
        if (mProperties.isToggle) {
            mIsToggled = !mIsToggled
            invalidate()
            sendKeyPresses(mIsToggled)
            return true
        }
        return false
    }

    override fun sendKeyPresses(isDown: Boolean) {
        isActivated = isDown
        sendKeyPressesWithoutActivation(isDown)
    }

    /**
     * キー入力をMinecraftエンジンに送信します。
     * @param isDown 押下状態
     */
    private fun sendKeyPressesWithoutActivation(isDown: Boolean) {
        for (keycode in mProperties.keycodes) {
            if (keycode >= GLFW_KEY_UNKNOWN.toInt()) {
                sendKeyPress(keycode, CallbackBridge.getCurrentMods(), isDown)
                CallbackBridge.setModifiers(keycode, isDown)
            } else {
                sendSpecialKey(keycode, isDown)
            }
        }
    }

    /** @return リピート間隔（ミリ秒） */
    private fun getRepeatIntervalMs(): Int {
        val cps = Math.max(1, mProperties.repeatCps)
        return Math.max(1, 1000 / cps)
    }

    /** ボタンのリピート処理を開始します。 */
    private fun startButtonRepeat() {
        stopButtonRepeat()
        mRepeatHandler.postDelayed(mRepeatRunnable, Math.max(0, mProperties.repeatLongPressDelayMs).toLong())
    }

    /** ボタンのリピート処理を停止します。 */
    private fun stopButtonRepeat() {
        mRepeatHandler.removeCallbacks(mRepeatRunnable)
    }

    /**
     * 特殊キーコードに対応するアクションを実行します。
     * @param keycode 特殊キーコード
     * @param isDown 押下状態
     */
    private fun sendSpecialKey(keycode: Int, isDown: Boolean) {
        when (keycode) {
            ControlData.SPECIALBTN_KEYBOARD -> if (isDown) MainActivity.switchKeyboardState()
            ControlData.SPECIALBTN_TOGGLECTRL -> if (isDown) mControlLayout.toggleControlVisible()
            ControlData.SPECIALBTN_VIRTUALMOUSE -> if (isDown) MainActivity.toggleMouse(context)
            ControlData.SPECIALBTN_MOUSEPRI -> sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), isDown)
            ControlData.SPECIALBTN_MOUSEMID -> sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE.toInt(), isDown)
            ControlData.SPECIALBTN_MOUSESEC -> sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), isDown)
            ControlData.SPECIALBTN_SCROLLDOWN -> if (isDown) CallbackBridge.sendScroll(0.0, 1.0)
            ControlData.SPECIALBTN_SCROLLUP -> if (isDown) CallbackBridge.sendScroll(0.0, -1.0)
            ControlData.SPECIALBTN_MENU -> mControlLayout.notifyAppMenu()
        }
    }

    override fun hasOverlappingRendering(): Boolean = false
}
