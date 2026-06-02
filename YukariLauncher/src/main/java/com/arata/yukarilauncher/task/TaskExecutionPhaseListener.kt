package com.arata.yukarilauncher.task

/**
 * タスク実行の各フェーズを監視するリスナー
 */
interface TaskExecutionPhaseListener {
    /**
     * タスク開始前に実行される処理
     */
    fun onBeforeStart() {}

    /**
     * メインのタスク処理
     */
    fun execute() {}

    /**
     * タスク終了時に実行される処理
     */
    fun onEnded() {}

    /**
     * タスクの最終処理（成功・失敗に関わらず実行）
     */
    fun onFinally() {}

    /**
     * タスク実行中に例外が発生した場合の処理
     * @param throwable 発生した例外
     */
    fun onThrowable(throwable: Throwable) {}
}