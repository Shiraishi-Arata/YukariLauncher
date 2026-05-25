package com.arata.yukarilauncher.utils.runtime

import java.util.Objects

/** Javaランタイム（JRE）の情報を保持するデータクラス。 */
class Runtime(
    /** ランタイムの名前（ディレクトリ名）。 */
    val name: String,
    /** バージョン文字列（releaseファイルから取得）。 */
    val versionString: String?,
    /** アーキテクチャ文字列（例: aarch64）。 */
    val arch: String?,
    /** Javaメジャーバージョン番号。 */
    val javaVersion: Int
) {
    /** ランチャーに組み込みで提供されているランタイムかどうか。 */
    var isProvidedByLauncher: Boolean = false

    /** バージョン情報がないランタイム用のコンストラクタ。 @param name ランタイム名 */
    constructor(name: String) : this(name, null, null, 0)

    /**
     * 等価比較（名前ベース）。
     * @param other 比較対象
     * @return 等しい場合は true
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val runtime = other as Runtime
        return name == runtime.name
    }

    /** ハッシュコードを返す（名前ベース）。 @return ハッシュコード */
    override fun hashCode(): Int = Objects.hash(name)
}
