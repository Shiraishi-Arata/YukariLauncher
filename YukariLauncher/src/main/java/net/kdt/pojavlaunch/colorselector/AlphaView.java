package net.kdt.pojavlaunch.colorselector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.math.MathUtils;

import net.kdt.pojavlaunch.Tools;

import top.defaults.checkerboarddrawable.CheckerboardDrawable;

/**
 * アルファ（透明度）値を選択するためのビュー。チェッカーボードの上に透明度グラデーションを表示します。
 */
public class AlphaView extends View {
    private final Drawable mCheckerboardDrawable = CheckerboardDrawable.create();
    private final Paint mShaderPaint = new Paint();
    private final Paint mBlackPaint;
    private final RectF mViewSize = new RectF(0,0,0,0);
    private AlphaSelectionListener mAlphaSelectionListener;
    private int mSelectedAlpha;
    private float mAlphaDiv; // 高速な位置→アルファ値変換用
    private float mScreenDiv; // 高速なアルファ値→位置変換用
    private float mWidthThird; // カーソル表示用のビュー幅の1/3

    /**
     * コンストラクタ。ペイントオブジェクトを初期化します。
     */
    public AlphaView(Context ctx, AttributeSet attrs) {
        super(ctx,attrs);
        mBlackPaint = new Paint();
        mBlackPaint.setStrokeWidth(Tools.dpToPx(3));
        mBlackPaint.setColor(Color.BLACK);
    }

    /**
     * アルファ選択リスナーを設定します。
     */
    public void setAlphaSelectionListener(AlphaSelectionListener alphaSelectionListener) {
        mAlphaSelectionListener = alphaSelectionListener;
    }

    /**
     * アルファ値を設定し、再描画します。
     */
    public void setAlpha(int alpha) {
        mSelectedAlpha = alpha;
        invalidate();
    }

    /**
     * タッチイベントを処理し、アルファ値を計算します。
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mSelectedAlpha = (int) MathUtils.clamp(mAlphaDiv * event.getY(), 0, 0xff);
        if(mAlphaSelectionListener != null) mAlphaSelectionListener.onAlphaSelected(mSelectedAlpha);
        invalidate();
        return true;
    }

    /**
     * サイズ変更時にグラデーションと変換値を再計算します。
     */
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        mViewSize.right = w;
        mViewSize.bottom = h;
        mShaderPaint.setShader(new LinearGradient(0,0,0,h, 0, Color.WHITE, Shader.TileMode.REPEAT));
        mAlphaDiv = 255f / mViewSize.bottom;
        mScreenDiv = mViewSize.bottom / 255f;
        mWidthThird = mViewSize.right / 3f;
    }

    /**
     * ビューを描画します。チェッカーボード、アルファグラデーション、選択インジケーターを描画します。
     */
    @Override
    protected void onDraw(Canvas canvas) {
        mCheckerboardDrawable.draw(canvas);
        canvas.drawRect(mViewSize, mShaderPaint);
        float linePos = mSelectedAlpha * mScreenDiv;
        canvas.drawLine(0, linePos , mWidthThird, linePos, mBlackPaint);
        canvas.drawLine(mWidthThird * 2, linePos, getRight(),linePos, mBlackPaint);
    }
}
