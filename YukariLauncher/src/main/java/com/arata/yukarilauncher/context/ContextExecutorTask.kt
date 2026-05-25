package com.arata.yukarilauncher.context

import android.app.Activity
import android.content.Context

/** コンテキスト依存タスクインターフェース。ActivityまたはApplicationコンテキストで処理を実行する。 */
interface ContextExecutorTask {
    /** Activityでタスクを実行する。 @param activity アクティビティ */
    fun executeWithActivity(activity: Activity)
    /** アプリケーションコンテキストでタスクを実行する。 @param context コンテキスト */
    fun executeWithApplication(context: Context)
}
