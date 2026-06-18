package com.arata.yukarilauncher.task

/**
 * タスク実行中に例外が発生した場合のリスナー
 */
fun interface OnTaskThrowableListener {
    fun onThrowable(throwable: Throwable)
}