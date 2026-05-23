package com.arata.yukarilauncher.utils

import java.util.regex.Pattern

/**
 * Minecraftバージョン文字列の正規表現パターンを管理するクラス
 */
class MCVersionRegex {
    companion object {
        /** リリースバージョン（例: 1.20.4, 1.20）にマッチする正規表現 */
        @JvmStatic val RELEASE_REGEX: Pattern = Pattern.compile("^\\d+\\.\\d+\\.\\d+$|^\\d+\\.\\d+$")
        /** スナップショットバージョンにマッチする正規表現 */
        @JvmStatic val SNAPSHOT_REGEX: Pattern = Pattern.compile("^\\d+[a-zA-Z]\\d+[a-zA-Z]$")
    }
}
