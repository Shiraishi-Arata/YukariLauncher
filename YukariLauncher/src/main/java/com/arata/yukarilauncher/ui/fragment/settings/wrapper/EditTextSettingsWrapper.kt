package com.arata.yukarilauncher.ui.fragment.settings.wrapper

import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.arata.yukarilauncher.setting.unit.StringSettingUnit

/**
 * テキスト編集用の設定ラッパークラスです。
 * EditTextの入力内容を設定ユニットに保存します。
 */
class EditTextSettingsWrapper(
    private val unit: StringSettingUnit,
    val mainView: View,
    private val editText: EditText
) : AbstractSettingsWrapper(mainView) {
    private var listener: OnTextChangedListener? = null

    /**
     * 初期化ブロックです。EditTextの設定とテキスト変更リスナーを設定します。
     */
    init {
        editText.apply {
            setText(unit.getValue())
            inputType = InputType.TYPE_CLASS_TEXT
            setOnEditorActionListener { _, _, _ ->
                clearFocus()
                false
            }

            doAfterTextChanged { text ->
                val string = text?.toString() ?: ""
                unit.put(string).save()
                listener?.onChanged(string)
            }
        }
    }

    /**
     * テキスト変更リスナーを設定します。
     * @param listener テキスト変更リスナー
     * @return 自身のインスタンス
     */
    fun setOnTextChangedListener(listener: OnTextChangedListener): EditTextSettingsWrapper {
        this.listener = listener
        return this
    }

    /**
     * 入力可能な最大文字数を設定します。
     * @param maxLength 最大文字数
     * @return 自身のインスタンス
     */
    fun setMaxLength(maxLength: Int): EditTextSettingsWrapper {
        val filters = arrayOf<InputFilter>(LengthFilter(maxLength))
        editText.filters = filters
        return this
    }

    /**
     * 入力欄のテキストの配置（gravity）を設定する。
     * @param gravity 設定するGravity値
     * @return 自身のインスタンス
     */
    fun setInputGravity(gravity: Int): EditTextSettingsWrapper {
        editText.gravity = gravity
        return this
    }

    /**
     * テキスト変更通知用の関数型インターフェースです。
     */
    fun interface OnTextChangedListener {
        fun onChanged(text: String)
    }
}