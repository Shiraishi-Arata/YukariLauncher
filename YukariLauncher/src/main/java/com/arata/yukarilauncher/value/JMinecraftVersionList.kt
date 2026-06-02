package com.arata.yukarilauncher.value

import androidx.annotation.Keep
import com.arata.yukarilauncher.value.DependentLibrary
import com.arata.yukarilauncher.value.MinecraftClientInfo

/**
 * Minecraftバージョン一覧のJSONデータを表現するクラス。
 * Mojangのバージョンマニフェストをデシリアライズするために使用されます。
 * 最新バージョン情報と利用可能なすべてのバージョンのリストを保持します。
 */
@Keep
@Suppress("unused")
class JMinecraftVersionList {
    /** 最新バージョンのマップ（キー: "release", "snapshot"）。 */
    var latest: MutableMap<String, String>? = null
    /** 利用可能なすべてのバージョンの配列。 */
    @JvmField var versions: Array<Version>? = null

    /**
     * ファイルプロパティの基底クラス。
     * バージョンファイルのID、SHA-1ハッシュ、URL、サイズを保持します。
     */
    @Keep
    open class FileProperties {
        /** ファイルの識別子。 */
        @JvmField var id: String? = null
        /** ファイルのSHA-1ハッシュ値。 */
        var sha1: String? = null
        /** ファイルのダウンロードURL。 */
        var url: String? = null
        /** ファイルのサイズ（バイト単位）。 */
        var size: Long = 0
    }

    /**
     * 個別のMinecraftバージョン情報を保持するクラス。
     * FilePropertiesを継承し、バージョン固有の詳細情報を追加で保持します。
     */
    @Keep
    class Version : FileProperties() {
        /** 起動引数の設定。 */
        var arguments: Arguments? = null
        /** アセットインデックスの情報。 */
        var assetIndex: AssetIndex? = null
        /** アセットのバージョン識別子。 */
        var assets: String? = null
        /** ダウンロード情報のマップ。 */
        var downloads: MutableMap<String, MinecraftClientInfo>? = null
        /** 継承元のバージョンID。 */
        var inheritsFrom: String? = null
        /** Javaバージョン情報。 */
        var javaVersion: JavaVersionInfo? = null
        /** 依存ライブラリの配列。 */
        var libraries: Array<DependentLibrary>? = null
        /** メインクラス名。 */
        var mainClass: String? = null
        /** レガシーなMinecraft起動引数。 */
        var minecraftArguments: String? = null
        /** 必要最小限のランチャーバージョン。 */
        var minimumLauncherVersion: Int = 0
        /** リリース日時。 */
        @JvmField var releaseTime: String? = null
        /** 更新日時。 */
        @JvmField var time: String? = null
        /** バージョンタイプ（"release", "snapshot", "old_beta"など）。 */
        @JvmField var type: String? = null
    }

    /**
     * Javaバージョン情報を保持するクラス。
     */
    @Keep
    class JavaVersionInfo {
        /** コンポーネント名。 */
        var component: String? = null
        /** メジャーバージョン番号。 */
        var majorVersion: Int = 0
        /** バージョン番号。 */
        var version: Int = 0
    }

    /**
     * 起動引数の設定を保持するクラス。
     * game引数とjvm引数をそれぞれ管理します。
     */
    @Keep
    class Arguments {
        /** ゲーム起動引数の配列。 */
        var game: Array<Any>? = null
        /** JVM起動引数の配列。 */
        var jvm: Array<Any>? = null

        /**
         * 引数値を保持するクラス。
         * ルールベースの条件付き引数に対応します。
         */
        @Keep
        class ArgValue {
            /** この引数を適用するルールの配列。 */
            var rules: Array<ArgValue.ArgRules>? = null
            /** 引数の値。 */
            var value: String? = null
            /** 複数の値を持つ場合の配列。 */
            var values: Array<String>? = null

            /**
             * 引数の適用ルールを定義するクラス。
             * アクション、機能要件、OS要件を指定します。
             */
            @Keep
            class ArgRules {
                /** ルールのアクション（"allow" または "disallow"）。 */
                var action: String? = null
                /** 必要な機能。 */
                var features: String? = null
                /** OS条件。 */
                var os: ArgRules.ArgOS? = null

                /**
                 * OS条件を定義するクラス。
                 */
                @Keep
                class ArgOS {
                    /** OS名。 */
                    var name: String? = null
                    /** OSバージョンのパターン。 */
                    var version: String? = null
                }
            }
        }
    }

    /**
     * アセットインデックス情報を保持するクラス。
     * FilePropertiesを継承し、合計サイズを追加で保持します。
     */
    @Keep
    class AssetIndex : FileProperties() {
        /** 全アセットの合計サイズ（バイト単位）。 */
        var totalSize: Long = 0
    }
}