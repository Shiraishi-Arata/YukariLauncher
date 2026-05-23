package com.arata.yukarilauncher.utils.file

import android.content.Context
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.Task
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * ファイルの削除処理を非同期で実行するハンドラクラス
 * 選択されたファイル・ディレクトリを再帰的に削除し、進捗状況を報告する
 */
class FileDeletionHandler(
    mContext: Context,
    private val mSelectedFiles: List<File>,
    private val endTask: Task<*>?
) : FileHandler(mContext), FileSearchProgress {
    private val foundFiles = mutableListOf<File>()
    private val totalFileSize = AtomicLong(0)
    private val fileSize = AtomicLong(0)
    private val fileCount = AtomicLong(0)

    /**
     * ファイル削除処理を開始する
     */
    fun start() {
        super.start(this)
    }

    /**
     * 単一ファイルを削除リストに追加する
     */
    private fun addFile(file: File) {
        foundFiles.add(file)
        fileCount.addAndGet(1)
        fileSize.addAndGet(FileUtils.sizeOf(file))
    }

    /**
     * ディレクトリ内のファイルを再帰的に削除リストに追加する
     */
    private fun addDirectory(directory: File) {
        if (directory.isFile) addFile(directory)
        else if (directory.isDirectory) {
            directory.listFiles()?.forEach {
                if (it.isFile) addFile(it)
                else if (it.isDirectory) addDirectory(it)
            }
        }
    }

    /**
     * 処理対象のファイル一覧を収集する
     */
    override fun searchFilesToProcess() {
        mSelectedFiles.forEach {
            currentTask?.let { task -> if (task.isCancelled) return@forEach }

            if (it.isFile) addFile(it)
            else if (it.isDirectory) addDirectory(it)
        }
        currentTask?.let { task -> if (task.isCancelled) return }
        totalFileSize.set(fileSize.get())
    }

    /**
     * ファイルの削除を並列実行する
     */
    override fun processFile() {
        Logging.i("FileDeletionHandler", "Delete files (total files: $fileCount)")
        foundFiles.parallelStream().forEach {
            currentTask?.let { task -> if (task.isCancelled) return@forEach }

            fileSize.addAndGet(-FileUtils.sizeOf(it))
            fileCount.getAndDecrement()
            FileUtils.deleteQuietly(it)
        }
        currentTask?.let { task -> if (task.isCancelled) return }
        // 残った空のディレクトリを削除する
        mSelectedFiles.forEach { FileUtils.deleteQuietly(it) }
    }

    override fun getCurrentFileCount() = fileCount.get()

    override fun getTotalSize() = totalFileSize.get()

    override fun getPendingSize() = fileSize.get()

    /**
     * 処理終了時に終了タスクを実行する
     */
    override fun onEnd() {
        endTask?.execute()
    }
}
