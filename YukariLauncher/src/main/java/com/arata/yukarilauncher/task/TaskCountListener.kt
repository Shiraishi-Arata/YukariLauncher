package com.arata.yukarilauncher.task

/** タスク数の変更を監視するリスナー。 */
fun interface TaskCountListener {
    /** タスク数が変更されたときに呼び出される。 @param taskCount 現在のタスク数 */
    fun onUpdateTaskCount(taskCount: Int)
}
