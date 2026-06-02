package com.arata.yukarilauncher.feature.download.platform.update

import com.arata.yukarilauncher.feature.log.Logging
import java.io.File

/**
 * インストール済みModをスキャンしてメタデータを収集するオブジェクト
 */
object InstalledModsScanner {
    /**
     * 指定されたディレクトリ内のJARファイルをスキャンし、各Modのメタデータを解析する
     * @param modsDir Modが格納されているディレクトリ
     * @return 解析されたModInfoのリスト
     */
/**
 * scanする
 */
    fun scan(modsDir: File): List<ModMetadataReader.ModInfo> {
        val mods = mutableListOf<ModMetadataReader.ModInfo>()
        if (!modsDir.exists()) {
            Logging.i("ModUpdate", "Mods directory does not exist: $modsDir")
            return mods
        }

        val jarFiles = modsDir.listFiles { f -> f.extension == "jar" }
        if (jarFiles.isNullOrEmpty()) {
            Logging.i("ModUpdate", "No JAR files found in $modsDir")
            return mods
        }

        Logging.i("ModUpdate", "Found ${jarFiles.size} JAR files in $modsDir")
        jarFiles.forEach { jar ->
            Logging.i("ModUpdate", "Parsing ${jar.name}")
            val modInfo = ModMetadataReader.parseMod(jar)
            if (modInfo != null) {
                Logging.i(
                    "ModUpdate",
                    "Parsed ${modInfo.modName} (${modInfo.modId}) v${modInfo.version} [${modInfo.loader}] supported=${modInfo.supportedLoaders}"
                )
                mods.add(modInfo)
            } else {
                Logging.w("ModUpdate", "Failed to parse ${jar.name}")
            }
        }
        return mods
    }
}