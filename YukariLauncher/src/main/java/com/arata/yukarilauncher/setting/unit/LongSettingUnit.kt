package com.arata.yukarilauncher.setting.unit

import com.arata.yukarilauncher.setting.Settings.Manager

class LongSettingUnit(key: String, defaultValue: Long) : AbstractSettingUnit<Long>(key, defaultValue) {
    /**
     * Long型の設定値を取得する
     */
    override fun getValue() = Manager.getValue(key, defaultValue) { it.toLongOrNull() }
}
