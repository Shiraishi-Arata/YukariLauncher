package com.arata.yukarilauncher.ui.subassembly.view

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import com.arata.yukarilauncher.utils.YLTools
import kotlin.math.max
import kotlin.math.min

/**
 * ドラッグ可能なビューをラップするクラス
 */
/**
 * ドラッグ可能なビューをラップするクラス。
 * @param mainView ドラッグ対象のビュー
 * @param fetcher 位置情報の取得・設定を行うインターフェース
 */
class DraggableViewWrapper(private val mainView: View, private val fetcher: AttributesFetcher) {
    private var lastUpdateTime: Long = 0
    private var initialX = 0f
    private var initialY = 0f
    private var touchX = 0f
    private var touchY = 0f

    /**
     * ドラッグ処理を初期化する
     */
    @SuppressLint("ClickableViewAccessibility")
    fun init() {
        mainView.setOnTouchListener { _: View?, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (updateRateLimits()) return@setOnTouchListener false

                    initialX = fetcher.get()[0].toFloat()
                    initialY = fetcher.get()[1].toFloat()
                    touchX = event.rawX
                    touchY = event.rawY
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (updateRateLimits()) return@setOnTouchListener false

                    val x = max(fetcher.screenPixels.minX.toDouble(), min(fetcher.screenPixels.maxX.toDouble(),
                        (initialX + (event.rawX - touchX)).toDouble())
                    ).toInt()
                    val y = max(fetcher.screenPixels.minY.toDouble(), min(fetcher.screenPixels.maxY.toDouble(),
                        (initialY + (event.rawY - touchY)).toDouble())
                    ).toInt()
                    fetcher.set(x, y)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    /**
     * 過度に頻繁な更新によるパフォーマンス低下を回避する
     */
    private fun updateRateLimits(): Boolean {
        var limit = false
        val millis = YLTools.getCurrentTimeMillis()
        if (millis - lastUpdateTime < 5) limit = true
        lastUpdateTime = millis
        return limit
    }

    /**
     * ドラッグ可能なビューの属性を取得/設定するインターフェース
     */
    interface AttributesFetcher {
        /** 画面のピクセル制限値を取得する */
        val screenPixels: ScreenPixels
        /** x, y座標を取得する */
        fun get(): IntArray
        /** x, y座標を設定する */
        fun set(x: Int, y: Int)
    }

    /**
     * 画面の座標制限を表すクラス
     */
    class ScreenPixels(var minX: Int, var minY: Int, var maxX: Int, var maxY: Int)
}
