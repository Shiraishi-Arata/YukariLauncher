package com.arata.yukarilauncher.feature.mod.modloader

/** Fabricローダーのバージョン情報を保持するクラス。 */
open class FabricVersion {
    /** バージョン文字列。 */
    var version: String? = null
    /** 安定版かどうか。 */
    var stable: Boolean = false

    /** Fabricローダーのローダー記述子を表すクラス。 */
    class LoaderDescriptor : FabricVersion() {
        /** ローダーのバージョン情報。 */
        var loader: FabricVersion? = null

        /** ローダーの文字列表現を返す。 @return ローダーバージョンの文字列、nullの場合は"null" */
        override fun toString(): String {
            return loader?.toString() ?: "null"
        }
    }

    /** バージョンの文字列表現を返す。 @return バージョン文字列、nullの場合は"null" */
    override fun toString(): String {
        return version ?: "null"
    }
}
