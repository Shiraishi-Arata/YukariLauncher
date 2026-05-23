package com.arata.yukarilauncher.ui.subassembly.settingsbutton

import android.widget.ImageButton
import androidx.core.content.ContextCompat
import com.arata.yukarilauncher.R

/**
 * 設定/ホームボタンのラッパークラス
 */
class SettingsButtonWrapper(val button: ImageButton) {
    private var onTypeChangeListener: OnTypeChangeListener? = null
    private var buttonType: ButtonType? = ButtonType.SETTINGS

    /**
     * ボタン種別を設定する
     */
    fun setButtonType(type: ButtonType) {
        if (buttonType != type) {
            buttonType = type
            button.setImageDrawable(
                ContextCompat.getDrawable(button.context,
                    if (type == ButtonType.SETTINGS) R.drawable.ic_menu_settings else R.drawable.ic_menu_home
                )
            )
            onTypeChangeListener?.onChange(type)
        }
    }

    /**
     * 種別変更リスナーを設定する
     */
    fun setOnTypeChangeListener(listener: OnTypeChangeListener) { onTypeChangeListener = listener }
}

/**
 * ボタンの種別
 */
enum class ButtonType {
    SETTINGS, HOME
}
