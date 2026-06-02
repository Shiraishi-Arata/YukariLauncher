package com.arata.yukarilauncher.ui.view.colorselector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.arata.yukarilauncher.Tools

/** 色相を選択するための縦方向スライダービュー。全色相のグラデーションを表示する。 */
class HueView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    /** 選択位置を示す線のペイント。 */
    private val blackPaint = Paint()
    /** 色相グラデーションビットマップ。 */
    private var mGamma: Bitmap? = null
    /** 色相選択リスナー。 */
    private var mHueSelectionListener: HueSelectionListener? = null
    /** 現在選択中の色相値。 */
    private var mSelectionHue = 0f
    /** ピクセル→色相 変換係数。 */
    private var mHeightHueRatio = 0f
    /** 色相→ピクセル 変換係数。 */
    private var mHueHeightRatio = 0f
    /** ビューの幅。 */
    private var mWidth = 0f
    /** ビューの高さ。 */
    private var mHeight = 0f
    /** ビュー幅の3分の1。 */
    private var mWidthThird = 0f

    init {
        blackPaint.apply {
            color = Color.BLACK
            strokeWidth = Tools.dpToPx(3f)
        }
    }

    /** 色相選択リスナーを設定する。 */
    fun setHueSelectionListener(listener: HueSelectionListener?) {
        mHueSelectionListener = listener
    }

    /** 色相値を設定し、ビューを再描画する。 @param hue 設定する色相値（0～360） */
    fun setHue(hue: Float) {
        mSelectionHue = hue
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        mSelectionHue = event.y * mHeightHueRatio
        invalidate()
        mHueSelectionListener?.onHueSelected(mSelectionHue)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        mGamma?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        val linePos = mSelectionHue * mHueHeightRatio
        canvas.drawLine(0f, linePos, mWidthThird, linePos, blackPaint)
        canvas.drawLine(mWidthThird * 2, linePos, mWidth, linePos, blackPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        mWidth = w.toFloat()
        mHeight = h.toFloat()
        mWidthThird = mWidth / 3
        regenerateGammaBitmap()
    }

    /** 色相のグラデーションビットマップを生成する。 */
    protected fun regenerateGammaBitmap() {
        mGamma?.recycle()
        mGamma = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        val canvas = Canvas(mGamma!!)
        mHeightHueRatio = 360f / mHeight
        mHueHeightRatio = mHeight / 360f
        val hsvFiller = floatArrayOf(0f, 1f, 1f)
        var i = 0f
        while (i < mHeight) {
            hsvFiller[0] = i * mHeightHueRatio
            paint.color = Color.HSVToColor(hsvFiller)
            canvas.drawLine(0f, i, mWidth, i, paint)
            i++
        }
    }
}