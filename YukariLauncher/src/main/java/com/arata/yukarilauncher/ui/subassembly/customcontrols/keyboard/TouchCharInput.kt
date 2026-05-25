package com.arata.yukarilauncher.ui.subassembly.customcontrols.keyboard

import android.content.Context
import android.text.Editable
import android.text.Selection
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.arata.yukarilauncher.R

/**
 * タッチスクリーン用の文字入力ビュー。
 * AndroidのEditTextをベースに、Minecraftへの文字送信機能を提供します。
 * キーボード表示/非表示の切り替えや、入力文字のリアルタイム送信を行います。
 */
class TouchCharInput : androidx.appcompat.widget.AppCompatEditText {
    companion object {
        /** テキストバッファのフィラー文字列。 */
        const val TEXT_FILLER = "                              "
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.editTextStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup()
    }

    private var mIsDoingInternalChanges = false
    private var mCharacterSender: CharacterSenderStrategy? = null

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        disable()
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            disable()
        }
        return super.onKeyPreIme(keyCode, event)
    }

    /** キーボードの表示状態を切り替えます。 */
    fun switchKeyboardState() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (hasFocus()) {
            clear()
            disable()
        } else {
            enable()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /** テキストバッファをクリアします。 */
    fun clear() {
        mIsDoingInternalChanges = true
        val editable = editableText
        editable.clear()
        editable.append(TEXT_FILLER)
        Selection.setSelection(editable, TEXT_FILLER.length)
        mIsDoingInternalChanges = false
    }

    /** 入力を有効にします。 */
    fun enable() {
        isEnabled = true
        isFocusable = true
        visibility = View.VISIBLE
        requestFocus()
    }

    /** 入力を無効にします。 */
    fun disable() {
        clear()
        visibility = View.GONE
        clearFocus()
        isEnabled = false
    }

    /** エンターキーを送信します。 */
    private fun sendEnter() {
        mCharacterSender?.sendEnter()
        clear()
    }

    /** @param characterSender 文字送信戦略を設定 */
    fun setCharacterSender(characterSender: CharacterSenderStrategy?) {
        mCharacterSender = characterSender
    }

    /** 初期設定を行います。 */
    private fun setup() {
        addTextChangedListener(InputTextWatcher())
        setOnEditorActionListener { _, _, _ ->
            sendEnter()
            clear()
            disable()
            false
        }
        clear()
        disable()
    }

    /** 入力テキストの変更を監視し、Minecraftに文字を送信する内部リスナークラス。 */
    private inner class InputTextWatcher : android.text.TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
            if (mIsDoingInternalChanges) return
            mCharacterSender?.let { sender ->
                for (i in 0 until lengthBefore) {
                    sender.sendBackspace()
                }
                var count = 0
                var i = start
                while (count < lengthAfter) {
                    sender.sendChar(text[i])
                    count++
                    i++
                }
            }
        }

        override fun afterTextChanged(editable: Editable) {
            if (mIsDoingInternalChanges) return
            if (editable.length < 1) clear()
        }
    }
}
