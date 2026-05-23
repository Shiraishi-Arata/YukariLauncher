package com.arata.yukarilauncher.task

import androidx.annotation.CheckResult
import java.util.concurrent.Callable
import java.util.concurrent.Executor

/**
 * 非同期タスクの抽象基底クラス
 * タスクの実行前後処理、エラーハンドリング、結果通知などをチェーン可能な形で提供する
 * @param V タスクの結果型
 */
abstract class Task<V>: TaskExecutionPhaseListener {
    private var executor: Executor = TaskExecutors.getDefault()
    private var throwableFromTask: Throwable? = null
    private var beforeStart: Pair<Runnable, Executor>? = null
    private var ended: Pair<OnTaskEndedListener<V>, Executor>? = null
    private var finally: Pair<Runnable, Executor>? = null
    private var onTaskThrowable: Pair<OnTaskThrowableListener, Executor>? = null
    private var result: V? = null

    /**
     * メインタスクの処理を実装する
     */
    protected abstract fun performMainTask()

    /**
     * タスクの実行エグゼキューターを設定する
     * @param executor 使用するエグゼキューター
     * @return 自身のインスタンス
     */
    @CheckResult(SUGGEST)
    open fun setExecutor(executor: Executor): Task<V> {
        this.executor = executor
        return this
    }

    /**
     * タスクで発生した例外を保存する
     */
    private fun setThrowable(e: Throwable) {
        this.throwableFromTask = e
    }

    /**
     * 保存された例外を確認し、あればリスナーに通知する
     */
    private fun checkThrowable() {
        throwableFromTask?.let {
            onThrowable(it)
        }
    }

    /**
     * タスク開始前の処理を設定する（メインタスクと同じエグゼキューターで実行）
     */
    @CheckResult(SUGGEST)
    fun beforeStart(runnable: Runnable): Task<V> {
        return beforeStart(this.executor, runnable)
    }

    /**
     * タスク開始前の処理を設定する（指定したエグゼキューターで実行）
     */
    @CheckResult(SUGGEST)
    fun beforeStart(executor: Executor, runnable: Runnable): Task<V> {
        this.beforeStart = Pair(runnable, executor)
        return this
    }

    /**
     * タスク終了時のリスナーを設定する（メインタスクと同じエグゼキューターで実行）
     */
    @CheckResult(SUGGEST)
    fun ended(listener: OnTaskEndedListener<V>): Task<V> {
        return ended(this.executor, listener)
    }

    /**
     * タスク終了時のリスナーを設定する（指定したエグゼキューターで実行）
     */
    @CheckResult(SUGGEST)
    fun ended(executor: Executor, listener: OnTaskEndedListener<V>): Task<V> {
        this.ended = Pair(listener, executor)
        return this
    }

    /**
     * タスク最終処理を設定する（メインタスクと同じエグゼキューターで実行）
     */
    @CheckResult(SUGGEST)
    fun finallyTask(runnable: Runnable): Task<V> {
        return finallyTask(this.executor, runnable)
    }

    /**
     * タスク最終処理を設定する（指定したエグゼキューターで実行）
     */
    @CheckResult(SUGGEST)
    fun finallyTask(executor: Executor, runnable: Runnable): Task<V> {
        this.finally = Pair(runnable, executor)
        return this
    }

    /**
     * 例外発生時のリスナーを設定する（メインタスクと同じエグゼキューターで実行）
     */
    @CheckResult(SUGGEST)
    fun onThrowable(listener: OnTaskThrowableListener): Task<V> {
        return onThrowable(this.executor, listener)
    }

    /**
     * 例外発生時のリスナーを設定する（指定したエグゼキューターで実行）
     */
    @CheckResult(SUGGEST)
    fun onThrowable(executor: Executor, listener: OnTaskThrowableListener): Task<V> {
        this.onTaskThrowable = Pair(listener, executor)
        return this
    }

    /**
     * タスク結果を設定する
     */
    fun setResult(result: V) {
        this.result = result
    }

    /**
     * タスク開始前の処理を実行する
     */
    override fun onBeforeStart() {
        this.beforeStart?.let { r ->
            r.second.execute {
                runCatching { r.first.run() }.getOrElse { t -> setThrowable(t) }
            }
        }
    }

    /**
     * タスクを実行する
     * 開始前処理、メイン処理、終了処理、例外処理を順に実行する
     */
    override fun execute() {
        onBeforeStart()
        checkThrowable()

        this.executor.execute run@{
            runCatching {
                performMainTask()
                onEnded()
            }.getOrElse { t -> setThrowable(t) }
            checkThrowable()
            onFinally()
        }
    }

    /**
     * タスク終了時の処理を実行する
     */
    override fun onEnded() {
        this.ended?.let { r ->
            r.second.execute {
                runCatching { r.first.onEnded(result) }.getOrElse { t -> onThrowable(t) }
            }
        }
    }

    /**
     * タスク最終処理を実行する
     */
    override fun onFinally() {
        this.finally?.let { r ->
            r.second.execute {
                runCatching { r.first.run() }
            }
        }
    }

    /**
     * 例外発生時の処理を実行する
     */
    override fun onThrowable(throwable: Throwable) {
        this.onTaskThrowable?.let { r ->
            r.second.execute {
                runCatching { r.first.onThrowable(throwable) }
            }
        }
    }

    companion object {
        const val SUGGEST = "NOT_REQUIRED_TO_EXECUTE"

        /**
         * Callableからタスクを作成して実行する
         * @param callable 実行する処理
         * @return 作成されたタスク
         */
        @CheckResult(SUGGEST)
        @JvmStatic
        fun <V> runTask(callable: Callable<V>): Task<V> {
            return SimpleTask(callable)
        }

        /**
         * 指定したエグゼキューターでCallableを実行するタスクを作成する
         * @param executor 使用するエグゼキューター
         * @param callable 実行する処理
         * @return 作成されたタスク
         */
        @CheckResult(SUGGEST)
        @JvmStatic
        fun <V> runTask(executor: Executor, callable: Callable<V>): Task<V> {
            return SimpleTask(callable).setExecutor(executor)
        }
    }
}
