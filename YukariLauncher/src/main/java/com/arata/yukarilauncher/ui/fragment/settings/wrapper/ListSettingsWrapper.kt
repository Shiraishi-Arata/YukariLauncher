package com.arata.yukarilauncher.ui.fragment.settings.wrapper

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.setting.unit.StringSettingUnit

/**
 * リスト選択式の設定ラッパークラスです。
 * ダイアログで選択肢を表示し、選択値を設定ユニットに保存します。
 */
@SuppressLint("UseSwitchCompatOrMaterialCode")
class ListSettingsWrapper(
    val context: Context,
    val unit: StringSettingUnit,
    val mainView: View,
    val titleView: TextView,
    val valueView: TextView,
    val entries: Array<String>,
    val entryValues: Array<String>,
    val onValueSelected: ((selectedValue: String) -> Boolean)? = null
) : AbstractSettingsWrapper(mainView) {

    /**
     * リソースIDから選択肢を指定するコンストラクタです。
     */
    constructor(
        context: Context,
        unit: StringSettingUnit,
        mainView: View,
        titleView: TextView,
        valueView: TextView,
        itemsId: Int,
        itemValuesId: Int
    ) : this(
        context, unit, mainView, titleView, valueView,
        context.resources.getStringArray(itemsId),
        context.resources.getStringArray(itemValuesId),
        null
    )

    /**
     * リソースIDから選択肢を指定し、選択時のコールバックを設定するコンストラクタです。
     */
    constructor(
        context: Context,
        unit: StringSettingUnit,
        mainView: View,
        titleView: TextView,
        valueView: TextView,
        itemsId: Int,
        itemValuesId: Int,
        onValueSelected: ((selectedValue: String) -> Boolean)?
    ) : this(
        context, unit, mainView, titleView, valueView,
        context.resources.getStringArray(itemsId),
        context.resources.getStringArray(itemValuesId),
        onValueSelected
    )

    /**
     * 初期化ブロックです。リスト値の表示を更新し、クリックリスナーを設定します。
     */
    init {
        updateListViewValue()
        mainView.setOnClickListener { createAListDialog() }
    }

    /**
     * 選択肢ダイアログを作成して表示します。
     */
    private fun createAListDialog() {
        val index = entryValues.indexOf(unit.getValue())
        AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
            .setTitle(titleView.text)
            .setSingleChoiceItems(entries, index) { dialog, which ->
                if (which != index) {
                    val selectedValue = entryValues[which]
                    val canSave = onValueSelected?.invoke(selectedValue) ?: true
                    if (!canSave) {
                        dialog.dismiss()
                        return@setSingleChoiceItems
                    }
                    unit.put(selectedValue).save()
                    updateListViewValue()
                    checkShowRebootDialog(context)
                }
                dialog.dismiss()
            }
            .setPositiveButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * 現在の設定値に基づいてリストビューの表示を更新します。
     */
    private fun updateListViewValue() {
        val index = entryValues.indexOf(unit.getValue()).takeIf { it in entryValues.indices } ?: run {
            unit.reset()
            0
        }
        valueView.text = entries[index]
    }
}
