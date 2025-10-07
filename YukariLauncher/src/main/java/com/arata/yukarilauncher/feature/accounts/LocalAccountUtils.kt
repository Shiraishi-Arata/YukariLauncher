package com.arata.yukarilauncher.feature.accounts

import android.annotation.SuppressLint
import android.app.Activity
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.utils.ZHTools
import com.arata.yukarilauncher.utils.path.UrlManager

class LocalAccountUtils {
    companion object {
        @JvmStatic
        fun checkUsageAllowed(listener: CheckResultListener) {
            if (AccountsManager.hasMicrosoftAccount()) {
                listener.onUsageAllowed()
            } else {
                listener.onUsageDenied()
            }
        }

        @JvmStatic
        fun saveReminders(checked: Boolean) {
            AllSettings.localAccountReminders.put(!checked).save()
        }

        @JvmStatic
        @SuppressLint("InflateParams")
        fun openDialog(
            activity: Activity,
            confirmClickListener: TipDialog.OnConfirmClickListener?,
            message: String?,
            confirm: Int
        ) {
            TipDialog.Builder(activity)
                .setTitle(R.string.generic_warning)
                .setMessage(message)
                .setWarning()
                .setShowCheckBox(true)
                .setCheckBox(R.string.generic_no_more_reminders)
                .setConfirmClickListener(confirmClickListener)
                .setConfirm(confirm)
                .setCancelClickListener { ZHTools.openLink(activity, UrlManager.URL_MINECRAFT) }
                .setCancel(R.string.account_purchase_minecraft_account)
                .showDialog()
        }
    }

    interface CheckResultListener {
        fun onUsageAllowed()
        fun onUsageDenied()
    }
}
