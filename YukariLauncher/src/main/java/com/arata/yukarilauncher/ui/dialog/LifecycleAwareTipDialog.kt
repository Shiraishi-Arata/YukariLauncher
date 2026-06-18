package com.arata.yukarilauncher.ui.dialog

import android.annotation.SuppressLint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.arata.yukarilauncher.task.TaskExecutors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ライフサイクル対応のTipダイアログ
 */
abstract class LifecycleAwareTipDialog: LifecycleEventObserver {
    private var mLifecycle: Lifecycle? = null
    private var mDialog: TipDialog? = null
    private var mLifecycleEnded = false

    /**
     * ライフサイクル対応ダイアログを表示する
     * @param lifecycle 追跡するライフサイクル
     */
    @SuppressLint("CheckResult")
    fun show(lifecycle: Lifecycle, builder: TipDialog.Builder) {
        this.mLifecycleEnded = false
        this.mLifecycle = lifecycle.apply {
            if (currentState == Lifecycle.State.DESTROYED) {
                mLifecycleEnded = true
                dialogHidden(true)
                return
            }
            builder.setDialogDismissListener {
                dispatchDialogHidden()
                true
            }
            addObserver(this@LifecycleAwareTipDialog)
        }
        mDialog = builder.buildDialog().apply {
            show()
        }
    }

    /**
     * ダイアログが非表示になった際に呼ばれる
     * @param lifecycleEnded ライフサイクルイベントによる非表示かどうか
     */
    protected abstract fun dialogHidden(lifecycleEnded: Boolean)

    /**
     * ダイアログ非表示イベントをディスパッチする
     */
    private fun dispatchDialogHidden() {
        Exception().printStackTrace()
        dialogHidden(mLifecycleEnded)
        mLifecycle!!.removeObserver(this)
    }

    /**
     * ライフサイクル状態変更時に呼ばれる
     * ON_DESTROYイベントでダイアログを破棄する
     * @param source ライフサイクル所有者
     * @param event ライフサイクルイベント
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            mDialog?.dismiss()
            mLifecycleEnded = true
        }
    }

    companion object {
        /**
         * ダイアログ表示を待機する
         */
        @JvmStatic
        fun haltOnDialog(lifecycle: Lifecycle, builder: TipDialog.Builder): Boolean {
            val waitLock = Object()
            val hasLifecycleEnded = AtomicBoolean(false)

            val showDialogRunnable = Runnable {
                val dialogBuilder: LifecycleAwareTipDialog =
                    object : LifecycleAwareTipDialog() {
                        override fun dialogHidden(lifecycleEnded: Boolean) {
                            hasLifecycleEnded.set(lifecycleEnded)
                            synchronized(waitLock) { waitLock.notifyAll() }
                        }
                    }
                dialogBuilder.show(lifecycle, builder)
            }
            synchronized(waitLock) {
                TaskExecutors.runInUIThread(showDialogRunnable)
                waitLock.wait()
            }
            return hasLifecycleEnded.get()
        }
    }
}