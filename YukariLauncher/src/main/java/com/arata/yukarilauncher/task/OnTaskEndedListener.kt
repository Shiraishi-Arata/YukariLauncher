package com.arata.yukarilauncher.task

/**
 * タスク終了時の結果を受け取るリスナー
 * @param V 結果の型
 */
fun interface OnTaskEndedListener<V> {
    @Throws(Throwable::class)
    fun onEnded(result: V?)
}