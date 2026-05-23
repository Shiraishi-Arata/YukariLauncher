package com.arata.yukarilauncher.feature

import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.utils.MCVersionRegex
import com.arata.yukarilauncher.utils.YLTools
import org.jackhuang.hmcl.util.versioning.VersionNumber
import org.jackhuang.hmcl.util.versioning.VersionRange

class ProfileLanguageSelector {
    companion object {
/**
 * 古いMCバージョンの言語コード形式に変換する
 * @param lang 言語コード
 * @return 変換後のコード
 */
        private fun getOlderLanguage(lang: String): String {
            val underscoreIndex = lang.indexOf('_')
            return if (underscoreIndex != -1) {
                //只将下划线后面的字符转换为大写
                val builder = StringBuilder(lang.substring(0, underscoreIndex + 1))
                builder.append(lang.substring(underscoreIndex + 1).uppercase())
                builder.toString()
            } else lang
        }

/**
 * getLanguageする
 */
        private fun getLanguage(minecraftVersion: Version, rawLang: String): String {
            val lang = if (rawLang == "system") YLTools.getSystemLanguage() else rawLang

            val version: String = minecraftVersion.getVersionInfo()?.minecraftVersion ?: "1.11"

            val versionId = VersionNumber.asVersion(version).canonical
            Logging.i("ProfileLanguageSelector", "Version Id : $versionId")

            return when {
                versionId.contains('.') -> {
                    if (isOlderVersionRelease(versionId)) getOlderLanguage(lang) // 1.10 -
                    else lang
                }
                MCVersionRegex.SNAPSHOT_REGEX.matcher(versionId).matches() -> { // 快照版本 "24w09a" "16w20a"
                    if (isOlderVersionSnapshot(versionId)) getOlderLanguage(lang)
                    else lang
                }
                else -> lang
            }
        }

/**
 * 1.10.2以前のリリース版かを判定する
 */
        private fun isOlderVersionRelease(versionName: String): Boolean {
            return VersionRange.atMost(VersionNumber.asVersion("1.10.2")).contains(VersionNumber.asVersion(versionName))
        }

/**
 * 16w32a以前のスナップショットかを判定する
 */
        private fun isOlderVersionSnapshot(versionName: String): Boolean {
            return VersionRange.atMost(VersionNumber.asVersion("16w32a")).contains(VersionNumber.asVersion(versionName))
        }

        @JvmStatic
/**
 * ゲームの言語をMCOptionsに設定する
 */
        fun setGameLanguage(version: Version, overridden: Boolean) {
            if (MCOptions.containsKey("lang") && !overridden) return
            val language = getLanguage(version, AllSettings.setGameLanguage.getValue())
            Logging.i("ProfileLanguageSelector", "The game language has been set to: $language")
            MCOptions.set("lang", language)
        }
    }
}
