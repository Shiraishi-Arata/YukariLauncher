package com.arata.yukarilauncher.setting.unit

import androidx.annotation.CheckResult
import com.arata.yukarilauncher.setting.Settings

abstract class AbstractSettingUnit<V>(
    val key: String,
    val defaultValue: V
) {
    /**
     * 現在の設定値を取得する
     * @return 設定値
     */
    abstract fun getValue(): V

    /**
     * 値を保存し、設定ビルダーを返す
     * @return 設定ビルダー
     */
    @CheckResult
    fun put(value: V): Settings.Manager.SettingBuilder = Settings.Manager.put(key, value!!)

    /**
     * 設定ユニットをデフォルト値にリセットする
     */
    fun reset() {
        Settings.Manager.put(key, defaultValue!!).save()
    }
}
