package com.arata.yukarilauncher.feature.mod.modloader

import com.arata.yukarilauncher.utils.http.DownloadUtils

/** OptiFineのバージョン情報を管理するユーティリティオブジェクト。 */
object OptiFineUtils {

    /** OptiFineのバージョン一覧をダウンロードして取得する。 @param force キャッシュを無視して再取得する場合はtrue @return OptiFineのバージョン情報 */
    fun downloadOptiFineVersions(force: Boolean): OptiFineVersions {
        return DownloadUtils.downloadStringCached("https://optifine.net/downloads", "of_downloads_page", force, OptiFineScraper())
    }

    /** OptiFineの全バージョン情報を保持するデータクラス。 */
    class OptiFineVersions {
        /** Minecraftバージョンのリスト。 */
        var minecraftVersions: MutableList<String>? = null
        /** MinecraftバージョンごとのOptiFineバージョンリスト。 */
        var optifineVersions: MutableList<List<OptiFineVersion>>? = null
    }

    /** 個別のOptiFineバージョン情報を保持するデータクラス。 */
    class OptiFineVersion {
        /** 対応するMinecraftバージョン。 */
        var minecraftVersion: String? = null
        /** OptiFineのバージョン名。 */
        var versionName: String? = null
        /** ダウンロードURL。 */
        var downloadUrl: String? = null
    }
}
