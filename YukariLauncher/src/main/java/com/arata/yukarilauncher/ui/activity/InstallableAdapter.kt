package com.arata.yukarilauncher.ui.activity

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.databinding.ItemInstallableBinding
import com.arata.yukarilauncher.feature.unpack.OnTaskRunningListener

/**
 * インストール可能アイテム一覧のRecyclerViewアダプター
 */
class InstallableAdapter(
    private val items: List<InstallableItem>,
    private val listener: TaskCompletionListener
) : RecyclerView.Adapter<InstallableAdapter.ViewHolder>() {
    @Volatile
    private var completedTasksCount = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInstallableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * 全てのタスクの完了状態を確認する
     */
    fun checkAllTask() {
        items.forEachIndexed { index, item ->
            if (!item.task.isNeedUnpack()) {
                item.isFinished = true
                updateTaskCount(index)
            }
        }
    }

    /**
     * 全てのタスクを開始する
     */
    fun startAllTasks() {
        items.forEachIndexed { index, item ->
            if (!item.isFinished) {
                Thread {
                    item.task.apply {
                        setTaskRunningListener(object : OnTaskRunningListener {
                            override fun onTaskStart() {
                                item.isRunning = true
                                updateUI { notifyItemChanged(index) }
                            }

                            override fun onTaskEnd() {
                                item.isRunning = false
                                item.isFinished = true
                                updateTaskCount(index)
                            }
                        })
                    }
                    item.task.run()
                }.start()
            }
        }
    }

    /**
     * タスク完了数を更新する
     */
    @Synchronized
    private fun updateTaskCount(index: Int) {
        completedTasksCount++
        updateUI { notifyItemChanged(index) }

        if (completedTasksCount >= itemCount) {
            updateUI { listener.onAllTasksCompleted() }
        }
    }

    /**
     * UIスレッドでアクションを実行する
     */
    private fun updateUI(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post { action() }
        }
    }

    /**
     * インストールアイテムのビューホルダー
     */
    class ViewHolder(
        private val binding: ItemInstallableBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        /**
         * アイテムデータをビューに設定する
         */
        fun setData(item: InstallableItem) {
            binding.name.text = item.name

            if (item.summary.isNullOrEmpty()) {
                binding.summary.visibility = View.GONE
            } else {
                binding.summary.text = item.summary
                binding.summary.visibility = View.VISIBLE
            }

            binding.progress.visibility = if (item.isRunning) View.VISIBLE else View.GONE
            binding.finish.visibility = if (item.isFinished) View.VISIBLE else View.GONE
        }
    }

    /**
     * 全てのタスク完了時のコールバック
     */
    fun interface TaskCompletionListener {
        fun onAllTasksCompleted()
    }
}
