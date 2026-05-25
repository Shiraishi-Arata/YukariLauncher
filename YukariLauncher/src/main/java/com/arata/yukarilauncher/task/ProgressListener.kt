package com.arata.yukarilauncher.task

/** 進捗状態の変更を監視するリスナー。 */
interface ProgressListener {
    /** 進捗が開始されたときに呼び出される。 */
    fun onProgressStarted()
    /** 進捗が更新されたときに呼び出される。 @param progress 進捗値 @param resid リソースID @param va 追加引数 */
    fun onProgressUpdated(progress: Int, resid: Int, vararg va: Any?)
    /** 進捗が終了したときに呼び出される。 */
    fun onProgressEnded()
}
