package com.arata.yukarilauncher.setting.unit

import com.arata.yukarilauncher.setting.Settings.Manager

class IntSettingUnit(key: String, defaultValue: Int) : AbstractSettingUnit<Int>(key, defaultValue) {
    /**
     * 整数の設定値を取得する
     */
    override fun getValue() = Manager.getValue(key, defaultValue) { it.toIntOrNull() }
}