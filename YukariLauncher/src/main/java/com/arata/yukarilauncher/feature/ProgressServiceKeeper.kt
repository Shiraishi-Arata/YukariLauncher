package com.arata.yukarilauncher.feature

import android.content.Context
import com.arata.yukarilauncher.task.TaskCountListener

/** タスク開始時にProgressServiceを起動するためのリスナークラス。 */
class ProgressServiceKeeper(private val context: Context) : TaskCountListener {

    /** タスクカウント更新時にサービスを開始する。 @param taskCount 現在のタスク数 */
    override fun onUpdateTaskCount(taskCount: Int) {
        if (taskCount > 0) ProgressService.startService(context)
    }
}
