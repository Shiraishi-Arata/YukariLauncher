package com.arata.yukarilauncher.task

import com.arata.yukarilauncher.task.TaskCountListener
import java.util.*

/** 進捗状態を管理するシングルトンオブジェクト。 */
object ProgressKeeper {

    /** 進捗リスナーマップ */
    private val sProgressListeners = HashMap<String, MutableList<ProgressListener>>()
    /** 進捗状態マップ */
    private val sProgressStates = HashMap<String, ProgressState>()
    /** タスクカウントリスナーリスト */
    private val sTaskCountListeners = ArrayList<TaskCountListener>()

    /** 進捗を登録し、リスナーに通知する。 @param progressRecord 進捗レコードキー @param progress 進捗値 @param resid リソースID @param va 追加引数 */
    @JvmStatic
    @Synchronized
    fun submitProgress(progressRecord: String, progress: Int, resid: Int, vararg va: Any?) {
        val progressState = sProgressStates[progressRecord]
        val shouldCallStarted = progressState == null
        val shouldCallEnded = resid == -1 && progress == -1
        if (shouldCallEnded) {
            sProgressStates.remove(progressRecord)
            updateTaskCount()
        } else if (shouldCallStarted) {
            val newState = ProgressState()
            sProgressStates[progressRecord] = newState
            updateTaskCount()
        }
        val currentState = sProgressStates[progressRecord]
        if (currentState != null) {
            currentState.progress = progress
            currentState.resid = resid
            currentState.varArg = va
        }

        val progressListeners = sProgressListeners[progressRecord]
        progressListeners?.forEach { listener ->
            when {
                shouldCallStarted -> listener.onProgressStarted()
                shouldCallEnded -> listener.onProgressEnded()
                else -> listener.onProgressUpdated(progress, resid, *va)
            }
        }
    }

    private fun notifyProgressUpdated(listener: ProgressListener, state: ProgressState) {
        listener.onProgressStarted()
        listener.onProgressUpdated(state.progress, state.resid, *(state.varArg ?: emptyArray<Any>()))
    }

    /** タスクカウントを更新し、リスナーに通知する。 */
    @Synchronized
    private fun updateTaskCount() {
        val count = sProgressStates.size
        for (listener in sTaskCountListeners) {
            listener.onUpdateTaskCount(count)
        }
    }

    /** 進捗リスナーを追加する。 @param progressRecord 進捗レコードキー @param listener リスナー */
    @JvmStatic
    @Synchronized
    fun addListener(progressRecord: String, listener: ProgressListener) {
        val state = sProgressStates[progressRecord]
        if (state != null && (state.resid != -1 || state.progress != -1)) {
            notifyProgressUpdated(listener, state)
        } else {
            listener.onProgressEnded()
        }
        val listenerList = sProgressListeners.computeIfAbsent(progressRecord) { ArrayList() }
        listenerList.add(listener)
    }

    /** 進捗リスナーを削除する。 @param progressRecord 進捗レコードキー @param listener リスナー */
    @JvmStatic
    @Synchronized
    fun removeListener(progressRecord: String, listener: ProgressListener) {
        sProgressListeners[progressRecord]?.remove(listener)
    }

    /** タスクカウントリスナーを追加する。 @param listener リスナー */
    @JvmStatic
    @Synchronized
    fun addTaskCountListener(listener: TaskCountListener) {
        listener.onUpdateTaskCount(sProgressStates.size)
        if (!sTaskCountListeners.contains(listener)) sTaskCountListeners.add(listener)
    }

    /** タスクカウントリスナーを追加する。 @param listener リスナー @param runUpdate 追加時に即時更新を実行するか */
    @JvmStatic
    @Synchronized
    fun addTaskCountListener(listener: TaskCountListener, runUpdate: Boolean) {
        if (runUpdate) listener.onUpdateTaskCount(sProgressStates.size)
        if (!sTaskCountListeners.contains(listener)) sTaskCountListeners.add(listener)
    }

    /** タスクカウントリスナーを削除する。 @param listener リスナー */
    @JvmStatic
    @Synchronized
    fun removeTaskCountListener(listener: TaskCountListener) {
        sTaskCountListeners.remove(listener)
    }

    /** 指定した進捗キーの進捗が存在するかを返す。 @param progressKey 進捗キー @return 存在するか */
    @JvmStatic
    fun containsProgress(progressKey: String): Boolean = sProgressStates.containsKey(progressKey)

    /** 全てのタスクが完了するまで待機してからRunnableを実行する。 @param runnable 実行する処理 */
    @JvmStatic
    fun waitUntilDone(runnable: Runnable) {
        if (taskCount == 0) {
            runnable.run()
            return
        }
        val listener = object : TaskCountListener {
            override fun onUpdateTaskCount(taskCount: Int) {
                if (taskCount == 0) {
                    runnable.run()
                    removeTaskCountListener(this)
                }
            }
        }
        addTaskCountListener(listener)
    }

    /** 現在のタスク数を返す。 */
    @JvmStatic
    @get:Synchronized
    val taskCount: Int
        get() = sProgressStates.size

    /** 進行中のタスクが存在するかを返す。 @return 進行中タスクが存在するか */
    @JvmStatic
    fun hasOngoingTasks(): Boolean = taskCount > 0

    /** 進捗レコード。 */
    class ProgressRecord(
        @JvmField val key: String,
        @JvmField val progress: Int,
        val resid: Int,
        val varArg: Array<Any?>
    )

    /** 全ての進捗レコードを取得する。 @return 進捗レコードのリスト */
    @JvmStatic
    @Synchronized
    fun getProgressRecords(): List<ProgressRecord> {
        if (sProgressStates.isEmpty()) return emptyList()
        val records = ArrayList<ProgressRecord>()
        for ((key, state) in sProgressStates) {
            records.add(
                ProgressRecord(
                    key,
                    state.progress,
                    state.resid,
                    state.varArg?.let { arg -> Array<Any?>(arg.size) { arg[it] } } ?: emptyArray<Any?>()
                )
            )
        }
        return records
    }
}
