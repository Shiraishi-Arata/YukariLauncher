package com.arata.yukarilauncher.utils.runtime

import android.content.Context
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.SelectRuntimeDialog

class SelectRuntimeUtils {
    companion object {
        /**
         * ランタイム選択処理を実行する
         * 設定に応じて、ダイアログ表示・デフォルト使用・自動選択のいずれかの動作を行う
         * @param context コンテキスト
         * @param dialogTitle ダイアログのタイトル（省略可）
         * @param selectedListener 選択結果を受け取るリスナー
         */
        @JvmStatic
        fun selectRuntime(context: Context, dialogTitle: String?, selectedListener: RuntimeSelectedListener) {
            TaskExecutors.runInUIThread {
                when (AllSettings.selectRuntimeMode.getValue()) {
                    "ask_me" -> SelectRuntimeDialog(context, selectedListener).apply {
                        dialogTitle?.let { setTitleText(it) }
                    }.show()
                    "default" -> selectedListener.onSelected(AllSettings.defaultRuntime.getValue().takeIf { it.isNotEmpty() })
                    "auto" -> selectedListener.onSelected(null)
                    else -> {}
                }
            }
        }
    }
}