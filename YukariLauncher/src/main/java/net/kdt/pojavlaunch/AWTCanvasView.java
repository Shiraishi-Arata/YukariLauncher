package net.kdt.pojavlaunch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;

import net.kdt.pojavlaunch.utils.JREUtils;

import java.util.LinkedList;

/**
 * AWTキャンバスを表示するTextureView。スレッドでレンダリングを行い、FPSを表示します。
 */
public class AWTCanvasView extends TextureView implements TextureView.SurfaceTextureListener, Runnable {
    public static final int AWT_CANVAS_WIDTH = (int) (Tools.currentDisplayMetrics.widthPixels * 0.8);
    public static final int AWT_CANVAS_HEIGHT = (int) (Tools.currentDisplayMetrics.heightPixels * 0.8);
    private static final int MAX_SIZE = 100;
    private static final double NANOS = 1000000000.0;
    private boolean mIsDestroyed = false;
    private final TextPaint mFpsPaint;

    // 一時的なFPSカウント用 https://stackoverflow.com/a/13729241
    private final LinkedList<Long> mTimes = new LinkedList<Long>(){{add(System.nanoTime());}};

    /**
     * コンテキストのみを受け取るコンストラクタ。
     * @param ctx コンテキスト
     */
    public AWTCanvasView(Context ctx) {
        this(ctx, null);
    }

    /**
     * 属性も受け取るコンストラクタ。ペイントの初期化とサーフェイスリスナーの設定を行います。
     * @param ctx コンテキスト
     * @param attrs 属性セット
     */
    public AWTCanvasView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);

        mFpsPaint = new TextPaint();
        mFpsPaint.setColor(Color.WHITE);
        mFpsPaint.setTextSize(24);

        setSurfaceTextureListener(this);

        post(this::refreshSize);
    }

    /**
     * サーフェイステクスチャが利用可能になったときに呼び出されます。バッファサイズを設定し、レンダリングスレッドを開始します。
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int w, int h) {
        getSurfaceTexture().setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT);
        mIsDestroyed = false;
        new Thread(this, "AndroidAWTRenderer").start();
    }

    /**
     * サーフェイステクスチャが破棄されたときに呼び出されます。
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        mIsDestroyed = true;
        return true;
    }

    /**
     * サーフェイステクスチャのサイズが変更されたときに呼び出されます。
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int w, int h) {
        getSurfaceTexture().setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT);
    }

    /**
     * サーフェイステクスチャが更新されたときに呼び出されます。
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        getSurfaceTexture().setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT);
    }

    /**
     * メインレンダリングループ。AWTフレームを描画し、FPSを表示します。
     */
    @Override
    public void run() {
        Canvas canvas;
        Surface surface = new Surface(getSurfaceTexture());
        Bitmap rgbArrayBitmap = Bitmap.createBitmap(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        try {
            while (!mIsDestroyed && surface.isValid()) {
                canvas = surface.lockCanvas(null);
                canvas.drawRGB(0, 0, 0);
                int[] rgbArray = JREUtils.renderAWTScreenFrame(/* canvas, mWidth, mHeight */);
                boolean mDrawing = rgbArray != null;
                if (rgbArray != null) {
                    canvas.save();
                    rgbArrayBitmap.setPixels(rgbArray, 0, AWT_CANVAS_WIDTH, 0, 0, AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT);
                    canvas.drawBitmap(rgbArrayBitmap, 0, 0, paint);
                    canvas.restore();
                }
                canvas.drawText("FPS: " + (Math.round(fps() * 10) / 10) + ", drawing=" + mDrawing, 20, 20, mFpsPaint);
                surface.unlockCanvasAndPost(canvas);
            }
        } catch (Throwable throwable) {
            Tools.showError(getContext(), throwable);
        }
        rgbArrayBitmap.recycle();
        surface.release();
    }

    /**
     * フレームレート（FPS）を計算して返します。
     */
    private double fps() {
        long lastTime = System.nanoTime();
        double difference = (lastTime - mTimes.getFirst()) / NANOS;
        mTimes.addLast(lastTime);
        int size = mTimes.size();
        if (size > MAX_SIZE) {
            mTimes.removeFirst();
        }
        return difference > 0 ? mTimes.size() / difference : 0.0;
    }

    /**
     * サーフェイスのアスペクト比に合わせてビューのサイズを調整します。
     */
    private void refreshSize(){
        ViewGroup.LayoutParams layoutParams = getLayoutParams();

        if(getHeight() < getWidth()){
            layoutParams.width = AWT_CANVAS_WIDTH * getHeight() / AWT_CANVAS_HEIGHT;
        }else{
            layoutParams.height = AWT_CANVAS_HEIGHT * getWidth() / AWT_CANVAS_WIDTH;
        }

        setLayoutParams(layoutParams);
    }

}
