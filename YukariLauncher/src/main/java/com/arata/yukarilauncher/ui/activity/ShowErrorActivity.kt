/**
 * エラーを表示するためのアクティビティ。
 * リモートエラーハンドリングもサポートします。
 */
package com.arata.yukarilauncher.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.context.ContextExecutorTask
import com.arata.yukarilauncher.utils.NotificationUtils
import java.io.Serializable

/**
 * エラーを表示するためのアクティビティ。
 * リモートエラーハンドリングもサポートします。
 */
class ShowErrorActivity : Activity() {

    companion object {
        private const val ERROR_ACTIVITY_REMOTE_TASK = "remoteTask"

        /**
         * リモートダイアログ処理をダイアログにインストールします。
         * ShowErrorActivity経由で表示されるダイアログは、閉じられたらアクティビティも終了します。
         * @param callerActivity ContextExecutorTask.executeWithActivityから提供されるアクティビティ
         * @param builder アラートダイアログビルダー
         */
        @JvmStatic
        fun installRemoteDialogHandling(callerActivity: Activity, @NonNull builder: AlertDialog.Builder) {
            if (callerActivity is ShowErrorActivity) {
                builder.setOnDismissListener { callerActivity.finish() }
            }
        }
    }

    /** インテントからエラータスクを取得し、アクティビティとともに実行します */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent ?: run { finish(); return }
        val remoteErrorTask = intent.getSerializableExtra(ERROR_ACTIVITY_REMOTE_TASK) as? RemoteErrorTask
            ?: run { finish(); return }
        remoteErrorTask.executeWithActivity(this)
    }

    /**
     * コンテキストに応じてエラーを表示するリモートエラータスク。シリアライズ可能。
     */
    class RemoteErrorTask(
        private val mThrowable: Throwable,
        private val mRolledMsg: String
    ) : ContextExecutorTask, Serializable {

        /** アクティビティが利用可能な場合、エラーダイアログを表示します */
        override fun executeWithActivity(activity: Activity) {
            if (mThrowable is ContextExecutorTask) {
                (mThrowable as ContextExecutorTask).executeWithActivity(activity)
            } else {
                Tools.showError(activity, mRolledMsg, mThrowable, activity is ShowErrorActivity)
            }
        }

        /** アクティビティが利用できない場合、通知でエラーを表示します */
        override fun executeWithApplication(context: Context) {
            val showErrorIntent = Intent(context, ShowErrorActivity::class.java)
            showErrorIntent.putExtra(ERROR_ACTIVITY_REMOTE_TASK, this)
            NotificationUtils.sendBasicNotification(
                context,
                R.string.notif_error_occured,
                R.string.notif_error_occured_desc,
                showErrorIntent,
                NotificationUtils.PENDINGINTENT_CODE_SHOW_ERROR,
                NotificationUtils.NOTIFICATION_ID_SHOW_ERROR
            )
        }
    }
}
