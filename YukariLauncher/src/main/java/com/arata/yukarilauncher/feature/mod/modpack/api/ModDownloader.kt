package com.arata.yukarilauncher.feature.mod.modpack.api

import androidx.annotation.Nullable
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.http.DownloadUtils
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** マルチスレッドでModファイルをダウンロードするクラス。 @param destinationDirectory ダウンロード先ディレクトリ @param mUseFileCount ファイル数で進捗を計測する場合はtrue（サイズ計測の場合はfalse） */
class ModDownloader(destinationDirectory: File, private val mUseFileCount: Boolean = false) {
    /** スレッドローカルなバッファ。ファイルダウンロードの読み込みに使用される。 */
    private val sThreadLocalBuffer = ThreadLocal<ByteArray>()
    /** ダウンロード用スレッドプール。 */
    private val mDownloadPool: ThreadPoolExecutor
    /** 中断フラグ。 */
    private val mTerminator = AtomicBoolean(false)
    /** ダウンロード完了ファイル数のカウンタ。 */
    private val mDownloadProgress = AtomicInteger(0)
    /** ダウンロード済みバイト数のカウンタ。 */
    private val mDownloadedSize = AtomicLong(0)
    /** 例外同期用のオブジェクト。 */
    private val mExceptionSyncPoint = Any()
    /** ダウンロード先ディレクトリ。 */
    private val mDestinationDirectory = destinationDirectory
    /** 最初に発生したIOException。 */
    private var mFirstIOException: IOException? = null
    /** ダウンロード予定の合計サイズ。 */
    private var mTotalSize: Long = 0

    /** スレッドプールを初期化する。設定から最大スレッド数を取得する。 */
    init {
        val maxThreads = AllSettings.maxDownloadThreads.getValue()
        mDownloadPool = ThreadPoolExecutor(
                maxOf(1, maxThreads / 2),
                maxThreads,
                100,
                TimeUnit.MILLISECONDS,
                LinkedBlockingQueue()
        )
        mDownloadPool.rejectedExecutionHandler = ThreadPoolExecutor.DiscardPolicy()
    }

    /** ダウンロードタスクをサブミットする。 @param fileSize ファイルサイズ @param relativePath 相対パス @param downloadHash SHA1ハッシュ（検証用、null可） @param url ダウンロードURL（複数指定可能） */
    fun submitDownload(fileSize: Int, relativePath: String, @Nullable downloadHash: String?, vararg url: String) {
        if (mUseFileCount) mTotalSize += 1
        else mTotalSize += fileSize
        mDownloadPool.execute(DownloadTask(arrayOf(*url), File(mDestinationDirectory, relativePath), downloadHash))
    }

    /** FileInfoProvider経由でダウンロードタスクをサブミットする。 @param infoProvider ファイル情報プロバイダ */
    fun submitDownload(infoProvider: FileInfoProvider) {
        if (!mUseFileCount) throw RuntimeException("This method can only be used in a file-counting ModDownloader")
        mTotalSize += 1
        mDownloadPool.execute(FileInfoQueryTask(infoProvider))
    }

    /** 全ダウンロードの完了を待機する（Tools.DownloaderFeedback版）。 @param feedback 進捗コールバック */
    fun awaitFinish(feedback: Tools.DownloaderFeedback) {
        awaitFinish(object : OnFileDownloadedListener {
            override fun downloaded() {
                feedback.updateProgress(mDownloadedSize.get(), mTotalSize)
            }
        })
    }

    /** 全ダウンロードの完了を待機する（DownloadProgressListener版）。 @param listener 進捗リスナー */
    fun awaitFinish(listener: DownloadProgressListener) {
        awaitFinish(object : OnFileDownloadedListener {
            override fun downloaded() {
                listener.feedback(mDownloadProgress.get(), mTotalSize.toInt(), mDownloadedSize.get())
            }
        })
    }

    /** 全ダウンロードの完了を待機する内部メソッド。 @param listener ファイルダウンロード完了時のコールバック */
    private fun awaitFinish(listener: OnFileDownloadedListener) {
        try {
            mDownloadPool.shutdown()
            val startTime = System.currentTimeMillis()
            val timeoutMs = 300_000L // 5 minutes max
            while (!mDownloadPool.awaitTermination(20, TimeUnit.MILLISECONDS) && !mTerminator.get()) {
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    mDownloadPool.shutdownNow()
                    throw IOException("Mod download timed out after 5 minutes")
                }
                listener.downloaded()
            }
            if (mTerminator.get()) {
                mDownloadPool.shutdownNow()
                synchronized(mExceptionSyncPoint) {
                    if (mFirstIOException == null) (mExceptionSyncPoint as Object).wait()
                    throw mFirstIOException!!
                }
            }
        } catch (e: InterruptedException) {
            Logging.e("ModDownloader", Tools.printToString(e))
        }
    }

    /** スレッドローカルバッファを取得する。 @return 8KBのバッファ */
    private fun getThreadLocalBuffer(): ByteArray {
        val buffer = sThreadLocalBuffer.get()
        if (buffer != null) return buffer
        val newBuffer = ByteArray(8192)
        sThreadLocalBuffer.set(newBuffer)
        return newBuffer
    }

    /** ダウンロード失敗時の処理。例外を記録し、全タスクを中断する。 @param exception 発生したIOException */
    private fun downloadFailed(exception: IOException) {
        mTerminator.set(true)
        synchronized(mExceptionSyncPoint) {
            if (mFirstIOException == null) {
                mFirstIOException = exception
                (mExceptionSyncPoint as Object).notify()
            }
        }
    }

    /** 外部APIからファイル情報を取得してからダウンロードする内部タスク。 @param mFileInfoProvider ファイル情報プロバイダ */
    inner class FileInfoQueryTask(private val mFileInfoProvider: FileInfoProvider) : Runnable {
        override fun run() {
            try {
                val fileInfo = mFileInfoProvider.getFileInfo() ?: return
                DownloadTask(arrayOf(fileInfo.url),
                        File(mDestinationDirectory, fileInfo.relativePath), fileInfo.sha1).run()
            } catch (e: IOException) {
                downloadFailed(e)
            }
        }
    }

    /** 単一ファイルのダウンロードを実行する内部タスク。 @param mDownloadUrls ダウンロード元URL配列 @param mDestination 保存先ファイル @param mSha1 検証用SHA1ハッシュ */
    inner class DownloadTask(
            private val mDownloadUrls: Array<String>,
            private val mDestination: File,
            private val mSha1: String?
    ) : Runnable, Tools.DownloaderFeedback {
        /** 前回の進捗報告時のダウンロード済みサイズ。 */
        private var last = 0L

        override fun run() {
            for (sourceUrl in mDownloadUrls) {
                try {
                    DownloadUtils.ensureSha1(mDestination, mSha1, Callable<Void> {
                        val exception = tryDownload(sourceUrl)
                        if (exception != null) {
                            throw exception
                        }
                        null
                    })
                } catch (e: IOException) {
                    downloadFailed(e)
                }
            }
        }

        /** 実際のダウンロードを実行する（最大5回リトライ）。 @param sourceUrl ダウンロード元URL @return 失敗時のIOException、成功時はnull */
        private fun tryDownload(sourceUrl: String): IOException? {
            var exception: IOException? = null
            for (i in 0 until 5) {
                try {
                    DownloadUtils.downloadFileMonitored(sourceUrl, mDestination, getThreadLocalBuffer(), this)
                    if (mUseFileCount) mDownloadProgress.addAndGet(1)
                    return null
                } catch (e: InterruptedIOException) {
                    throw e
                } catch (e: IOException) {
                    Logging.e("ModDownloader", Tools.printToString(e))
                    exception = e
                }
                mDownloadedSize.addAndGet(-last)
                last = 0
            }
            return exception
        }

        /** ダウンロード進捗の更新を受け取る。 @param curr 現在の進捗 @param max 最大値 */
        override fun updateProgress(curr: Long, max: Long) {
            val size = curr - last
            mDownloadedSize.addAndGet(size)
            last = curr
        }
    }

    /** ダウンロードするファイルの情報。 @param url ダウンロードURL @param relativePath 保存先相対パス @param sha1 SHA1ハッシュ */
    class FileInfo(val url: String, val relativePath: String, @Nullable val sha1: String?)

    /** ファイル情報を提供するインターフェース。 */
    interface FileInfoProvider {
        /** ファイル情報を取得する。 @return ファイル情報、取得不可の場合はnull */
        fun getFileInfo(): FileInfo?
    }

    /** ファイルダウンロード完了のコールバックインターフェース。 */
    private interface OnFileDownloadedListener {
        /** ダウンロード完了時に呼び出される。 */
        fun downloaded()
    }

    /** ダウンロード進捗リスナー。 @param downloadedCount 完了ファイル数 @param totalCount 合計ファイル数 @param downloadedSize ダウンロード済みサイズ */
    interface DownloadProgressListener {
        fun feedback(downloadedCount: Int, totalCount: Int, downloadedSize: Long)
    }
}