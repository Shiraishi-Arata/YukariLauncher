package com.arata.yukarilauncher.event.value

/**
 * ダウンロードページに関するイベント
 */
class DownloadPageEvent {
    /**
     * ダウンロードページ切り替え時にFragmentにアニメーションを通知するイベント
     * @param index Fragmentのカテゴリインデックス
     * @param classify アニメーションの種類（IN：開始アニメーション、OUT：終了アニメーション）
     */
    class PageSwapEvent(val index: Int, val classify: Int) {
        companion object {
            const val IN = 0
            const val OUT = 1
        }
    }

    /**
     * ダウンロードページが破棄されたことを通知するイベント
     */
    class PageDestroyEvent

    /**
     * RecyclerViewの有効/無効を設定するイベント
     */
    class RecyclerEnableEvent(val enable: Boolean)
}
