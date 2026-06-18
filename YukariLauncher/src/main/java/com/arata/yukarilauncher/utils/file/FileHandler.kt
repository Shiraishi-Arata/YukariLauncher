package com.arata.yukarilauncher.utils.file

import android.content.Context
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.ProgressDialog
import com.arata.yukarilauncher.utils.YLTools
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Future

/**
 * ファイル操作の抽象基底クラス
 * 進捗ダイアログを表示しながら、ファイル操作（コピー・削除など）を非同期で実行する
 */
abstract class FileHandler(
    protected val context: Context,
) {
    protected var currentTask: Future<*>? = null
    private var timer: Timer? = null
    private var lastSize: Long = 0
    private var lastTime: Long = YLTools.getCurrentTimeMillis()

    /**
     * ファイル操作を開始する
     * 進捗ダイアログを表示し、定期的に進捗を更新しながら非同期でファイル処理を実行する
     */
    protected fun start(progress: FileSearchProgress) {
        TaskExecutors.runInUIThread {
            val dialog = ProgressDialog(context) {
                cancelTask()
                onEnd()
                true
            }
            dialog.updateText(context.getString(R.string.file_operation_file, "0 B", "0 B", 0))

            currentTask = TaskExecutors.getDefault().submit {
                TaskExecutors.runInUIThread { dialog.show() }

                timer = Timer()
                timer?.schedule(object : TimerTask() {
                    override fun run() {
                        val pendingSize = progress.getPendingSize()
                        val totalSize = progress.getTotalSize()
                        val processedSize = totalSize - pendingSize

                        val currentTime = YLTools.getCurrentTimeMillis()
                        val timeElapsed = (currentTime - lastTime) / 1000.0
                        val sizeChange = processedSize - lastSize
                        val rate = (if (timeElapsed > 0) sizeChange / timeElapsed else 0.0).toLong()

                        lastSize = processedSize
                        lastTime = currentTime

                        TaskExecutors.runInUIThread {
                            dialog.updateText(
                                context.getString(
                                    R.string.file_operation_file,
                                    FileTools.formatFileSize(pendingSize),
                                    FileTools.formatFileSize(totalSize),
                                    progress.getCurrentFileCount()
                                )
                            )
                            dialog.updateRate(rate)
                            dialog.updateProgress(
                                processedSize.toDouble(),
                                totalSize.toDouble()
                            )
                        }
                    }
                }, 0, 100)

                searchFilesToProcess()
                currentTask?.let { task -> if (task.isCancelled) return@submit }
                processFile()

                TaskExecutors.runInUIThread { dialog.dismiss() }
                timer?.cancel()
                onEnd()
            }
        }
    }

    /**
     * 処理するファイル一覧を収集する
     */
    abstract fun searchFilesToProcess()

    /**
     * ファイル処理を実行する
     */
    abstract fun processFile()

    /**
     * 処理終了時のコールバック
     */
    abstract fun onEnd()

    /**
     * 現在のタスクをキャンセルする
     */
    private fun cancelTask() {
        currentTask?.let {
            if (!currentTask!!.isDone) {
                currentTask?.cancel(true)
                timer?.let { timer?.cancel() }
            }
        }
    }
}