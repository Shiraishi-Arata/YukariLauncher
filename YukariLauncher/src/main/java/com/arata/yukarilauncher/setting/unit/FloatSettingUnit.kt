package com.arata.yukarilauncher.setting.unit

import com.arata.yukarilauncher.setting.Settings.Manager

class FloatSettingUnit(key: String, defaultValue: Float) : AbstractSettingUnit<Float>(key, defaultValue) {
    /**
     * Float型の設定値を取得する
     */
    override fun getValue() = Manager.getValue(key, defaultValue) { it.toFloatOrNull() }
}
