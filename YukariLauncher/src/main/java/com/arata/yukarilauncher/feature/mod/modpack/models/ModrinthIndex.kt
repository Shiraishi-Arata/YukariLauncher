package com.arata.yukarilauncher.feature.mod.modpack.models

import java.util.Arrays

/** ModrinthのModパックインデックスを表すモデルクラス。 */
class ModrinthIndex {
    /** フォーマットバージョン。 */
    var formatVersion: Int = 0
    /** ゲーム識別子（例: "minecraft"）。 */
    var game: String? = null
    /** バージョンID。 */
    var versionId: String? = null
    /** Modパック名。 */
    var name: String? = null
    /** 概要説明。 */
    var summary: String? = null
    /** 含まれるファイルの配列。 */
    var files: Array<ModrinthIndexFile>? = null
    /** 依存関係マップ（キー: 依存関係名、値: バージョン）。 */
    var dependencies: Map<String, String>? = null

    /** Modrinthインデックス内の個別ファイル情報。 */
    class ModrinthIndexFile {
        /** ファイルのパス。 */
        var path: String? = null
        /** ダウンロードURLの配列。 */
        var downloads: Array<String>? = null
        /** ファイルサイズ（バイト）。 */
        var fileSize: Int = 0
        /** ファイルのハッシュ情報。 */
        var hashes: ModrinthIndexFileHashes? = null
        /** ファイルの環境設定（クライアント/サーバー）。 */
        var env: ModrinthIndexFileEnv? = null

        /** 文字列表現を返す。 @return ファイル情報の文字列 */
        override fun toString(): String {
            return "ModrinthIndexFile{" +
                    "path='" + path + '\'' +
                    ", downloads=" + Arrays.toString(downloads) +
                    ", fileSize=" + fileSize +
                    ", hashes=" + hashes +
                    '}'
        }

        /** ファイルのハッシュ情報（SHA1 / SHA512）。 */
        class ModrinthIndexFileHashes {
            /** SHA1ハッシュ。 */
            var sha1: String? = null
            /** SHA512ハッシュ。 */
            var sha512: String? = null

            /** 文字列表現を返す。 @return ハッシュ情報の文字列 */
            override fun toString(): String {
                return "ModrinthIndexFileHashes{" +
                        "sha1='" + sha1 + '\'' +
                        ", sha512='" + sha512 + '\'' +
                        '}'
            }
        }

        /** ファイルの対応環境設定。 */
        class ModrinthIndexFileEnv {
            /** クライアントでの対応状況（"required" / "optional" / "unsupported"）。 */
            var client: String? = null
            /** サーバーでの対応状況。 */
            var server: String? = null

            /** 文字列表現を返す。 @return 環境設定の文字列 */
            override fun toString(): String {
                return "ModrinthIndexFileEnv{" +
                        "client='" + client + '\'' +
                        ", server='" + server + '\'' +
                        '}'
            }
        }
    }

    /** 文字列表現を返す。 @return インデックス情報の文字列 */
    override fun toString(): String {
        return "ModrinthIndex{" +
                "formatVersion=" + formatVersion +
                ", game='" + game + '\'' +
                ", versionId='" + versionId + '\'' +
                ", name='" + name + '\'' +
                ", summary='" + summary + '\'' +
                ", files=" + Arrays.toString(files) +
                '}'
    }
}
