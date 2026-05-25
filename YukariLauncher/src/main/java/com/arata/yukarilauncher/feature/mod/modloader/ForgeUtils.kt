package com.arata.yukarilauncher.feature.mod.modloader

import com.arata.yukarilauncher.utils.http.DownloadUtils
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

/** Forgeのバージョン取得とインストーラURL生成を行うユーティリティオブジェクト。 */
object ForgeUtils {
    /** ForgeのMavenメタデータURL。 */
    private const val FORGE_METADATA_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"
    /** ForgeインストーラJARのURLテンプレート。 */
    private const val FORGE_INSTALLER_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/%1\$s/forge-%1\$s-installer.jar"

    /** Forgeのバージョン一覧をダウンロードして取得する。 @param force キャッシュを無視して再取得する場合はtrue @return バージョン文字列のリスト */
    @JvmStatic fun downloadForgeVersions(force: Boolean): List<String> {
        val parserFactory = SAXParserFactory.newInstance()
        val saxParser = parserFactory.newSAXParser()

        return DownloadUtils.downloadStringCached(FORGE_METADATA_URL, "forge_versions", force) { input ->
            try {
                val handler = ForgeVersionListHandler()
                saxParser.parse(InputSource(StringReader(input)), handler)
                handler.versions
            } catch (e: Exception) {
                throw DownloadUtils.ParseException(e)
            }
        }
    }

    /** 指定されたバージョンのForgeインストーラURLを取得する。 @param version Forgeのバージョン @return インストーラJARのURL */
    @JvmStatic fun getInstallerUrl(version: String): String {
        return String.format(FORGE_INSTALLER_URL, version)
    }
}
