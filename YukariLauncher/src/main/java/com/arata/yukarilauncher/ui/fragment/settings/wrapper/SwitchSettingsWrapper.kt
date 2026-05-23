package com.arata.yukarilauncher.ui.fragment.settings.wrapper

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.CompoundButton
import com.arata.yukarilauncher.setting.unit.BooleanSettingUnit

/**
 * スイッチ形式の設定ラッパークラスです。
 * トグルスイッチでブール値設定を編集できます。
 */
@SuppressLint("UseSwitchCompatOrMaterialCode")
class SwitchSettingsWrapper(
    private val context: Context,
    private val unit: BooleanSettingUnit,
    val mainView: View,
    val switchView: CompoundButton  // Changed from Switch to CompoundButton
) : AbstractSettingsWrapper(mainView) {
    private var listener: OnCheckedChangeListener? = null

    /**
     * 初期化ブロックです。スイッチの状態とリスナーを設定します。
     */
    init {
        switchView.isChecked = unit.getValue()

        switchView.setOnCheckedChangeListener { buttonView, isChecked ->
            val switchChangeListener = OnSwitchSaveListener {
                unit.put(isChecked).save()
                checkShowRebootDialog(context)
            }
            listener?.onChange(buttonView, isChecked, switchChangeListener) ?: switchChangeListener.onSave()
        }
        mainView.setOnClickListener {
            switchView.isChecked = !switchView.isChecked
        }
    }

    /**
     * チェック状態変更リスナーを設定します。
     * @param listener チェック状態変更リスナー
     * @return 自身のインスタンス
     */
    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener): SwitchSettingsWrapper {
        this.listener = listener
        return this
    }

    /**
     * スイッチ保存通知用の関数型インターフェースです。
     */
    fun interface OnSwitchSaveListener {
        fun onSave()
    }

    /**
     * チェック状態変更通知用の関数型インターフェースです。
     */
    fun interface OnCheckedChangeListener {
        fun onChange(buttonView: CompoundButton, isChecked: Boolean, listener: OnSwitchSaveListener)
    }
}