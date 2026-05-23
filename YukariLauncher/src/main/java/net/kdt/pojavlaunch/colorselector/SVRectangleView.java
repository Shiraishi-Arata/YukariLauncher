package net.kdt.pojavlaunch.colorselector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.Tools;

/**
 * 彩度（Saturation）と明度（Value）を選択するための矩形ビュー。
 */
public class SVRectangleView extends View {
    private final Paint mColorPaint = new Paint();
    private final Paint mPointerPaint = new Paint();
    private final float mPointerSize;
    private Bitmap mSvRectangle;
    private RectF mViewSize;
    private float mHeightInverted;
    private float mWidthInverted;
    private float mFingerPosX;
    private float mFingerPosY;
    RectangleSelectionListener mRectSelectionListener;

    /**
     * コンストラクタ。ペイントオブジェクトを初期化します。
     */
    public SVRectangleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mColorPaint.setColor(Color.BLACK);
        mColorPaint.setStyle(Paint.Style.FILL);
        mPointerSize = Tools.dpToPx(6);
        mPointerPaint.setColor(Color.BLACK);
        mPointerPaint.setStrokeWidth(Tools.dpToPx(3));
    }

    /**
     * タッチイベントを処理し、彩度と明度を計算します。
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        mFingerPosX = x * mWidthInverted;
        mFingerPosY = y * mHeightInverted;

        if(mFingerPosX < 0) mFingerPosX = 0;
        else if(mFingerPosX > 1) mFingerPosX = 1;

        if(mFingerPosY < 0) mFingerPosY = 0;
        else if(mFingerPosY > 1) mFingerPosY = 1;

        if(mRectSelectionListener != null) mRectSelectionListener.onLuminosityIntensityChanged(mFingerPosY,mFingerPosX);
        invalidate();
        return true;
    }

    /**
     * 彩度と明度を設定します。
     */
    public void setLuminosityIntensity(float luminosity, float intensity) {
        mFingerPosX = intensity;
        mFingerPosY = luminosity;
        invalidate();
    }

    /**
     * ベースカラーを設定します。
     */
    public void setColor(int color, boolean invalidate) {
        mColorPaint.setColor(color);
        if(invalidate) invalidate();
    }

    /**
     * 矩形選択リスナーを設定します。
     */
    public void setRectSelectionListener(RectangleSelectionListener listener) {
        mRectSelectionListener = listener;
    }

    /**
     * ポインター（十字線）を描画します。
     */
    protected void drawPointer(Canvas canvas, float x, float y) {
        canvas.drawLine(mPointerSize * 2 + x, y, mPointerSize + x, y, mPointerPaint);
        canvas.drawLine(x - mPointerSize * 2, y, x - mPointerSize, y, mPointerPaint);
        canvas.drawLine(x, mPointerSize * 2 + y, x, mPointerSize + y, mPointerPaint);
        canvas.drawLine(x, y - mPointerSize * 2, x, y - mPointerSize, mPointerPaint);
    }

    /**
     * ビューを描画します。ベースカラー、SVグラデーション、ポインターを描画します。
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(mViewSize, mColorPaint);
        canvas.drawBitmap(mSvRectangle, 0,0, null);
        drawPointer(canvas, mViewSize.right * mFingerPosX, mViewSize.bottom * mFingerPosY);
    }

    /**
     * サイズ変更時にSV矩形ビットマップを再生成します。
     */
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        mViewSize = new RectF(0,0, w,h);
        mWidthInverted = 1/mViewSize.right;
        mHeightInverted = 1/mViewSize.bottom;
        if(w > 0 && h > 0)
            regenerateRectangle();
    }

    /**
     * SVグラデーションのビットマップを生成します。
     */
    protected void regenerateRectangle() {
        int w = getWidth();
        int h = getHeight();
        if(mSvRectangle != null)
            mSvRectangle.recycle();
        mSvRectangle = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Paint rectPaint = new Paint();
        Canvas canvas = new Canvas(mSvRectangle);
        float h2f = h/2f;
        float w2f = w/2f;
        rectPaint.setShader(new LinearGradient(0,h2f, w, h2f, Color.WHITE,0, Shader.TileMode.CLAMP));
        canvas.drawRect(mViewSize, rectPaint);
        rectPaint.setShader(new LinearGradient(w2f,0, w2f, h, Color.BLACK,0, Shader.TileMode.CLAMP));
        canvas.drawRect(mViewSize, rectPaint);
    }
}
