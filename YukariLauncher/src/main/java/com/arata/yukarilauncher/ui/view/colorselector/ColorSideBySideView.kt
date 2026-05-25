package com.arata.yukarilauncher.ui.view.colorselector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import top.defaults.checkerboarddrawable.CheckerboardDrawable

/** 選択色とそのアルファ適用色を上下に並べて表示するプレビュービュー。 */
class ColorSideBySideView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    /** 描画用ペイント。 */
    private val mPaint = Paint()
    /** 透明部分を表現するチェッカーボード描画用Drawable。 */
    private val mCheckerboardDrawable = CheckerboardDrawable.create()
    /** 不透明度100%の色。 */
    private var mColor = 0
    /** 実際のアルファ適用色。 */
    private var mAlphaColor = 0
    /** ビューの幅。 */
    private var mWidth = 0f
    /** ビューの高さ。 */
    private var mHeight = 0f
    /** ビューの高さの半分。 */
    private var mHalfHeight = 0f

    /** 表示する色を設定する。 @param color 表示する色 */
    fun setColor(color: Int) {
        mColor = Color.argb(0xff, Color.red(color), Color.green(color), Color.blue(color))
        mAlphaColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        mCheckerboardDrawable.draw(canvas)
        mPaint.color = mColor
        canvas.drawRect(0f, 0f, mWidth, mHalfHeight, mPaint)
        mPaint.color = mAlphaColor
        canvas.drawRect(0f, mHalfHeight, mWidth, mHeight, mPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        mHalfHeight = h / 2f
        mWidth = w.toFloat()
        mHeight = h.toFloat()
    }
}
