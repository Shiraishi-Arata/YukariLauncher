package com.arata.yukarilauncher.setting

import androidx.annotation.CheckResult
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.arata.yukarilauncher.event.single.SettingsChangeEvent
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.setting.unit.AbstractSettingUnit
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.Tools
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

class Settings {
    companion object {
        private val GSON: Gson = GsonBuilder().disableHtmlEscaping().create()

        private val settingsLock = Any()
        private var settingsMap = ConcurrentHashMap<String, SettingAttribute>()

        /**
         * 設定ファイルから設定マップを再読み込みする
         * ファイルが存在しない場合は空のマップを返す
         * @return キーとSettingAttributeのマップ
         */
        private fun refreshSettingsMap(): Map<String, SettingAttribute> {
            return PathManager.FILE_SETTINGS.takeIf { it.exists() }?.let { file ->
                try {
                    val jsonString = Tools.read(file)
                    val listType: Type = object : TypeToken<List<SettingAttribute>>() {}.type
                    GSON.fromJson<List<SettingAttribute>>(jsonString, listType)
                        .associateBy { it.key }
                } catch (e: Exception) {
                    Logging.e("Settings", "Failed to refresh settings: ${Tools.printToString(e)}")
                    emptyMap()
                }
            } ?: emptyMap()
        }

        /**
         * すべての設定項目をリフレッシュする
         */
        @Synchronized
        fun refreshSettings() {
            settingsMap = ConcurrentHashMap(refreshSettingsMap())
        }
    }

    class Manager private constructor() {
        companion object {
            /**
             * 設定からキーに対応する値を取得する
             * @param key 設定キー
             * @param defaultValue デフォルト値
             * @param parser 文字列から型Tへのパーサー
             * @return 見つかった値、なければデフォルト値
             */
            fun <T> getValue(key: String, defaultValue: T, parser: (String) -> T?): T {
                return settingsMap[key]?.value?.let { parser(it) } ?: defaultValue
            }

            /**
             * 設定に指定されたキーが存在するかを確認する
             */
            @JvmStatic
            fun contains(key: String): Boolean {
                return settingsMap.containsKey(key)
            }

            /**
             * 設定にキーと値を設定する
             * @return SettingBuilderインスタンス
             */
            @JvmStatic
            @CheckResult
            fun put(key: String, value: Any) = SettingBuilder().put(key, value)
        }

        class SettingBuilder {
            private val valueMap = ConcurrentHashMap<String, Any>()

            /**
             * 設定にキーと値を追加する
             * @return チェーン用のSettingBuilder
             */
            @CheckResult
            fun put(key: String, value: Any): SettingBuilder {
                valueMap[key] = value
                return this
            }

            /**
             * 設定ユニットを使用して値を設定する
             * @param unit 設定ユニット
             * @return チェーン用のSettingBuilder
             */
            @CheckResult
            fun put(unit: AbstractSettingUnit<*>, value: Any): SettingBuilder {
                return put(unit.key, value)
            }

            /**
             * すべての変更を保存し、設定ファイルに書き込む
             * 保存後、設定をリフレッシュし、SettingsChangeEventを発行する
             */
            fun save() {
                val settingsFile = PathManager.FILE_SETTINGS
                val newSettings = ConcurrentHashMap(settingsMap)

                valueMap.forEach { (key, value) ->
                    newSettings[key] = SettingAttribute(key, value.toString())
                }

                synchronized(settingsLock) {
                    runCatching {
                        if (!settingsFile.exists() && !settingsFile.createNewFile()) {
                            throw IllegalStateException("Failed to create settings file")
                        }

                        val settingsList = newSettings.values.toList()
                        val json = GSON.toJson(settingsList)
                        FileUtils.write(settingsFile, json, Charsets.UTF_8)
                        refreshSettings()
                        EventBus.getDefault().post(SettingsChangeEvent())
                    }.onFailure { e ->
                        Logging.e("SettingBuilder", "Save failed!", e)
                    }
                }
            }
        }
    }

    private class SettingAttribute(
        var key: String = "",
        var value: String? = null
    )
}
