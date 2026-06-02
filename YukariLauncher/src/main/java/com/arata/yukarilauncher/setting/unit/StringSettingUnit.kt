package com.arata.yukarilauncher.setting.unit

import com.arata.yukarilauncher.setting.Settings.Manager

class StringSettingUnit(key: String, defaultValue: String) : AbstractSettingUnit<String>(key, defaultValue) {
    /**
     * 文字列の設定値を取得する
     */
    override fun getValue() = Manager.getValue(key, defaultValue) { it }
}