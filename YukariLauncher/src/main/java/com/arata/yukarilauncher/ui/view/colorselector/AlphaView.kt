package com.arata.yukarilauncher.ui.view.colorselector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.math.MathUtils
import com.arata.yukarilauncher.Tools
import top.defaults.checkerboarddrawable.CheckerboardDrawable

/** アルファ値（透明度）を選択するための縦方向スライダービュー。 */
class AlphaView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    /** 透明部分を表現するチェッカーボード描画用Drawable。 */
    private val mCheckerboardDrawable: Drawable = CheckerboardDrawable.create()
    /** シェーダー（白→透明のグラデーション）描画用ペイント。 */
    private val mShaderPaint = Paint()
    /** 選択位置を示す線のペイント。 */
    private val mBlackPaint: Paint
    /** ビューの矩形領域。 */
    private val mViewSize = RectF(0f, 0f, 0f, 0f)
    /** アルファ選択リスナー。 */
    private var mAlphaSelectionListener: AlphaSelectionListener? = null
    /** 現在選択中のアルファ値。 */
    private var mSelectedAlpha = 0
    /** ピクセル→アルファ値 変換係数。 */
    private var mAlphaDiv = 0f
    /** アルファ値→ピクセル 変換係数。 */
    private var mScreenDiv = 0f
    /** ビュー幅の3分の1。 */
    private var mWidthThird = 0f

    init {
        mBlackPaint = Paint().apply {
            strokeWidth = Tools.dpToPx(3f)
            color = Color.BLACK
        }
    }

    /** アルファ選択リスナーを設定する。 */
    fun setAlphaSelectionListener(alphaSelectionListener: AlphaSelectionListener?) {
        mAlphaSelectionListener = alphaSelectionListener
    }

    /** アルファ値を設定し、ビューを再描画する。 @param alpha 設定するアルファ値（0～255） */
    fun setAlpha(alpha: Int) {
        mSelectedAlpha = alpha
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        mSelectedAlpha = MathUtils.clamp(mAlphaDiv * event.y, 0.0f, 255.0f).toInt()
        mAlphaSelectionListener?.onAlphaSelected(mSelectedAlpha)
        invalidate()
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        mViewSize.right = w.toFloat()
        mViewSize.bottom = h.toFloat()
        mShaderPaint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0, Color.WHITE, Shader.TileMode.REPEAT)
        mAlphaDiv = 255f / mViewSize.bottom
        mScreenDiv = mViewSize.bottom / 255f
        mWidthThird = mViewSize.right / 3f
    }

    override fun onDraw(canvas: Canvas) {
        mCheckerboardDrawable.draw(canvas)
        canvas.drawRect(mViewSize, mShaderPaint)
        val linePos = mSelectedAlpha * mScreenDiv
        canvas.drawLine(0f, linePos, mWidthThird, linePos, mBlackPaint)
        canvas.drawLine(mWidthThird * 2, linePos, right.toFloat(), linePos, mBlackPaint)
    }
}
