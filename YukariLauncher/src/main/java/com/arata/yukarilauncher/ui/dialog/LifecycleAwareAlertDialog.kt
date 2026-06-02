package com.arata.yukarilauncher.ui.dialog

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle

object LifecycleAwareAlertDialog {
    fun interface DialogCreator {
        fun create(dialog: AlertDialog, builder: AlertDialog.Builder)
    }

    fun haltOnDialog(lifecycle: Lifecycle, activity: android.app.Activity, creator: DialogCreator): Boolean = false
}