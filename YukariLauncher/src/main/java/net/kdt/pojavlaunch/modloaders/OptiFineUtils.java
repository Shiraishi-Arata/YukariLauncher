package net.kdt.pojavlaunch.modloaders;

import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.util.List;

public class OptiFineUtils {

    /**
     * OptiFineの利用可能なバージョン一覧をダウンロードします。
     * @param force キャッシュを無視する場合はtrue
     * @return OptiFineバージョン情報
     * @throws Exception ダウンロードまたはパースに失敗した場合
     */
    public static OptiFineVersions downloadOptiFineVersions(boolean force) throws Exception {
        return DownloadUtils.downloadStringCached("https://optifine.net/downloads", "of_downloads_page", force, new OptiFineScraper());
    }

    /**
     * OptiFineのバージョン情報を保持するクラス
     */
    public static class OptiFineVersions {
        public List<String> minecraftVersions;
        public List<List<OptiFineVersion>> optifineVersions;
    }

    /**
     * 個別のOptiFineバージョンを表すクラス
     */
    public static class OptiFineVersion {
        public String minecraftVersion;
        public String versionName;
        public String downloadUrl;
    }
}
