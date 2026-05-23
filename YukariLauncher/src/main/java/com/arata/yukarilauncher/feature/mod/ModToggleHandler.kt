package com.arata.yukarilauncher.feature.mod

import android.content.Context
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.utils.file.FileHandler
import com.arata.yukarilauncher.utils.file.FileSearchProgress
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class ModToggleHandler(
    mContext: Context,
    private val mSelectedFiles: List<File>,
    private val onEndTask: Task<*>
) : FileHandler(mContext), FileSearchProgress {
    private val foundFiles = mutableListOf<File>()
    private val totalFileSize = AtomicLong(0)
    private val fileSize = AtomicLong(0)
    private val fileCount = AtomicLong(0)

/**
 * startする
 */
    fun start() {
        super.start(this)
    }

/**
 * addFileする
 */
    private fun addFile(file: File) {
        foundFiles.add(file)
        fileCount.addAndGet(1)
        fileSize.addAndGet(FileUtils.sizeOf(file))
    }

/**
 * searchFilesToProcessする
 */
    override fun searchFilesToProcess() {
        mSelectedFiles.forEach {
            currentTask?.let { task -> if (task.isCancelled) return@forEach }
            if (it.isFile) addFile(it)
        }
        currentTask?.let { task -> if (task.isCancelled) return }
        totalFileSize.set(fileSize.get())
    }

/**
 * processFileする
 */
    override fun processFile() {
        (foundFiles).forEach {
            currentTask?.let { task -> if (task.isCancelled) return@forEach }

            fileSize.addAndGet(-FileUtils.sizeOf(it))
            fileCount.getAndDecrement()

            val fileName = it.name
            if (fileName.endsWith(ModUtils.JAR_FILE_SUFFIX)) {
                ModUtils.disableMod(it)
            } else if (fileName.endsWith(ModUtils.DISABLE_JAR_FILE_SUFFIX)) {
                ModUtils.enableMod(it)
            }
        }
    }

/**
 * getCurrentFileCountする
 */
    override fun getCurrentFileCount() = fileCount.get()

/**
 * getTotalSizeする
 */
    override fun getTotalSize() = totalFileSize.get()

/**
 * getPendingSizeする
 */
    override fun getPendingSize() = fileSize.get()

/**
 * onEndする
 */
    override fun onEnd() {
        onEndTask.execute()
    }
}