package com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.res.ResourcesCompat
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlInterface

/**
 * コントロール編集時のサイズ変更ハンドルビュー。
 * 選択されたボタンの右下に表示され、ドラッグでサイズを変更できます。
 */
class ControlHandleView : View {
    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init() }

    private val mDrawable: Drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_view_handle, context.theme)!!
    private var mView: ControlInterface? = null
    private var mXOffset = 0f
    private var mYOffset = 0f

    private val mPositionListener = ViewTreeObserver.OnPreDrawListener {
        if (mView == null || !mView!!.controlView.isShown) {
            hide()
            return@OnPreDrawListener true
        }
        x = mView!!.controlView.x + mView!!.controlView.width
        y = mView!!.controlView.y + mView!!.controlView.height
        true
    }

    private fun init() {
        val size = resources.getDimensionPixelOffset(R.dimen._22sdp)
        mDrawable.setBounds(0, 0, size, size)
        val params = ViewGroup.LayoutParams(size, size)
        layoutParams = params
        background = mDrawable
        translationZ = 10.5f
    }

    /**
     * サイズ変更ハンドルを指定されたコントロールにアタッチします。
     * @param controlInterface 編集対象のコントロール
     */
    fun setControlButton(controlInterface: ControlInterface) {
        mView?.let {
            it.controlView.viewTreeObserver.removeOnPreDrawListener(mPositionListener)
        }
        visibility = VISIBLE
        mView = controlInterface
        mView!!.controlView.viewTreeObserver.addOnPreDrawListener(mPositionListener)
        x = controlInterface.controlView.x + controlInterface.controlView.width
        y = controlInterface.controlView.y + controlInterface.controlView.height
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mXOffset = event.x
                mYOffset = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                x += event.x - mXOffset
                y += event.y - mYOffset

                mView!!.properties.setWidth(x - mView!!.controlView.x)
                mView!!.properties.setHeight(y - mView!!.controlView.y)
                mView!!.regenerateDynamicCoordinates()
            }
        }
        return true
    }

    /** ハンドルを非表示にします。 */
    fun hide() {
        mView?.let {
            it.controlView.viewTreeObserver.removeOnPreDrawListener(mPositionListener)
        }
        visibility = GONE
    }
}