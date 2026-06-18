package com.arata.yukarilauncher.feature

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import com.arata.yukarilauncher.InfoCenter
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.ui.activity.MainActivity
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.NotificationUtils

/** ゲーム実行中にフォアグラウンドサービスとして動作するサービスクラス。 */
class GameService : Service() {

    /** メッセンジャー */
    private val mMessenger = Messenger(IncomingHandler())

    /** サービスの作成時に通知チャンネルを構築する。 */
    override fun onCreate() {
        Tools.buildNotificationChannel(applicationContext)
    }

    /** サービスの起動時にフォアグラウンド通知を表示する。 @param intent Intent @param flags フラグ @param startId 開始ID @return 起動モード */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.getBooleanExtra("kill", false)) {
            stopSelf()
            Process.killProcess(Process.myPid())
            return START_NOT_STICKY
        }
        val killIntent = Intent(applicationContext, GameService::class.java)
        killIntent.putExtra("kill", true)
        val pendingKillIntent = PendingIntent.getService(
            this,
            NotificationUtils.PENDINGINTENT_CODE_KILL_GAME_SERVICE,
            killIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notificationBuilder = NotificationCompat.Builder(this, Tools.NOTIFICATION_CHANNEL_DEFAULT)
            .setContentTitle(InfoCenter.replaceName(this, R.string.lazy_service_default_title))
            .setContentText(getString(R.string.notification_game_runs))
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_terminate), pendingKillIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setSilent(true)

        val notification: Notification = notificationBuilder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            }
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification, serviceType)
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification)
        }
        return START_NOT_STICKY
    }

    /** タスクが削除されたときにサービスを停止する。 @param rootIntent ルートIntent */
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        Process.killProcess(Process.myPid())
    }

    /** サービスをバインドする。 @param intent Intent @return メッセンジャーのBinder */
    @Nullable
    override fun onBind(intent: Intent?): IBinder? = mMessenger.binder

    /** 受信メッセージを処理するハンドラ。 */
    private class IncomingHandler : Handler(Looper.getMainLooper()) {
        /** メッセージを処理する。 @param msg メッセージ */
        override fun handleMessage(@NonNull msg: Message) {}
    }

    companion object {
        /** サービスがアクティブか */
        private var isActive = false

        /** サービスがアクティブかを返す。 @return アクティブか */
        @JvmStatic
        fun isActive(): Boolean = isActive

        /** サービスのアクティブ状態を設定する。 @param active アクティブか */
        @JvmStatic
        fun setActive(active: Boolean) {
            isActive = active
            if (active) {
                GameStateMonitor.notifyGameStarted()
            } else {
                GameStateMonitor.notifyGameStopped()
            }
        }
    }
}