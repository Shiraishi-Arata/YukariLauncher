package com.arata.yukarilauncher.setting.unit

import com.arata.yukarilauncher.setting.Settings.Manager

class DoubleSettingUnit(key: String, defaultValue: Double) : AbstractSettingUnit<Double>(key, defaultValue) {
    override fun getValue() = Manager.getValue(key, defaultValue) { it.toDoubleOrNull() }
}