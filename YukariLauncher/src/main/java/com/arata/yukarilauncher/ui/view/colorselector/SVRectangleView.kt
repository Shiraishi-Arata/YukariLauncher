package com.arata.yukarilauncher.ui.view.colorselector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.arata.yukarilauncher.Tools

/** 彩度（Saturation）と明度（Value）を2次元で選択する矩形ビュー。 */
class SVRectangleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    /** ベース色描画用ペイント。 */
    private val mColorPaint = Paint()
    /** ポインター描画用ペイント。 */
    private val mPointerPaint = Paint()
    /** ポインター十字線のサイズ。 */
    private val mPointerSize: Float
    /** SV矩形のビットマップキャッシュ。 */
    private var mSvRectangle: Bitmap? = null
    /** ビューの矩形領域。 */
    private var mViewSize = RectF(0f, 0f, 0f, 0f)
    /** 高さ反転係数（1/height）。 */
    private var mHeightInverted = 0f
    /** 幅反転係数（1/width）。 */
    private var mWidthInverted = 0f
    /** 指のX座標（正規化済み）。 */
    private var mFingerPosX = 0f
    /** 指のY座標（正規化済み）。 */
    private var mFingerPosY = 0f
    /** 矩形選択リスナー。 */
    var mRectSelectionListener: RectangleSelectionListener? = null

    init {
        mColorPaint.apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        mPointerSize = Tools.dpToPx(6f)
        mPointerPaint.apply {
            color = Color.BLACK
            strokeWidth = Tools.dpToPx(3f)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        mFingerPosX = x * mWidthInverted
        mFingerPosY = y * mHeightInverted

        if (mFingerPosX < 0) mFingerPosX = 0f
        else if (mFingerPosX > 1) mFingerPosX = 1f

        if (mFingerPosY < 0) mFingerPosY = 0f
        else if (mFingerPosY > 1) mFingerPosY = 1f

        mRectSelectionListener?.onLuminosityIntensityChanged(mFingerPosY, mFingerPosX)
        invalidate()
        return true
    }

    /** 彩度と明度を設定し、ポインター位置を更新する。 @param luminosity 明度 @param intensity 彩度 */
    fun setLuminosityIntensity(luminosity: Float, intensity: Float) {
        mFingerPosX = intensity
        mFingerPosY = luminosity
        invalidate()
    }

    /** ベース色を設定する。 @param color 設定する色 @param invalidate 即時再描画するか */
    fun setColor(color: Int, invalidate: Boolean) {
        mColorPaint.color = color
        if (invalidate) invalidate()
    }

    /** 矩形選択リスナーを設定する。 */
    fun setRectSelectionListener(listener: RectangleSelectionListener?) {
        mRectSelectionListener = listener
    }

    /** 指定位置に十字線のポインターを描画する。 @param x X座標 @param y Y座標 */
    protected fun drawPointer(canvas: Canvas, x: Float, y: Float) {
        canvas.drawLine(mPointerSize * 2 + x, y, mPointerSize + x, y, mPointerPaint)
        canvas.drawLine(x - mPointerSize * 2, y, x - mPointerSize, y, mPointerPaint)
        canvas.drawLine(x, mPointerSize * 2 + y, x, mPointerSize + y, mPointerPaint)
        canvas.drawLine(x, y - mPointerSize * 2, x, y - mPointerSize, mPointerPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(mViewSize, mColorPaint)
        mSvRectangle?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        drawPointer(canvas, mViewSize.right * mFingerPosX, mViewSize.bottom * mFingerPosY)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        mViewSize = RectF(0f, 0f, w.toFloat(), h.toFloat())
        mWidthInverted = 1f / mViewSize.right
        mHeightInverted = 1f / mViewSize.bottom
        if (w > 0 && h > 0)
            regenerateRectangle()
    }

    /** SV矩形のグラデーションビットマップを生成する。白→透明の横グラデーションと黒→透明の縦グラデーションでSV領域を表現する。 */
    protected fun regenerateRectangle() {
        val w = width
        val h = height
        mSvRectangle?.recycle()
        mSvRectangle = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val rectPaint = Paint()
        val canvas = Canvas(mSvRectangle!!)
        val h2f = h / 2f
        val w2f = w / 2f
        rectPaint.shader = LinearGradient(0f, h2f, w.toFloat(), h2f, Color.WHITE, 0, Shader.TileMode.CLAMP)
        canvas.drawRect(mViewSize, rectPaint)
        rectPaint.shader = LinearGradient(w2f, 0f, w2f, h.toFloat(), Color.BLACK, 0, Shader.TileMode.CLAMP)
        canvas.drawRect(mViewSize, rectPaint)
    }
}