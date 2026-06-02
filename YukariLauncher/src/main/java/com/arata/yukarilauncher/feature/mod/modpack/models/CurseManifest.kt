package com.arata.yukarilauncher.feature.mod.modpack.models

/** CurseForgeのModパックマニフェストを表すモデルクラス。 */
class CurseManifest {
    /** Modパック名。 */
    var name: String? = null
    /** Modパックのバージョン。 */
    var version: String? = null
    /** 作者名。 */
    var author: String? = null
    /** マニフェストタイプ。 */
    var manifestType: String? = null
    /** マニフェストバージョン。 */
    var manifestVersion: Int = 0
    /** 含まれるModファイルの配列。 */
    var files: Array<CurseFile>? = null
    /** Minecraft設定情報。 */
    var minecraft: CurseMinecraft? = null
    /** 上書き設定のパス。 */
    var overrides: String? = null
    /** プロフィール画像のパス（ZIP内の相対パス）。 */
    var image: String? = null

    /** CurseForgeのModファイル情報。 */
    class CurseFile {
        /** CurseForge上のプロジェクトID。 */
        var projectID: Long = 0
        /** ファイルID。 */
        var fileID: Long = 0
        /** 必須Modかどうか。 */
        var required: Boolean = false
    }

    /** MinecraftのバージョンとModローダー情報。 */
    class CurseMinecraft {
        /** Minecraftのバージョン。 */
        var version: String? = null
        /** 使用するModローダーの配列。 */
        var modLoaders: Array<CurseModLoader>? = null
    }

    /** CurseForgeのModローダー情報。 */
    class CurseModLoader {
        /** ローダーのID（例: "forge-36.2.0"）。 */
        var id: String? = null
        /** プライマリローダーかどうか。 */
        var primary: Boolean = false
    }
}
