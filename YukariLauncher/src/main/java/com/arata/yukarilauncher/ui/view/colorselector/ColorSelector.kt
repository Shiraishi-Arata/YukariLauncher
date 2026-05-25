package com.arata.yukarilauncher.ui.view.colorselector

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.listener.SimpleTextWatcher

/** 色選択ダイアログのロジックを管理するクラス。色相、彩度・明度、アルファ、HEX入力に対応する。 */
class ColorSelector(context: Context, parent: ViewGroup, colorSelectionListener: ColorSelectionListener?) :
    HueSelectionListener, RectangleSelectionListener, AlphaSelectionListener, SimpleTextWatcher {

    /** ルートビュー。 */
    private val mRootView: View
    /** 色相選択ビュー。 */
    private val mHueView: HueView
    /** 彩度・明度選択ビュー。 */
    private val mLuminosityIntensityView: SVRectangleView
    /** アルファ選択ビュー。 */
    private val mAlphaView: AlphaView
    /** プレビュー表示ビュー。 */
    private val mColorView: ColorSideBySideView
    /** HEX入力用テキストフィールド。 */
    private val mTextView: EditText
    /** 色選択リスナー。 */
    private var mColorSelectionListener: ColorSelectionListener?
    /** 色相テンプレート（彩度・明度は1固定）。 */
    private val mHueTemplate = floatArrayOf(0f, 1f, 1f)
    /** 現在選択中のHSV値。 */
    private val mHsvSelected = floatArrayOf(360f, 1f, 1f)
    /** 現在選択中のアルファ値。 */
    private var mAlphaSelected = 0xff
    /** テキストビューの通常時の文字色。 */
    private val mTextColors: ColorStateList
    /** HEX入力の監視フラグ（循環更新防止）。 */
    private var mWatch = true
    /** アルファ選択が有効かどうか。 */
    private var mAlphaEnabled = true

    init {
        mRootView = LayoutInflater.from(context).inflate(R.layout.dialog_color_selector, parent, false)
        mHueView = mRootView.findViewById(R.id.color_selector_hue_view)
        mLuminosityIntensityView = mRootView.findViewById(R.id.color_selector_rectangle_view)
        mAlphaView = mRootView.findViewById(R.id.color_selector_alpha_view)
        mColorView = mRootView.findViewById(R.id.color_selector_color_view)
        mTextView = mRootView.findViewById(R.id.color_selector_hex_edit)
        runColor(Color.RED)
        mHueView.setHueSelectionListener(this)
        mLuminosityIntensityView.setRectSelectionListener(this)
        mAlphaView.setAlphaSelectionListener(this)
        mTextView.addTextChangedListener(this)
        mTextColors = mTextView.textColors
        mColorSelectionListener = colorSelectionListener
        parent.addView(mRootView)
    }

    /** ルートビューを取得する。 @return ルートビュー */
    fun getRootView(): View = mRootView

    /** デフォルト色（赤）でセレクターを表示する。 */
    fun show() = show(Color.RED)

    /** 指定された色でセレクターを表示する。 @param previousColor 初期表示色 */
    fun show(previousColor: Int) {
        runColor(previousColor)
        dispatchColorChange()
    }

    /** 色相が選択されたときの処理。 @param hue 選択された色相値 */
    override fun onHueSelected(hue: Float) {
        mHsvSelected[0] = hue
        mHueTemplate[0] = hue
        mLuminosityIntensityView.setColor(Color.HSVToColor(mHueTemplate), true)
        dispatchColorChange()
    }

    /** 彩度・明度が変更されたときの処理。 @param luminosity 明度 @param intensity 彩度 */
    override fun onLuminosityIntensityChanged(luminosity: Float, intensity: Float) {
        mHsvSelected[1] = intensity
        mHsvSelected[2] = luminosity
        dispatchColorChange()
    }

    /** アルファ値が選択されたときの処理。 @param alpha アルファ値 */
    override fun onAlphaSelected(alpha: Int) {
        mAlphaSelected = alpha
        dispatchColorChange()
    }

    /** 色変更イベントをリスナーと全子ビューに通知する。 */
    protected fun dispatchColorChange() {
        val color = Color.HSVToColor(mAlphaSelected, mHsvSelected)
        mColorView.setColor(color)
        mWatch = false
        mTextView.setText(String.format("%08X", color))
        notifyColorSelector(color)
    }

    /** 指定された色を各選択ビューに反映する。 @param color 反映する色 */
    protected fun runColor(color: Int) {
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), mHsvSelected)
        mHueTemplate[0] = mHsvSelected[0]
        mHueView.setHue(mHsvSelected[0])
        mLuminosityIntensityView.setColor(Color.HSVToColor(mHueTemplate), false)
        mLuminosityIntensityView.setLuminosityIntensity(mHsvSelected[2], mHsvSelected[1])
        mAlphaSelected = Color.alpha(color)
        mAlphaView.setAlpha(if (mAlphaEnabled) mAlphaSelected else 255)
        mColorView.setColor(color)
    }

    /** HEXテキストが編集されたときの処理。入力をパースして色選択に反映する。 @param s 編集後のテキスト */
    override fun afterTextChanged(s: Editable) {
        if (mWatch) {
            try {
                val color = Integer.parseInt(s.toString(), 16)
                mTextView.setTextColor(mTextColors)
                runColor(color)
            } catch (exception: NumberFormatException) {
                mTextView.setTextColor(Color.RED)
            }
        } else {
            mWatch = true
        }
    }

    /** 色選択リスナーを設定する。 */
    fun setColorSelectionListener(listener: ColorSelectionListener?) {
        mColorSelectionListener = listener
    }

    /** アルファ選択の有効/無効を設定する。 @param alphaEnabled アルファ選択を有効にするか */
    fun setAlphaEnabled(alphaEnabled: Boolean) {
        mAlphaEnabled = alphaEnabled
        mAlphaView.visibility = if (alphaEnabled) View.VISIBLE else View.GONE
        mAlphaView.setAlpha(255)
    }

    /** 選択された色をリスナーに通知する。 @param color 選択された色 */
    private fun notifyColorSelector(color: Int) {
        mColorSelectionListener?.onColorSelected(color)
    }

    companion object {
        /** アルファ成分をマスクするための定数。 */
        private const val ALPHA_MASK = (0xFF shl 24).inv()

        /** 指定された色にアルファ値を設定した新しい色を返す。 @param color 元の色 @param alpha 設定するアルファ値 @return アルファ適用後の色 */
        fun setAlpha(color: Int, alpha: Int): Int {
            return color and ALPHA_MASK or (alpha and 0xFF shl 24)
        }
    }
}
