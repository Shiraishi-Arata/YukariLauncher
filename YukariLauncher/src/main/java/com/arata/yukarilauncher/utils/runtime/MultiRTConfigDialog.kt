package com.arata.yukarilauncher.utils.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R

/** マルチランタイム設定ダイアログを管理するクラス。 */
class MultiRTConfigDialog {
    /** 設定ダイアログのインスタンス。 */
    private var mDialog: AlertDialog? = null
    /** ダイアログ内のRecyclerView。 */
    private var mDialogView: RecyclerView? = null

    /** ダイアログを表示する。 */
    fun show() {
        refresh()
        mDialog?.show()
    }

    /** アダプターのデータを更新して再描画する。 */
    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        val adapter = mDialogView?.adapter
        adapter?.notifyDataSetChanged()
    }

    /**
     * ダイアログを構築する。
     * @param activity コンテキスト
     * @param installJvmLauncher JVMインストール用 ActivityResultLauncher
     */
    fun prepare(activity: Context, installJvmLauncher: ActivityResultLauncher<Any?>) {
        mDialogView = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            adapter = RTRecyclerViewAdapter(MultiRTUtils.runtimes)
        }

        mDialog = AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
            .setTitle(R.string.multirt_config_title)
            .setView(mDialogView)
            .setPositiveButton(R.string.multirt_import) { _, _ -> installJvmLauncher.launch(Any()) }
            .setNeutralButton(R.string.multirt_delete_runtime, null)
            .create()

        mDialog?.setOnShowListener { dialog ->
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
            button.setOnClickListener {
                val adapter = mDialogView?.adapter as RTRecyclerViewAdapter
                val isEditing = !adapter.isEditing
                adapter.isEditing = isEditing
                button.setText(if (isEditing) R.string.multirt_swap_setdefault else R.string.multirt_delete_runtime)
            }
        }
    }
}