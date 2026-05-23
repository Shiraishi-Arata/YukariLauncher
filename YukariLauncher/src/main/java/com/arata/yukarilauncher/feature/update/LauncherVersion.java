package com.arata.yukarilauncher.feature.update;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class LauncherVersion {
    @SerializedName("version_code")
    private final int versionCode;
    @SerializedName("version_name")
    private final String versionName;
    private final WhatsNew title;
    private final WhatsNew description;
    @SerializedName("published_at")
    private final String publishedAt;
    @SerializedName("file_size")
    private final FileSize fileSize;
    @SerializedName("download_link")
    private final DownloadLink downloadLink;
    @SerializedName("pre_release")
    private final boolean isPreRelease;

/**
 * LauncherVersionを構築します
 */
    public LauncherVersion(int versionCode, String versionName, WhatsNew title, WhatsNew description, String publishedAt, FileSize fileSize, DownloadLink downloadLink, boolean isPreRelease) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.title = title;
        this.description = description;
        this.publishedAt = publishedAt;
        this.fileSize = fileSize;
        this.downloadLink = downloadLink;
        this.isPreRelease = isPreRelease;
    }

/**
 * versionCodeを取得する
 * @return versionCodeの値
 */
    public int getVersionCode() {
        return versionCode;
    }

/**
 * versionNameを取得する
 * @return versionNameの値
 */
    public String getVersionName() {
        return versionName;
    }

/**
 * titleを取得する
 * @return titleの値
 */
    public WhatsNew getTitle() {
        return title;
    }

/**
 * descriptionを取得する
 * @return descriptionの値
 */
    public WhatsNew getDescription() {
        return description;
    }

/**
 * publishedAtを取得する
 * @return publishedAtの値
 */
    public String getPublishedAt() {
        return publishedAt;
    }

/**
 * fileSizeを取得する
 * @return fileSizeの値
 */
    public FileSize getFileSize() {
        return fileSize;
    }

/**
 * downloadLinkを取得する
 * @return downloadLinkの値
 */
    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

/**
 * preReleaseを取得する
 * @return preReleaseの値
 */
    public boolean isPreRelease() {
        return isPreRelease;
    }

    @NonNull
    @Override
    public String toString() {
        return "Version{" +
                "versionCode=" + versionCode +
                ", versionName='" + versionName + '\'' +
                ", title=" + title +
                ", description=" + description +
                ", publishedAt='" + publishedAt + '\'' +
                ", fileSize=" + fileSize +
                ", downloadLink=" + downloadLink +
                '}';
    }

/**
 * WhatsNew内部クラス
 */
    public static class WhatsNew {
        @SerializedName("en_us")
        private final String enUS;
        @SerializedName("ja_jp")
        private final String jaJP;

/**
 * WhatsNewを構築します
 */
        public WhatsNew(String enUS, String jaJP) {
            this.enUS = enUS;
            this.jaJP = jaJP;
        }

/**
 * enUSを取得する
 * @return enUSの値
 */
        public String getEnUS() {
            return enUS;
        }

/**
 * jaJPを取得する
 * @return jaJPの値
 */
        public String getJaJP() {
            return jaJP;
        }

        @NonNull
        @Override
        public String toString() {
            return "WhatsNew{" +
                    "enUS='" + enUS + '\'' +
                    ", jaJP='" + jaJP + '\'' +
                    '}';
        }
    }

/**
 * FileSize内部クラス
 */
    public static class FileSize {
        private final long all;
        private final long arm;
        private final long arm64;
        private final long x86;
        private final long x86_64;

/**
 * FileSizeを構築します
 */
        public FileSize(long all, long arm, long arm64, long x86, long x86_64) {
            this.all = all;
            this.arm = arm;
            this.arm64 = arm64;
            this.x86 = x86;
            this.x86_64 = x86_64;
        }

/**
 * allを取得する
 * @return allの値
 */
        public long getAll() {
            return all;
        }

/**
 * armを取得する
 * @return armの値
 */
        public long getArm() {
            return arm;
        }

/**
 * arm64を取得する
 * @return arm64の値
 */
        public long getArm64() {
            return arm64;
        }

/**
 * x86を取得する
 * @return x86の値
 */
        public long getX86() {
            return x86;
        }

/**
 * x86_64を取得する
 * @return x86_64の値
 */
        public long getX86_64() {
            return x86_64;
        }

        @NonNull
        @Override
        public String toString() {
            return "FileSize{" +
                    "all=" + all +
                    ", arm=" + arm +
                    ", arm64=" + arm64 +
                    ", x86=" + x86 +
                    ", x86_64=" + x86_64 +
                    '}';
        }
    }

/**
 * DownloadLink内部クラス
 */
    public static class DownloadLink {
        private final String all;
        private final String arm;
        private final String arm64;
        private final String x86;
        private final String x86_64;

/**
 * DownloadLinkを構築します
 */
        public DownloadLink(String all, String arm, String arm64, String x86, String x86_64) {
            this.all = all;
            this.arm = arm;
            this.arm64 = arm64;
            this.x86 = x86;
            this.x86_64 = x86_64;
        }

/**
 * allを取得する
 * @return allの値
 */
        public String getAll() {
            return all;
        }

/**
 * armを取得する
 * @return armの値
 */
        public String getArm() {
            return arm;
        }

/**
 * arm64を取得する
 * @return arm64の値
 */
        public String getArm64() {
            return arm64;
        }

/**
 * x86を取得する
 * @return x86の値
 */
        public String getX86() {
            return x86;
        }

/**
 * x86_64を取得する
 * @return x86_64の値
 */
        public String getX86_64() {
            return x86_64;
        }

        @NonNull
        @Override
        public String toString() {
            return "DownloadLink{" +
                    "all='" + all + '\'' +
                    ", arm='" + arm + '\'' +
                    ", arm64='" + arm64 + '\'' +
                    ", x86='" + x86 + '\'' +
                    ", x86_64='" + x86_64 + '\'' +
                    '}';
        }
    }
}
