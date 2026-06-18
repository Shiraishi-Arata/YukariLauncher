package com.arata.yukarilauncher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.EditText
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.DialogEditControlInfoBinding
import com.arata.yukarilauncher.ui.dialog.DraggableDialog.DialogInitializationListener
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlInfoData

/**
 * コントロール情報編集ダイアログ
 */
class EditControlInfoDialog(
    context: Context,
    private val editFileName: Boolean,
    private val mFileName: String?,
    private val controlInfoData: ControlInfoData
) :
    FullScreenDialog(context), DialogInitializationListener {
    private val binding = DialogEditControlInfoBinding.inflate(layoutInflater)
    private var title: String? = null
    private var mOnConfirmClickListener: OnConfirmClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setContentView(binding.root)

        binding.apply {
            fileNameEdit.isEnabled = editFileName
            fileNameEdit.setHint(R.string.generic_required)
            nameEdit.setHint(R.string.generic_optional)
            versionEdit.setHint(R.string.generic_optional)
            authorEdit.setHint(R.string.generic_optional)
            descEdit.setHint(R.string.generic_optional)

            cancelButton.setOnClickListener { dismiss() }
            confirmButton.setOnClickListener { confirmClick() }

            if (!mFileName.isNullOrEmpty() && mFileName != "null") fileNameEdit.setText(mFileName)
            setValueIfNotNull(controlInfoData.name, nameEdit)
            setValueIfNotNull(controlInfoData.version, versionEdit)
            setValueIfNotNull(controlInfoData.author, authorEdit)
            setValueIfNotNull(controlInfoData.desc, descEdit)

            checkHeight(root, contentView, scrollView)
        }
        DraggableDialog.initDialog(this)
    }

    /**
     * 確認ボタンがクリックされた際の処理
     */
    private fun confirmClick() {
        val fileNameText = binding.fileNameEdit.text.toString()

        if (fileNameText.isEmpty()) {
            binding.fileNameEdit.error = context.getString(R.string.generic_error_field_empty)
            return
        }

        updateControlInfoData()
        mOnConfirmClickListener?.onClick(
            fileNameText,
            controlInfoData
        )
    }

    /**
     * 編集内容をControlInfoDataに反映する
     */
    private fun updateControlInfoData() {
        controlInfoData.name = getValueOrDefault(binding.nameEdit)
        controlInfoData.version = getValueOrDefault(binding.versionEdit)
        controlInfoData.author = getValueOrDefault(binding.authorEdit)
        controlInfoData.desc = getValueOrDefault(binding.descEdit)
    }

    /**
     * EditTextの値を取得し、空文字の場合はデフォルト値"null"を返す
     */
    private fun getValueOrDefault(editText: EditText): String {
        val value = editText.text.toString()
        return value.ifEmpty { "null" }
    }

    /**
     * 値が有効な場合にEditTextに設定する
     */
    private fun setValueIfNotNull(value: String?, editText: EditText) {
        if (!value.isNullOrEmpty() && value != "null") editText.setText(value)
    }

    /** 確認ボタンクリックリスナーを設定する */
    fun setOnConfirmClickListener(listener: OnConfirmClickListener) {
        this.mOnConfirmClickListener = listener
    }

    /** ファイル名入力ボックスを取得する */
    val fileNameEditBox: EditText
        get() = binding.fileNameEdit

    /** タイトルを設定する */
    fun setTitle(title: String?) {
        this.title = title
    }

    override fun show() {
        title?.takeIf { it.isNotEmpty() }?.let { binding.title.text = it }
        super.show()
    }

    override fun onInit(): Window? {
        return window
    }

    /**
     * 確認ボタンクリックのコールバックインターフェース
     */
    fun interface OnConfirmClickListener {
        fun onClick(fileName: String, controlInfoData: ControlInfoData)
    }
}