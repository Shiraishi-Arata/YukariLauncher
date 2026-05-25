package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.view.MotionEvent
import android.view.View

/** タッチイベントを処理するプロセッサーのインターフェース。ゲーム内/メニュー画面で異なる実装を持ちます。 */
interface TouchEventProcessor {
    /**
     * タッチイベントを処理します。
     * @param motionEvent 処理するタッチイベント
     * @return イベントを消費した場合はtrue
     */
    fun processTouchEvent(motionEvent: MotionEvent): Boolean
    /** 保留中のアクションをキャンセルします。 */
    fun cancelPendingActions()
    /**
     * タッチイベントをディスパッチします。
     * @param event ディスパッチするイベント
     * @param view 対象のビュー
     */
    fun dispatchTouchEvent(event: MotionEvent, view: View) {}
}
