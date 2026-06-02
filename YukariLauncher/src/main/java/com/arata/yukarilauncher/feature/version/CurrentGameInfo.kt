package com.arata.yukarilauncher.feature.version

import com.google.gson.annotations.SerializedName
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 現在のゲーム状態情報（旧設定からの移行をサポート）
 * @property version 現在選択されているバージョン名
 * @property favoritesMap お気に入りマッピングテーブル <お気に入り名, 含まれるバージョンのセット>
 */
data class CurrentGameInfo(
    @SerializedName("version")
    var version: String = "",
    @SerializedName("favoritesInfo")
    val favoritesMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
) {
    /**
     * 現在の状態を原子的にファイルに保存する
     */
    fun saveCurrentInfo() {
        val infoFile = getInfoFile()
        runCatching {
            FileUtils.writeByteArrayToFile(
                infoFile,
                Tools.GLOBAL_GSON.toJson(this).toByteArray(Charsets.UTF_8)
            )
        }.onFailure { e ->
            Logging.e("CurrentGameInfo", "Save failed: ${infoFile.absolutePath}", e)
        }
    }

    companion object {
        /**
         * @return 現在のゲーム情報ファイルを取得する
         */
        private fun getInfoFile() = File(ProfilePathHome.getGameHome(), "CurrentInfo.cfg")

        /**
         * @return 旧バージョンのゲーム情報ファイルを取得する
         */
        private fun getLegacyInfoFile() = File(ProfilePathHome.getGameHome(), "CurrentVersion.cfg")

        /**
         * 最新のゲーム情報を取得する（旧設定の移行を自動処理）
         * @return 現在のゲーム情報
         */
        fun refreshCurrentInfo(): CurrentGameInfo {
            val infoFile = getInfoFile()
            val legacyInfoFile = getLegacyInfoFile()

            return try {
                when {
                    infoFile.exists() -> loadFromJsonFile(infoFile)
                    legacyInfoFile.exists() -> migrateLegacyConfig(legacyInfoFile)
                    else -> createNewConfig()
                }
            } catch (e: Exception) {
                Logging.e("CurrentGameInfo", "Refresh failed", e)
                createNewConfig()
            }
        }

        /**
         * JSONファイルからCurrentGameInfoを読み込む
         * @param infoFile JSONファイル
         * @return 読み込まれたCurrentGameInfo
         */
        private fun loadFromJsonFile(infoFile: File): CurrentGameInfo {
            return Tools.GLOBAL_GSON.fromJson(infoFile.readText(), CurrentGameInfo::class.java)
                .also { info -> checkNotNull(info) { "Deserialization returned null" } }
        }

        /**
         * 旧設定ファイルから新形式に移行する
         * @param infoFile 旧設定ファイル
         * @return 移行後のCurrentGameInfo
         */
        private fun migrateLegacyConfig(infoFile: File): CurrentGameInfo {
            return CurrentGameInfo().apply {
                version = infoFile.takeIf { it.exists() }?.readText() ?: ""
                infoFile.delete()
            }.applyPostActions()
        }

        /**
         * 新しいデフォルト設定を作成する
         * @return 新しいCurrentGameInfo
         */
        private fun createNewConfig() = CurrentGameInfo().applyPostActions()

        /**
         * 保存処理を含む後処理を実行する
         * @return 処理後のCurrentGameInfo
         */
        private fun CurrentGameInfo.applyPostActions(): CurrentGameInfo {
            saveCurrentInfo()
            return this
        }
    }
}