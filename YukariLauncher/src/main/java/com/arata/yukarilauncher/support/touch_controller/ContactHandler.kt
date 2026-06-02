package com.arata.yukarilauncher.support.touch_controller

import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.View
import top.fifthlight.touchcontroller.proxy.client.LauncherProxyClient
import top.fifthlight.touchcontroller.proxy.data.Offset

/**
 * タッチ操作の触点処理を行う
 * TouchController Modの制御プロキシに情報を提供する
 */
object ContactHandler {
    private val pointerIdMap = SparseIntArray()
    private var nextPointerId = 1

    /**
     * MotionEventからViewに対する相対座標オフセットを取得する
     */
    private fun MotionEvent.getOffset(index: Int, view: View) = Offset(
        getX(index) / view.width,
        getY(index) / view.height
    )

    /**
     * タッチイベントを処理し、制御プロキシに通知する
     * @param event モーションイベント
     * @param view タッチされたビュー
     */
    fun progressEvent(event: MotionEvent, view: View) {
        val client = ControllerProxy.getProxyClient() ?: return

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handlePointerDown(event, client, 0, view)

            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(event, client, event.actionIndex, view)

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = pointerIdMap.get(event.getPointerId(i))
                    client.addPointer(pointerId, event.getOffset(i, view))
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                client.clearPointer()
                pointerIdMap.clear()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val i = event.actionIndex
                val pointerId = pointerIdMap.get(event.getPointerId(i))
                if (pointerId != 0) {
                    pointerIdMap.delete(pointerId)
                    client.removePointer(pointerId)
                }
            }
        }
    }

    /**
     * ポインターダウンイベントを処理する
     * 新しいポインターIDを割り当ててプロキシに追加する
     */
    private fun handlePointerDown(event: MotionEvent, client: LauncherProxyClient, index: Int, view: View) {
        val pointerId = nextPointerId++
        pointerIdMap.put(event.getPointerId(index), pointerId)
        client.addPointer(pointerId, event.getOffset(index, view))
    }
}