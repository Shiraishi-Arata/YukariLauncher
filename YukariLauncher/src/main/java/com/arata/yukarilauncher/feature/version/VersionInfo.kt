package com.arata.yukarilauncher.feature.version

import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.Tools
import java.io.File
import java.io.FileWriter

/**
 * バージョンの詳細情報（MinecraftバージョンとModLoader情報）
 * @param minecraftVersion Minecraftのバージョン文字列
 * @param loaderInfo ModLoader情報の配列
 */
class VersionInfo(
    val minecraftVersion: String,
    val loaderInfo: Array<LoaderInfo>?
) {
    /**
     * Minecraftのバージョン情報を、ModLoader情報を含めて連結する
     * @return カンマ＋スペースで区切られた情報文字列
     */
    fun getInfoString(): String {
        val infoList = mutableListOf<String>().apply {
            add(minecraftVersion)
            loaderInfo?.forEach { info ->
                when {
                    info.name.isNotBlank() && info.version.isNotBlank() -> add("${info.name} - ${info.version}")
                    info.name.isNotBlank() -> add(info.name)
                    info.version.isNotBlank() -> add(info.version)
                }
            }
        }
        return infoList.joinToString(", ")
    }

    /**
     * ModLoaderの情報
     * @param name ModLoaderの名前
     * @param version ModLoaderのバージョン
     */
    data class LoaderInfo(
        val name: String,
        val version: String
    ) {
        /**
         * ローダー名から対応する環境変数キーを取得する
         * @return 環境変数キー。該当なしの場合はnull
         */
        fun getLoaderEnvKey(): String? {
            return when(name) {
                "OptiFine" -> "INST_OPTIFINE"
                "Forge" -> "INST_FORGE"
                "NeoForge" -> "INST_NEOFORGE"
                "Fabric" -> "INST_FABRIC"
                "Quilt" -> "INST_QUILT"
                "LiteLoader" -> "INST_LITELOADER"
                else -> null
            }
        }
    }

    /**
     * VersionInfoを指定されたバージョンフォルダに保存する
     * @param versionFolder 保存先のバージョンフォルダ
     */
    fun save(versionFolder: File) {
        runCatching {
            val yukariVersionPath = VersionsManager.getYukariVersionPath(versionFolder)
            val infoFile = File(yukariVersionPath, "VersionInfo.json")
            if (!yukariVersionPath.exists()) yukariVersionPath.mkdirs()

            FileWriter(infoFile, false).use {
                val json = Tools.GLOBAL_GSON.toJson(this)
                it.write(json)
            }
        }.onFailure { e -> Logging.e("Save Version Info", Tools.printToString(e)) }
    }
}
