package com.arata.yukarilauncher.task

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * タスク実行に使用するスレッドプールとUIスレッド実行ユーティリティ
 */
class TaskExecutors {
    companion object {
        private val defaultExecutors: ExecutorService = ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        private val uiHandler = Handler(Looper.getMainLooper())

        /**
         * デフォルトのスレッドプールエグゼキューターを取得する
         * @return デフォルトのExecutorService
         */
        @JvmStatic
        fun getDefault(): ExecutorService {
            return defaultExecutors
        }

        /**
         * Android UIスレッド用のエグゼキューターを取得する
         * @return UIスレッドで実行するExecutor
         */
        @JvmStatic
        fun getAndroidUI(): Executor {
            return Executor { r: Runnable -> uiHandler.post(r) }
        }

        /**
         * UIスレッドのハンドラーを取得する
         */
        @JvmStatic
        fun getUIHandler() = uiHandler

        /**
         * UIスレッドでRunnableを実行する
         * @param runnable 実行する処理
         */
        @JvmStatic
        fun runInUIThread(runnable: Runnable) {
            uiHandler.post(runnable)
        }
    }
}
