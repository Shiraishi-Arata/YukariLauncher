package com.arata.yukarilauncher.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools

/** 通知送信ユーティリティを提供するオブジェクト。 */
object NotificationUtils {

    /** 進捗サービス通知ID。 */
    const val NOTIFICATION_ID_PROGRESS_SERVICE = 1
    /** ゲームサービス通知ID。 */
    const val NOTIFICATION_ID_GAME_SERVICE = 2
    /** ダウンロードリスナー通知ID。 */
    const val NOTIFICATION_ID_DOWNLOAD_LISTENER = 3
    /** エラー表示通知ID。 */
    const val NOTIFICATION_ID_SHOW_ERROR = 4
    /** ゲーム開始通知ID。 */
    const val NOTIFICATION_ID_GAME_START = 5
    /** 進捗サービス停止用PendingIntentコード。 */
    const val PENDINGINTENT_CODE_KILL_PROGRESS_SERVICE = 1
    /** ゲームサービス停止用PendingIntentコード。 */
    const val PENDINGINTENT_CODE_KILL_GAME_SERVICE = 2
    /** ダウンロードサービス用PendingIntentコード。 */
    const val PENDINGINTENT_CODE_DOWNLOAD_SERVICE = 3
    /** エラー表示用PendingIntentコード。 */
    const val PENDINGINTENT_CODE_SHOW_ERROR = 4
    /** ゲーム開始用PendingIntentコード。 */
    const val PENDINGINTENT_CODE_GAME_START = 5

    /**
     * 基本的な通知を送信する。
     * @param context コンテキスト
     * @param contentTitle 通知タイトルのリソースID
     * @param contentText 通知本文のリソースID
     * @param actionIntent アクションインテント
     * @param pendingIntentCode PendingIntentコード
     * @param notificationId 通知ID
     */
    fun sendBasicNotification(context: Context, contentTitle: Int, contentText: Int, actionIntent: Intent?,
                              pendingIntentCode: Int, notificationId: Int) {

        val pendingIntent = PendingIntent.getActivity(context, pendingIntentCode, actionIntent,
                PendingIntent.FLAG_IMMUTABLE)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(context, Tools.NOTIFICATION_CHANNEL_DEFAULT)
        if (contentTitle != -1) notificationBuilder.setContentTitle(context.getString(contentTitle))
        if (contentText != -1) notificationBuilder.setContentText(context.getString(contentText))
        if (actionIntent != null) notificationBuilder.setContentIntent(pendingIntent)
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
