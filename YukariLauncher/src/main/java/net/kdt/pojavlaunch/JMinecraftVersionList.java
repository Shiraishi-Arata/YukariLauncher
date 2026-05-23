package net.kdt.pojavlaunch;

import androidx.annotation.Keep;

import net.kdt.pojavlaunch.value.DependentLibrary;
import net.kdt.pojavlaunch.value.MinecraftClientInfo;

import java.util.Map;

/**
 * マインクラフトのバージョンリストをJSONからデシリアライズするためのクラス。
 */
@Keep
@SuppressWarnings("unused") // 未使用フィールドはすべてJSON構造の一部です
public class JMinecraftVersionList {
    /** 最新バージョンのマップ（release, snapshot） */
    public Map<String, String> latest;
    /** 全バージョンの配列 */
    public Version[] versions;

    /**
     * ファイルプロパティ（id, sha1, url, size）を保持する基底クラス。
     */
    @Keep
    public static class FileProperties {
        public String id, sha1, url;
        public long size;
    }

    /**
     * 個々のバージョン情報を保持するクラス。
     */
    @Keep
    public static class Version extends FileProperties {
        // 1.13以降の引数
        public Arguments arguments;
        /** アセットインデックス */
        public AssetIndex assetIndex;

        public String assets;
        /** ダウンロード情報のマップ */
        public Map<String, MinecraftClientInfo> downloads;
        /** 継承元バージョン */
        public String inheritsFrom;
        /** Javaバージョン情報 */
        public JavaVersionInfo javaVersion;
        /** 依存ライブラリ */
        public DependentLibrary[] libraries;
        /** メインクラス名 */
        public String mainClass;
        /** マインクラフト引数（1.12以前） */
        public String minecraftArguments;
        /** 最小ランチャーバージョン */
        public int minimumLauncherVersion;
        /** リリース日時 */
        public String releaseTime;
        public String time;
        public String type;
    }

    /**
     * Javaバージョン情報。
     */
    @Keep
    public static class JavaVersionInfo {
        public String component;
        public int majorVersion;
        public int version; // LabyMod 4で使用されるパラメータ
    }

    /**
     * 1.13以降の引数構造。
     */
    @Keep
    public static class Arguments {
        public Object[] game;
        public Object[] jvm;

        @Keep
        public static class ArgValue {
            public ArgRules[] rules;
            public String value;

            // TLauncherスタイルの引数
            public String[] values;

            @Keep
            public static class ArgRules {
                public String action;
                public String features;
                public ArgOS os;

                @Keep
                public static class ArgOS {
                    public String name;
                    public String version;
                }
            }
        }
    }

    /**
     * アセットインデックス情報。
     */
    @Keep
    public static class AssetIndex extends FileProperties {
        public long totalSize;
    }
}
