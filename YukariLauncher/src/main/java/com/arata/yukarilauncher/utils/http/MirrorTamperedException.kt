package com.arata.yukarilauncher.utils.http

import android.app.Activity
import android.content.Context
import android.text.Html
import androidx.appcompat.app.AlertDialog
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.context.ContextExecutorTask
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.ui.activity.ShowErrorActivity

/** ミラーファイルが改ざんされたことを示す例外。コンテキスト実行タスクとしてダイアログ表示を行う。 */
class MirrorTamperedException : Exception(), ContextExecutorTask {
    /**
     * アクティビティ上で改ざん警告ダイアログを表示する。
     * @param activity 表示元のアクティビティ
     */
    override fun executeWithActivity(activity: Activity) {
        val builder = AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
        builder.setTitle(R.string.dl_tampered_manifest_title)
        builder.setMessage(Html.fromHtml(activity.getString(R.string.dl_tampered_manifest)))
        addButtons(builder)
        ShowErrorActivity.installRemoteDialogHandling(activity, builder)
        builder.show()
    }

    /**
     * ダイアログに操作ボタンを追加する。
     * @param builder アラートダイアログビルダー
     */
    private fun addButtons(builder: AlertDialog.Builder) {
        builder.setPositiveButton(R.string.dl_switch_to_official_site) { _, _ -> AllSettings.downloadSource.reset() }
        builder.setNegativeButton(R.string.dl_turn_off_manifest_checks) { _, _ -> AllSettings.verifyManifest.put(false).save() }
        builder.setNeutralButton(android.R.string.cancel) { _, _ -> }
    }

    /**
     * アプリケーションコンテキストでの実行（何もしない）。
     * @param context アプリケーションコンテキスト
     */
    override fun executeWithApplication(context: Context) {}
}
