package com.arata.yukarilauncher.ui.fragment.settings.wrapper

import android.annotation.SuppressLint
import android.content.Context
import android.view.View

/**
 * 基本的な設定ラッパークラスです。
 * クリック時に指定されたリスナーを呼び出します。
 */
@SuppressLint("UseSwitchCompatOrMaterialCode")
class BaseSettingsWrapper(
    val context: Context,
    val mainView: View,
    listener: OnViewClickListener
) : AbstractSettingsWrapper(mainView) {

    /**
     * 初期化ブロックです。メインビューのクリックリスナーを設定します。
     */
    init {
        mainView.setOnClickListener {
            listener.onClick()
        }
    }
}