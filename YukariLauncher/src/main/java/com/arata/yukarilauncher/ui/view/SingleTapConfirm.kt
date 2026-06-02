package com.arata.yukarilauncher.ui.view

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent

/**
 * シングルタップを検出するジェスチャーリスナークラス。
 * 単一のタップ操作のみを確認し、長押しやダブルタップなど他のジェスチャーは無視します。
 * マウス操作の簡易クリック検出などに使用されます。
 */
class SingleTapConfirm : SimpleOnGestureListener() {
    /**
     * シングルタップアップイベントが発生したときに呼び出されます。
     * @param event タップイベントの情報
     * @return 常にtrue（タップを確認したことを示す）
     */
    override fun onSingleTapUp(event: MotionEvent): Boolean = true
}