package com.arata.yukarilauncher.feature.mod.models;

/**
 * MCBBS形式の整合包メタデータを保持するクラス。
 * 作者、バージョン、説明、ファイル情報などを格納する。
 */
public class MCBBSPackMeta {
    public String author;
    public String version;
    public String description;
    public String fileApi;

    public MCBBSFile[] files;
    public MCBBSAddons[] addons;
    public MCBBSLaunchInfo launchInfo;
    public String manifestType;
    public int manifestVersion;
    public String name;

    /**
     * MCBBS整合包内の個別ファイル情報。
     */
    public static class MCBBSFile {
        public String hash;
        public String path;
        public boolean force;
        public String type;
    }

    /**
     * MCBBS整合包のアドオン情報。
     */
    public static class MCBBSAddons {
        public String id;
        public String version;
    }

    /**
     * MCBBS整合包の起動情報。
     * 最小メモリ、起動引数、JVM引数を保持する。
     */
    public static class MCBBSLaunchInfo {
        public int minMemory;
        public String[] launchArgument;
        public String[] javaArgument;
    }
}
