package com.arata.yukarilauncher.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.runtime.JREUtils
import java.util.LinkedList

/**
 * AWTキャンバスを表示するTextureView実装。
 * Java AWTの画面描画をAndroidのTextureView上にレンダリングします。
 * FPS表示や画面サイズの自動調整機能を備えています。
 */
class AWTCanvasView : TextureView, TextureView.SurfaceTextureListener, Runnable {
    companion object {
        /** AWTキャンバスの幅（画面幅の80%）。 */
        val AWT_CANVAS_WIDTH: Int = (Tools.currentDisplayMetrics.widthPixels * 0.8).toInt()
        /** AWTキャンバスの高さ（画面高さの80%）。 */
        val AWT_CANVAS_HEIGHT: Int = (Tools.currentDisplayMetrics.heightPixels * 0.8).toInt()
        /** FPS計算用の最大サンプル数。 */
        private const val MAX_SIZE = 100
        /** ナノ秒から秒への変換定数。 */
        private const val NANOS = 1000000000.0
    }

    /** 破棄フラグ。 */
    private var mIsDestroyed = false
    /** FPS表示用のテキストペイント。 */
    private val mFpsPaint: TextPaint
    /** FPS計算用のタイムスタンプリスト。 */
    private val mTimes = LinkedList<Long>().apply { add(System.nanoTime()) }

    /**
     * コンテキストのみを使用してAWTCanvasViewを作成します。
     * @param ctx コンテキスト
     */
    constructor(ctx: Context) : this(ctx, null)

    /**
     * コンテキストと属性セットを使用してAWTCanvasViewを作成します。
     * @param ctx コンテキスト
     * @param attrs 属性セット（null可）
     */
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs) {
        mFpsPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 24f
        }
        setSurfaceTextureListener(this)
        post { refreshSize() }
    }

    /**
     * サーフェステクスチャが利用可能になったときに呼び出されます。
     * レンダリングスレッドを開始します。
     * @param texture 利用可能になったサーフェステクスチャ
     * @param w サーフェスの幅
     * @param h サーフェスの高さ
     */
    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, w: Int, h: Int) {
        surfaceTexture?.setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
        mIsDestroyed = false
        Thread(this, "AndroidAWTRenderer").start()
    }

    /**
     * サーフェステクスチャが破棄されるときに呼び出されます。
     * @param texture 破棄されるサーフェステクスチャ
     * @return 常にtrue
     */
    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        mIsDestroyed = true
        return true
    }

    /**
     * サーフェステクスチャのサイズが変更されたときに呼び出されます。
     * @param texture サイズ変更されたサーフェステクスチャ
     * @param w 新しい幅
     * @param h 新しい高さ
     */
    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, w: Int, h: Int) {
        surfaceTexture?.setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
    }

    /**
     * サーフェステクスチャが更新されたときに呼び出されます。
     * @param texture 更新されたサーフェステクスチャ
     */
    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
        surfaceTexture?.setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
    }

    /**
     * メインのレンダリングループ。
     * JREからAWTフレームデータを取得し、サーフェスに描画します。
     */
    override fun run() {
        val surface = Surface(surfaceTexture)
        val rgbArrayBitmap = Bitmap.createBitmap(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        try {
            while (!mIsDestroyed && surface.isValid) {
                val canvas = surface.lockCanvas(null)
                canvas.drawRGB(0, 0, 0)
                val rgbArray = JREUtils.renderAWTScreenFrame()
                val mDrawing = rgbArray != null
                if (rgbArray != null) {
                    canvas.save()
                    rgbArrayBitmap.setPixels(rgbArray, 0, AWT_CANVAS_WIDTH, 0, 0, AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
                    canvas.drawBitmap(rgbArrayBitmap, 0f, 0f, paint)
                    canvas.restore()
                }
                canvas.drawText("FPS: ${(Math.round(fps() * 10) / 10)}, drawing=$mDrawing", 20f, 20f, mFpsPaint)
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (throwable: Throwable) {
            Tools.showError(context, throwable)
        }
        rgbArrayBitmap.recycle()
        surface.release()
    }

    /**
     * 現在のFPS値を計算します。
     * @return 1秒あたりのフレーム数
     */
    private fun fps(): Double {
        val lastTime = System.nanoTime()
        val difference = (lastTime - mTimes.first) / NANOS
        mTimes.addLast(lastTime)
        val size = mTimes.size
        if (size > MAX_SIZE) {
            mTimes.removeFirst()
        }
        return if (difference > 0) mTimes.size / difference else 0.0
    }

    /**
     * アスペクト比を維持したままViewのサイズを調整します。
     */
    private fun refreshSize() {
        val layoutParams = layoutParams as ViewGroup.LayoutParams
        if (height < width) {
            layoutParams.width = AWT_CANVAS_WIDTH * height / AWT_CANVAS_HEIGHT
        } else {
            layoutParams.height = AWT_CANVAS_HEIGHT * width / AWT_CANVAS_WIDTH
        }
        setLayoutParams(layoutParams)
    }
}
