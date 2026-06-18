package com.arata.yukarilauncher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.EditText
import androidx.annotation.CheckResult
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.DialogEditTextBinding
import com.arata.yukarilauncher.ui.dialog.DraggableDialog.DialogInitializationListener
import com.arata.yukarilauncher.utils.stringutils.StringUtilsKt.Companion.isEmptyOrBlank

/**
 * テキスト入力ダイアログ
 */
class EditTextDialog private constructor(
    private val context: Context,
    private val title: String?,
    private val message: String?,
    private val editText: String?,
    private val hintText: String?,
    private val checkBox: String?,
    private val confirm: String?,
    private val emptyError: String?,
    private val showCheckBox: Boolean,
    private val inputType: Int,
    private val cancelListener: View.OnClickListener?,
    private val confirmListener: ConfirmListener?,
    private val required: Boolean
) : FullScreenDialog(context),
    DialogInitializationListener {
    private val binding = DialogEditTextBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setCancelable(false)
        this.setContentView(binding.root)

        init()
        DraggableDialog.initDialog(this)
    }

    /**
     * ダイアログを初期化する
     */
    private fun init() {
        binding.apply {
            title?.let { titleView.text = it }
            message?.let {
                messageView.text = it
                messageView.visibility = View.VISIBLE
            }
            editText?.let { textEdit.setText(it) }
            hintText?.let { textEdit.hint = it } ?: run {
                if (required) textEdit.setHint(R.string.generic_required)
            }

            checkHeight()

            confirm?.let { confirmButton.text = it }
            if (showCheckBox) {
                checkBox.visibility = View.VISIBLE
                checkBox.text = this@EditTextDialog.checkBox
            }
            if (inputType != -1) textEdit.inputType = inputType

            confirmListener?.let {
                confirmButton.setOnClickListener { _ ->
                    if (required) {
                        val text = textEdit.text.toString()
                        if (isEmptyOrBlank(text)) {
                            textEdit.error = emptyError ?: context.getString(R.string.generic_error_field_empty)
                            return@setOnClickListener
                        }
                    }
                    val dismissDialog = it.onConfirm(textEdit, checkBox.isChecked)
                    if (dismissDialog) dismiss()
                }
            }

            val cancelListener = cancelListener ?: View.OnClickListener { dismiss() }
            cancelButton.setOnClickListener(cancelListener)
        }
    }

    /**
     * ダイアログの高さをチェックし、画面に収まるよう調整する
     */
    private fun checkHeight() {
        checkHeight(binding.root, binding.contentView, binding.scrollView)
    }

    /**
     * ダイアログ初期化時にWindowを返す
     * @return Windowオブジェクト
     */
    override fun onInit(): Window? = window

    /**
     * 確認ボタンクリックのコールバックインターフェース
     */
    fun interface ConfirmListener {
        fun onConfirm(editText: EditText, checked: Boolean): Boolean
    }

    /**
     * テキスト入力ダイアログのビルダークラス
     */
    class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var editText: String? = null
        private var hintText: String? = null
        private var checkBox: String? = null
        private var confirm: String? = null
        private var emptyError: String? = null
        private var showCheckBox = false
        private var inputType = -1
        private var cancelListener: View.OnClickListener? = null
        private var confirmListener: ConfirmListener? = null
        private var required = false

        /** タイトルテキストを設定する */
        @CheckResult
        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        /** タイトルテキストを設定する */
        @CheckResult
        fun setTitle(title: Int): Builder {
            return setTitle(context.getString(title))
        }

        /** メッセージテキストを設定する */
        @CheckResult
        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        /** メッセージテキストを設定する */
        @CheckResult
        fun setMessage(message: Int): Builder {
            return setMessage(context.getString(message))
        }

        /** 入力フィールドのテキストを設定する */
        @CheckResult
        fun setEditText(editText: String): Builder {
            this.editText = editText
            return this
        }

        /** 入力フィールドのヒントを設定する */
        @CheckResult
        fun setHintText(hintText: Int): Builder {
            return setHintText(context.getString(hintText))
        }

        /** 入力フィールドのヒントを設定する */
        @CheckResult
        fun setHintText(hintText: String): Builder {
            this.hintText = hintText
            return this
        }

        /** 確認ボタンのテキストを設定する */
        @CheckResult
        fun setConfirmText(text: Int): Builder {
            return setConfirmText(context.getString(text))
        }

        /** 確認ボタンのテキストを設定する */
        @CheckResult
        fun setConfirmText(text: String): Builder {
            this.confirm = text
            return this
        }

        /** 必須入力エラーテキストを設定する */
        @CheckResult
        fun setEmptyErrorText(text: Int): Builder {
            return setEmptyErrorText(context.getString(text))
        }

        /** 必須入力エラーテキストを設定する */
        @CheckResult
        fun setEmptyErrorText(text: String): Builder {
            this.emptyError = text
            return this
        }

        /** チェックボックスの表示有無を設定する */
        @CheckResult
        fun setShowCheckBox(show: Boolean): Builder {
            this.showCheckBox = show
            return this
        }

        /** チェックボックスのテキストを設定する */
        @CheckResult
        fun setCheckBoxText(text: Int): Builder {
            return setCheckBoxText(context.getString(text))
        }

        /** チェックボックスのテキストを設定する */
        @CheckResult
        fun setCheckBoxText(text: String): Builder {
            this.checkBox = text
            return this
        }

        /** 入力フィールドの入力タイプを設定する */
        @CheckResult
        fun setInputType(inputType: Int): Builder {
            this.inputType = inputType
            return this
        }

        /** キャンセルボタンのクリックリスナーを設定する */
        @CheckResult
        fun setCancelListener(cancel: View.OnClickListener): Builder {
            this.cancelListener = cancel
            return this
        }

        /** 確認ボタンのクリックリスナーを設定する */
        @CheckResult
        fun setConfirmListener(confirmListener: ConfirmListener): Builder {
            this.confirmListener = confirmListener
            return this
        }

        /** 必須入力に設定する */
        @CheckResult
        fun setAsRequired(): Builder {
            this.required = true
            return this
        }

        /** ダイアログを構築する */
        fun buildDialog(): EditTextDialog {
            return EditTextDialog(
                context,
                title, message, editText, hintText, checkBox, confirm, emptyError,
                showCheckBox, inputType,
                cancelListener, confirmListener,
                required
            ).apply {
                create()
            }
        }

        /** ダイアログを表示する */
        fun showDialog() {
            buildDialog().show()
        }
    }
}