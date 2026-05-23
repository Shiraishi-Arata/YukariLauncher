package com.arata.yukarilauncher.ui.fragment

import androidx.fragment.app.Fragment
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener

/**
 * すべてのフラグメントの基底クラス
 */
abstract class BaseFragment : Fragment, TaskCountListener {
    private var mIsTaskRunning: Boolean = false

    constructor() : super()

    constructor(contentLayoutId: Int) : super(contentLayoutId)

    /**
     * 戻るボタン処理
     */
    open fun onBackPressed(): Boolean = true

    /**
     * タスク実行中かどうかを返す
     */
    fun isTaskRunning() = mIsTaskRunning

    /**
     * 強制的に前の画面に戻る
     */
    fun forceBack() {
        requireActivity().supportFragmentManager.popBackStackImmediate()
    }

    override fun onStart() {
        super.onStart()
        ProgressKeeper.addTaskCountListener(this)
    }

    override fun onStop() {
        super.onStop()
        ProgressKeeper.removeTaskCountListener(this)
    }

    override fun onUpdateTaskCount(taskCount: Int) {
        this.mIsTaskRunning = taskCount != 0
    }
}
