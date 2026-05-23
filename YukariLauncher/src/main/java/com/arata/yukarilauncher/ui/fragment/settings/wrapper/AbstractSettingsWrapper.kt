package com.arata.yukarilauncher.ui.fragment.settings.wrapper

import android.content.Context
import android.view.View
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.utils.YLTools

/**
 * 設定ラッパーの抽象基底クラスです。
 * 各設定項目の共通動作（再起動ダイアログ表示、表示制御など）を提供します。
 */
abstract class AbstractSettingsWrapper(
    private val mainView: View
) {
    private var isRequiresReboot = false

    /**
     * この設定に再起動が必要であることをマークします。
     * @return 自身のインスタンス
     */
    fun setRequiresReboot(): AbstractSettingsWrapper {
        isRequiresReboot = true
        return this
    }

    /**
     * 再起動が必要な場合、確認ダイアログを表示します。
     * @param context コンテキスト
     */
    fun checkShowRebootDialog(context: Context) {
        if (isRequiresReboot) {
            TipDialog.Builder(context)
                .setTitle(R.string.generic_tip)
                .setMessage(R.string.setting_reboot_tip)
                .setConfirmClickListener { YLTools.killProcess() }
                .showDialog()
        }
    }

    /**
     * メインビューを非表示にします。
     */
    fun setGone() {
        mainView.visibility = View.GONE
    }
}