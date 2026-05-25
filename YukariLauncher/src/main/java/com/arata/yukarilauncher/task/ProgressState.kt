package com.arata.yukarilauncher.task

/** 進捗状態を保持するデータクラス。 */
class ProgressState {
    /** 進捗値 */
    var progress: Int = 0
    /** リソースID */
    var resid: Int = 0
    /** 追加引数 */
    var varArg: Array<out Any?>? = null
}
