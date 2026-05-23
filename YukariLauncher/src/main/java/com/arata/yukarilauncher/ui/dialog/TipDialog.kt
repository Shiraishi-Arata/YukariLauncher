package com.arata.yukarilauncher.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.annotation.CheckResult
import com.arata.yukarilauncher.databinding.DialogTipBinding
import com.arata.yukarilauncher.ui.dialog.DraggableDialog.DialogInitializationListener

/**
 * ヒント/確認ダイアログ
 */
class TipDialog private constructor(
    context: Context,
    private val title: String?,
    private val message: String?,
    private val confirm: String?,
    private val cancel: String?,
    private val checkBoxText: String?,
    private val showCheckBox: Boolean,
    private val showCancel: Boolean,
    private val showConfirm: Boolean,
    private val centerMessage: Boolean,
    private var selectable: Boolean,
    private val confirmButtonCountdown: Long,
    private val warning: Boolean,
    private val textBeautifier: TextBeautifier?,
    private val cancelListener: OnCancelClickListener?,
    private val confirmListener: OnConfirmClickListener?,
    private val dismissListener: OnDialogDismissListener?
) : FullScreenDialog(context), DialogInitializationListener {
    private val binding = DialogTipBinding.inflate(layoutInflater)

    /**
     * ダイアログ作成時にUIコンポーネントを初期化する
     * @param savedInstanceState 保存されたインスタンス状態
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * TextViewにテキストを設定し、nullの場合は非表示にする
         * @param textString 設定するテキスト
         */
        fun TextView.addText(textString: String?) {
            this.text = textString
            textString ?: run { this.visibility = View.GONE }
        }

        binding.apply {
            setContentView(root)
            DraggableDialog.initDialog(this@TipDialog)

            titleView.addText(title)
            messageView.addText(message)

            messageView.setTextIsSelectable(selectable)

            textBeautifier?.beautify(titleView, messageView)

            checkHeight()

            cancel?.apply { cancelButton.text = this }
            confirm?.apply { confirmButton.text = this }
            if (centerMessage) messageView.gravity = Gravity.CENTER_HORIZONTAL
            if (showCheckBox) {
                checkBox.visibility = View.VISIBLE
                checkBoxText?.let { checkBox.text = it }
            }

            cancelButton.setOnClickListener {
                cancelListener?.onCancelClick()
                this@TipDialog.dismiss()
            }
            confirmButton.setOnClickListener {
                confirmListener?.onConfirmClick(checkBox.isChecked)
                this@TipDialog.dismiss()
            }

            cancelButton.visibility = if (showCancel) View.VISIBLE else View.GONE
            confirmButton.visibility = if (showConfirm) View.VISIBLE else View.GONE

            if (warning) {
                warningIcon.visibility = View.VISIBLE
                warningIcon.drawable.setTint(Color.RED)
            }
        }
    }

    /**
     * ダイアログの高さをチェックし、画面に収まるよう調整する
     */
    private fun checkHeight() {
        checkHeight(binding.root, binding.contentView, binding.scrollView)
    }

    /**
     * ダイアログを表示する
     * 確認ボタンのカウントダウンが設定されている場合はタイマーを開始する
     */
    override fun show() {
        super.show()
        window?.findViewById<View>(android.R.id.content)?.measure(0, 0)

        if (confirmButtonCountdown > 0) {
            binding.confirmButton.apply {
                isEnabled = false

                val buttonText = text
                var remainingTime = confirmButtonCountdown

                val interval = 500L
                val handler = Handler(Looper.getMainLooper())
                val runnable = object : Runnable {
                    @SuppressLint("SetTextI18n")
                    override fun run() {
                        if (remainingTime > 0) {
                            val secondsRemaining = (remainingTime / 1000.0).toInt()
                            text = "$buttonText (${secondsRemaining}s)"
                            remainingTime -= interval
                            handler.postDelayed(this, interval)
                        } else {
                            isEnabled = true
                            text = buttonText
                        }
                    }
                }

                handler.post(runnable)
            }
        }
    }

    /**
     * ダイアログを破棄する
     * 破棄リスナーがfalseを返した場合は破棄をキャンセルする
     */
    override fun dismiss() {
        if (dismissListener?.onDismiss() == false) return
        super.dismiss()
    }

    /**
     * ダイアログ初期化時にWindowを返す
     * @return Windowオブジェクト
     */
    override fun onInit(): Window? = window

    /**
     * テキスト整形のインターフェース
     */
    fun interface TextBeautifier {
        fun beautify(titleText: TextView, messageText: TextView)
    }

    /**
     * キャンセルボタンクリックのコールバック
     */
    fun interface OnCancelClickListener {
        fun onCancelClick()
    }

    /**
     * 確認ボタンクリックのコールバック
     */
    fun interface OnConfirmClickListener {
        fun onConfirmClick(checked: Boolean)
    }

    /**
     * ダイアログ破棄時のコールバック
     */
    fun interface OnDialogDismissListener {
        fun onDismiss(): Boolean
    }

    /**
     * TipDialogのビルダークラス
     */
    open class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var cancel: String? = null
        private var confirm: String? = null
        private var checkBox: String? = null
        private var textBeautifier: TextBeautifier? = null
        private var cancelClickListener: OnCancelClickListener? = null
        private var confirmClickListener: OnConfirmClickListener? = null
        private var dialogDismissListener: OnDialogDismissListener? = null
        private var confirmButtonCountdown: Long = 0L
        private var cancelable = true
        private var showCheckBox = false
        private var showCancel = true
        private var showConfirm = true
        private var centerMessage = true
        private var selectable = false
        private var warning = false

        /** ダイアログを構築する */
        fun buildDialog(): TipDialog {
            if (confirmButtonCountdown > 0 && cancelable)
                throw IllegalArgumentException("Before setting the confirm button countdown, please disable the cancelable option first.")
            return TipDialog(
                this.context,
                title, message, confirm, cancel, checkBox,
                showCheckBox,
                showCancel, showConfirm, centerMessage, selectable, confirmButtonCountdown, warning,
                textBeautifier, cancelClickListener, confirmClickListener, dialogDismissListener
            ).apply {
                setCancelable(cancelable)
                create()
            }
        }

        /** ダイアログを表示する */
        fun showDialog() {
            buildDialog().show()
        }

        /** タイトルを設定する */
        @CheckResult
        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }

        /** タイトルを設定する */
        @CheckResult
        fun setTitle(title: Int): Builder {
            return setTitle(context.getString(title))
        }

        /** メッセージを設定する */
        @CheckResult
        fun setMessage(message: String?): Builder {
            this.message = message
            return this
        }

        /** メッセージを設定する */
        @CheckResult
        fun setMessage(message: Int): Builder {
            return setMessage(context.getString(message))
        }

        /** キャンセルボタンのテキストを設定する */
        @CheckResult
        fun setCancel(cancel: String?): Builder {
            this.cancel = cancel
            return this
        }

        /** キャンセルボタンのテキストを設定する */
        @CheckResult
        fun setCancel(cancel: Int): Builder {
            return setCancel(context.getString(cancel))
        }

        /** チェックボックステキストを設定する */
        @CheckResult
        fun setCheckBox(checkBoxText: String?): Builder {
            this.checkBox = checkBoxText
            return this
        }

        /** チェックボックステキストを設定する */
        @CheckResult
        fun setCheckBox(checkBoxText: Int): Builder {
            return setCheckBox(context.getString(checkBoxText))
        }

        /** 確認ボタンのテキストを設定する */
        @CheckResult
        fun setConfirm(confirm: String?): Builder {
            this.confirm = confirm
            return this
        }

        /** 確認ボタンのテキストを設定する */
        @CheckResult
        fun setConfirm(confirm: Int): Builder {
            return setConfirm(context.getString(confirm))
        }

        /** チェックボックスの表示有無を設定する */
        @CheckResult
        fun setShowCheckBox(show: Boolean): Builder {
            showCheckBox = show
            return this
        }

        /** テキスト整形を設定する */
        @CheckResult
        fun setTextBeautifier(beautifier: TextBeautifier): Builder {
            this.textBeautifier = beautifier
            return this
        }

        /** キャンセルボタンクリックリスナーを設定する */
        @CheckResult
        fun setCancelClickListener(cancelClickListener: OnCancelClickListener?): Builder {
            this.cancelClickListener = cancelClickListener
            return this
        }

        /** 確認ボタンクリックリスナーを設定する */
        @CheckResult
        fun setConfirmClickListener(confirmClickListener: OnConfirmClickListener?): Builder {
            this.confirmClickListener = confirmClickListener
            return this
        }

        /** ダイアログ破棄リスナーを設定する */
        @CheckResult
        fun setDialogDismissListener(dialogDismissListener: OnDialogDismissListener?): Builder {
            this.dialogDismissListener = dialogDismissListener
            return this
        }

        /** 確認ボタンのカウントダウンを設定する */
        @CheckResult
        fun setConfirmButtonCountdown(countdownMillis: Long): Builder {
            if (countdownMillis < 0L) throw IllegalArgumentException("The countdown cannot be negative!")
            this.confirmButtonCountdown = countdownMillis
            return this
        }

        /** キャンセル可否を設定する */
        @CheckResult
        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        /** キャンセルボタンの表示有無を設定する */
        @CheckResult
        fun setShowCancel(showCancel: Boolean): Builder {
            this.showCancel = showCancel
            return this
        }

        /** 確認ボタンの表示有無を設定する */
        @CheckResult
        fun setShowConfirm(showConfirm: Boolean): Builder {
            this.showConfirm = showConfirm
            return this
        }

        /** メッセージを中央寄せにするかどうか */
        @CheckResult
        fun setCenterMessage(center: Boolean): Builder {
            this.centerMessage = center
            return this
        }

        /** メッセージの選択可否を設定する */
        @CheckResult
        fun setSelectable(selectable: Boolean): Builder {
            this.selectable = selectable
            return this
        }

        /** タイトルバーに赤色の警告アイコンを追加する */
        @CheckResult
        fun setWarning(): Builder {
            this.warning = true
            return this
        }
    }
}
