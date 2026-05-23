package com.arata.yukarilauncher.setting.unit

import com.arata.yukarilauncher.setting.Settings.Manager

class BooleanSettingUnit(key: String, defaultValue: Boolean) : AbstractSettingUnit<Boolean>(key, defaultValue) {
    /**
     * 真偽値の設定値を取得する
     */
    override fun getValue() = Manager.getValue(key, defaultValue) { it.toBoolean() }
}
