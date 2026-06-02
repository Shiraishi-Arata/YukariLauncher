package com.arata.yukarilauncher.feature

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arata.yukarilauncher.InfoCenter
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.task.ProgressKeeper
import com.arata.yukarilauncher.task.TaskCountListener
import com.arata.yukarilauncher.utils.NotificationUtils

/** タスク進捗をフォアグラウンド通知で表示するサービスクラス。 */
class ProgressService : Service(), TaskCountListener {

    /** 通知管理コンパチ */
    private lateinit var notificationManagerCompat: NotificationManagerCompat
    /** 通知ビルダー */
    private var mNotificationBuilder: NotificationCompat.Builder? = null

    /** サービスの作成時に通知チャンネルと通知ビルダーを初期化する。 */
    override fun onCreate() {
        Tools.buildNotificationChannel(applicationContext)
        notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        val killIntent = Intent(applicationContext, ProgressService::class.java)
        killIntent.putExtra("kill", true)
        val pendingKillIntent = PendingIntent.getService(
            this,
            NotificationUtils.PENDINGINTENT_CODE_KILL_PROGRESS_SERVICE,
            killIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        mNotificationBuilder = NotificationCompat.Builder(this, Tools.NOTIFICATION_CHANNEL_DEFAULT)
            .setContentTitle(InfoCenter.replaceName(this, R.string.lazy_service_default_title))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_terminate), pendingKillIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setSilent(true)
    }

    /** サービスの起動時にフォアグラウンドサービスを開始する。 @param intent Intent @param flags フラグ @param startId 開始ID @return 起動モード */
    @SuppressLint("StringFormatInvalid")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.getBooleanExtra("kill", false)) {
                stopSelf()
                Process.killProcess(Process.myPid())
                return START_NOT_STICKY
            }
        }
        Logging.d("ProgressService", "Started!")
        mNotificationBuilder?.setContentText(getString(R.string.progresslayout_tasks_in_progress, ProgressKeeper.taskCount))
        val notification: Notification = mNotificationBuilder!!.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE, notification)
        }
        if (ProgressKeeper.taskCount < 1) stopSelf()
        else ProgressKeeper.addTaskCountListener(this, false)

        return START_NOT_STICKY
    }

    /** サービスをバインドする。 @param intent Intent @return null（バインド不可） */
    @Nullable
    override fun onBind(intent: Intent?): IBinder? = null

    /** サービス破棄時にタスクカウントリスナーを削除する。 */
    override fun onDestroy() {
        ProgressKeeper.removeTaskCountListener(this)
    }

    /** タスクカウント更新時に通知を更新する。 @param taskCount 現在のタスク数 */
    override fun onUpdateTaskCount(taskCount: Int) {
        if (YLTools.checkForNotificationPermission()) {
            TaskExecutors.runInUIThread {
                if (taskCount > 0) {
                    mNotificationBuilder?.setContentText(getString(R.string.progresslayout_tasks_in_progress, taskCount))
                    notificationManagerCompat.notify(1, mNotificationBuilder!!.build())
                } else {
                    stopSelf()
                }
            }
        }
    }

    companion object {
        /** プログレスサービスを開始する。 @param context コンテキスト */
        fun startService(context: Context) {
            val intent = Intent(context, ProgressService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}