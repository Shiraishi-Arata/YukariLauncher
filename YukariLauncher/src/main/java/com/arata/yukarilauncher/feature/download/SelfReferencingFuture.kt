package com.arata.yukarilauncher.feature.download

import com.arata.yukarilauncher.feature.log.Logging.i
import okhttp3.internal.notify
import okhttp3.internal.wait
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/**
 * 自身のFutureを内部で参照可能にするラッパークラス。
 * タスク実行中に自身のFutureオブジェクトにアクセスする必要がある場合に使用する。
 */
class SelfReferencingFuture(private val mFutureInterface: FutureInterface) {
    private val mFutureLock = Any()
    private var mMyFuture: Future<*>? = null

    /**
     * タスクをExecutorServiceで実行し、そのFutureを返す。
     * タスク開始後に自身のFutureを内部で設定する。
     * @param executorService 実行に使用するExecutorService
     * @return タスクのFuture
     */
    fun startOnExecutor(executorService: ExecutorService): Future<*> {
        val future = executorService.submit { this.run() }
        synchronized(mFutureLock) {
            mMyFuture = future
            mFutureLock.notify()
        }
        return future
    }

    /**
     * 内部実行メソッド。自身のFutureが設定されるまで待機し、
     * その後 FutureInterface の run を呼び出す。
     */
    private fun run() {
        try {
            synchronized(mFutureLock) {
                if (mMyFuture == null) mFutureLock.wait()
            }
            mFutureInterface.run(mMyFuture!!)
        } catch (e: InterruptedException) {
            i("SelfReferencingFuture", "Interrupted while acquiring own Future")
        }
    }

    /**
     * 自身のFutureを受け取って処理を行うコールバックインターフェース。
     */
    interface FutureInterface {
        fun run(myFuture: Future<*>)
    }
}
