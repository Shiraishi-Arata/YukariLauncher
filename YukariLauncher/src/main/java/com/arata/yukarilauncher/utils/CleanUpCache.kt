package com.arata.yukarilauncher.utils

import android.content.Context
import android.widget.Toast
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.utils.path.PathManager
import org.apache.commons.io.FileUtils
import java.io.File

class CleanUpCache {
    companion object {
        private var isCleaning = false

        /**
         * キャッシュのクリーンアップを開始する
         * キャッシュディレクトリとバージョンリストファイルを削除し、結果をトーストで表示する
         */
        @JvmStatic
        fun start(context: Context) {
            if (isCleaning) return
            isCleaning = true

            var totalSize: Long = 0
            var fileCount = 0
            try {
                Task.runTask {
                    /* 複数のキャッシュディレクトリと指定ファイルを収集し、一つのリストに結合する */
                    val filesToDelete = mutableListOf<File>()

                    PathManager.DIR_CACHE.listFiles()?.let { filesToDelete.addAll(it) }
                    PathManager.DIR_APP_CACHE.listFiles()?.let { filesToDelete.addAll(it) }
                    File(PathManager.DIR_MOD_LIBRARY).listFiles()?.let { filesToDelete.addAll(it) }

                    val versionListFile = File(PathManager.FILE_VERSION_LIST)
                    if (versionListFile.exists()) filesToDelete.add(versionListFile)

                    for (file in filesToDelete) {
                        ++fileCount
                        totalSize += FileUtils.sizeOf(file)
                        FileUtils.deleteQuietly(file)
                    }
                }.ended(TaskExecutors.getAndroidUI()) {
                    if (fileCount != 0) {
                        Toast.makeText(context,
                            context.getString(R.string.clear_up_cache_clean_up, FileTools.formatFileSize(totalSize)),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context,
                            context.getString(R.string.clear_up_cache_not_found),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.execute()
            } finally {
                isCleaning = false
            }
        }


    }
}
