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

    fun start() {
        super.start(this)
    }

    private fun addFile(file: File) {
        foundFiles.add(file)
        fileCount.addAndGet(1)
        fileSize.addAndGet(FileUtils.sizeOf(file))
    }

    override fun searchFilesToProcess() {
        mSelectedFiles.forEach {
            currentTask?.let { task -> if (task.isCancelled) return@forEach }
            if (it.isFile) addFile(it)
        }
        currentTask?.let { task -> if (task.isCancelled) return }
        totalFileSize.set(fileSize.get())
    }

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

    override fun getCurrentFileCount() = fileCount.get()

    override fun getTotalSize() = totalFileSize.get()

    override fun getPendingSize() = fileSize.get()

    override fun onEnd() {
        onEndTask.execute()
    }
}