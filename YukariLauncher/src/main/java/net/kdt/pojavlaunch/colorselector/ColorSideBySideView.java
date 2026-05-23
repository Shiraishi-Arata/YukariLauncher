package net.kdt.pojavlaunch.colorselector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import top.defaults.checkerboarddrawable.CheckerboardDrawable;

/**
 * 色を2分割（不透明版とアルファ版）で表示するビュー。
 */
public class ColorSideBySideView extends View {
    private final Paint mPaint;
    private final CheckerboardDrawable mCheckerboardDrawable = CheckerboardDrawable.create();
    private int mColor;
    private int mAlphaColor;
    private float mWidth;
    private float mHeight;
    private float mHalfHeight;

    /**
     * コンストラクタ。ペイントを初期化します。
     */
    public ColorSideBySideView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
    }

    /**
     * 表示する色を設定します。
     * @param color ARGB色
     */
    public void setColor(int color) {
        mColor = ColorSelector.setAlpha(color, 0xff);
        mAlphaColor = color;
        invalidate();
    }

    /**
     * ビューを描画します。上半分は不透明、下半分は元の透明度で描画します。
     */
    @Override
    protected void onDraw(Canvas canvas) {
        mCheckerboardDrawable.draw(canvas);
        mPaint.setColor(mColor);
        canvas.drawRect(0,0,mWidth, mHalfHeight, mPaint);
        mPaint.setColor(mAlphaColor);
        canvas.drawRect(0,mHalfHeight,mWidth,mHeight, mPaint);
    }

    /**
     * サイズ変更時に内部パラメータを更新します。
     */
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        mHalfHeight = h/2f;
        mWidth = w;
        mHeight = h;
    }
}
