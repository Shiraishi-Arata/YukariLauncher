package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import com.arata.yukarilauncher.Tools.currentDisplayMetrics
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.image.Dimension
import com.arata.yukarilauncher.utils.image.ImageUtils
import com.arata.yukarilauncher.utils.mouse.CursorDrawableUtils
import com.arata.yukarilauncher.ui.view.GrabListener
import org.lwjgl.glfw.CallbackBridge

/**
 * タッチスクリーン上の仮想マウス SurfaceView。
 * マウスカーソルの描画、位置管理、表示/非表示切り替えを行います。
 */
class Touchpad : View, GrabListener, AbstractTouchpad {
    private var mDisplayState = false
    private var mMousePointerDrawable: Drawable? = null
    private var mLastCursorType = Int.MIN_VALUE
    private var mMouseX = 0f
    private var mMouseY = 0f

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init() }

    private fun _enable() {
        visibility = VISIBLE
        placeMouseAt(currentDisplayMetrics.widthPixels / 2f, currentDisplayMetrics.heightPixels / 2f)
    }

    private fun _disable() {
        visibility = GONE
    }

    /**
     * 表示状態を切り替えます。
     * @return 切り替え後の表示状態
     */
    fun switchState(): Boolean {
        mDisplayState = !mDisplayState
        if (!CallbackBridge.isGrabbing()) {
            if (mDisplayState) _enable()
            else _disable()
        }
        return mDisplayState
    }

    /**
     * マウスカーソルを指定位置に配置します。
     * @param x X座標
     * @param y Y座標
     */
    fun placeMouseAt(x: Float, y: Float) {
        mMouseX = x
        mMouseY = y
        updateMousePosition()
    }

    /** マウス位置をMinecraftエンジンに送信します。 */
    private fun sendMousePosition() {
        CallbackBridge.sendCursorPos(mMouseX * AllStaticSettings.scaleFactor, mMouseY * AllStaticSettings.scaleFactor)
    }

    /** マウス位置を更新して再描画します。 */
    private fun updateMousePosition() {
        sendMousePosition()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cursorType = CallbackBridge.getCurrentCursorType()
        if (cursorType != mLastCursorType) {
            updateMouseDrawable()
        }
        canvas.translate(mMouseX, mMouseY)
        mMousePointerDrawable!!.draw(canvas)
    }

    private fun init() {
        updateMouseDrawable()
        assert(mMousePointerDrawable != null)
        updateMouseScale()
        isFocusable = false
        setDefaultFocusHighlightEnabled(false)
        disable()
        mDisplayState = false
    }

    /** マウスカーソルのスケールを更新します。 */
    fun updateMouseScale() {
        val mousescale = ImageUtils.resizeWithRatio(
            mMousePointerDrawable!!.intrinsicWidth, mMousePointerDrawable!!.intrinsicHeight,
            AllSettings.mouseScale.getValue()
        )
        val scaledWidth = (mousescale.width * 0.5).toInt()
        val scaledHeight = (mousescale.height * 0.5).toInt()
        val hotspot = CursorDrawableUtils.getScaledHotspot(mMousePointerDrawable!!, scaledWidth, scaledHeight)
        mMousePointerDrawable!!.setBounds(-hotspot[0], -hotspot[1], scaledWidth - hotspot[0], scaledHeight - hotspot[1])
    }

    /** マウスカーソルの描画を更新します。 */
    fun updateMouseDrawable() {
        mLastCursorType = CallbackBridge.getCurrentCursorType()
        mMousePointerDrawable = YLTools.customMouse(context)
        mMousePointerDrawable!!.setCallback(this)
        mMousePointerDrawable!!.setVisible(true, true)
        if (mMousePointerDrawable is Animatable) {
            (mMousePointerDrawable as Animatable).start()
        }
        updateMouseScale()
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who === mMousePointerDrawable || super.verifyDrawable(who)
    }

    override fun invalidateDrawable(drawable: Drawable) {
        if (drawable === mMousePointerDrawable) {
            invalidate()
            return
        }
        super.invalidateDrawable(drawable)
    }

    override fun onGrabState(isGrabbing: Boolean) {
        post { updateGrabState(isGrabbing) }
    }

    /** @param isGrabbing グラブ状態に応じて表示を更新 */
    private fun updateGrabState(isGrabbing: Boolean) {
        if (!isGrabbing) {
            if (mDisplayState && visibility != VISIBLE) _enable()
            if (!mDisplayState && visibility == VISIBLE) _disable()
        } else {
            if (visibility != GONE) _disable()
        }
    }

    override val displayState: Boolean
        get() = mDisplayState

    override fun applyMotionVector(x: Float, y: Float) {
        mMouseX = Math.max(0f, Math.min(currentDisplayMetrics.widthPixels.toFloat(), mMouseX + x * (AllSettings.mouseSpeed.getValue() / 100f)))
        mMouseY = Math.max(0f, Math.min(currentDisplayMetrics.heightPixels.toFloat(), mMouseY + y * (AllSettings.mouseSpeed.getValue() / 100f)))
        updateMousePosition()
    }

    override fun enable(supposed: Boolean) {
        if (mDisplayState) return
        mDisplayState = true
        if (supposed && CallbackBridge.isGrabbing()) return
        _enable()
    }

    override fun disable() {
        if (!mDisplayState) return
        mDisplayState = false
        _disable()
    }
}
