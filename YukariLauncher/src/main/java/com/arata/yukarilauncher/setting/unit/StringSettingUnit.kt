package com.arata.yukarilauncher.setting.unit

import com.arata.yukarilauncher.setting.Settings.Manager

class StringSettingUnit(key: String, defaultValue: String) : AbstractSettingUnit<String>(key, defaultValue) {
    override fun getValue() = Manager.getValue(key, defaultValue) { it }
}