package net.kdt.pojavlaunch.colorselector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.Tools;

/**
 * 色相を選択するためのビュー。色相グラデーションを表示し、タッチで選択できます。
 */
public class HueView extends View {
    private final Paint blackPaint = new Paint();
    private Bitmap mGamma;
    private HueSelectionListener mHueSelectionListener;
    private float mSelectionHue;
    private float mHeightHueRatio;
    private float mHueHeightRatio;
    private float mWidth;
    private float mHeight;
    private float mWidthThird;

    /**
     * コンストラクタ。ペイントを初期化します。
     */
    public HueView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        blackPaint.setColor(Color.BLACK);
        blackPaint.setStrokeWidth(Tools.dpToPx(3));
    }

    /**
     * 色相選択リスナーを設定します。
     */
    public void setHueSelectionListener(HueSelectionListener listener) {
        mHueSelectionListener = listener;
    }

    /**
     * 色相を設定し、再描画します。
     */
    public void setHue(float hue) {
        mSelectionHue = hue;
        invalidate();
    }

    /**
     * タッチイベントで色相を選択します。
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mSelectionHue = event.getY() * mHeightHueRatio;
        invalidate();
        if(mHueSelectionListener != null) mHueSelectionListener.onHueSelected(mSelectionHue);
        return true;
    }

    /**
     * ビューを描画します。色相ビットマップと選択インジケーターを描画します。
     */
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mGamma, 0, 0 ,null);
        float linePos = mSelectionHue * mHueHeightRatio;
        canvas.drawLine(0, linePos , mWidthThird, linePos, blackPaint);
        canvas.drawLine( mWidthThird * 2 ,linePos, mWidth, linePos, blackPaint);
    }

    /**
     * サイズ変更時に色相ビットマップを再生成します。
     */
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        mWidth = w;
        mHeight = h;
        mWidthThird = mWidth / 3;
        regenerateGammaBitmap();
    }

    /**
     * 色相グラデーションのビットマップを生成します。
     */
    protected void regenerateGammaBitmap() {
        if(mGamma != null)
            mGamma.recycle();
        mGamma = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(mGamma);
        mHeightHueRatio = 360/ mHeight;
        mHueHeightRatio = mHeight / 360;
        float[] hsvFiller = new float[] {0, 1, 1};
        for(float i = 0; i < mHeight; i++) {
            hsvFiller[0] = i * mHeightHueRatio;
            paint.setColor(Color.HSVToColor(hsvFiller));
            canvas.drawLine(0,i,mWidth, i,paint);
        }
    }
}
