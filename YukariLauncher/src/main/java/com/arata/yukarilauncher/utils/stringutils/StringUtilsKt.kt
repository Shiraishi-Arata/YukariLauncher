package com.arata.yukarilauncher.utils.stringutils

import java.util.UUID

class StringUtilsKt {
    companion object {
        /**
         * 文字列がnullでなく、かつ空または空白のみでない場合にその文字列を返す
         */
        @JvmStatic
        fun getNonEmptyOrBlank(string: String?): String? {
            return string?.takeIf { it.isNotEmpty() && it.isNotBlank() }
        }

        /**
         * 文字列がnullまたは空白かどうかを判定する
         */
        @JvmStatic
        fun isBlank(string: String?): Boolean = string.isNullOrBlank()

        /**
         * 文字列がnullでなく、かつ空白でないかどうかを判定する
         */
        @JvmStatic
        fun isNotBlank(string: String?): Boolean = string?.isNotBlank() ?: false

        /**
         * 文字列が空または空白かどうかを判定する
         */
        @JvmStatic
        fun isEmptyOrBlank(string: String): Boolean = string.isEmpty() || string.isBlank()

        /**
         * 文字列から指定されたサフィックスを削除する
         */
        @JvmStatic
        fun removeSuffix(string: String, suffix: String) = string.removeSuffix(suffix)

        /**
         * 文字列から指定されたプレフィックスを削除する
         */
        @JvmStatic
        fun removePrefix(string: String, prefix: String) = string.removePrefix(prefix)

        /**
         * Unicodeエスケープ文字列（\\uXXXX形式）を実際の文字に変換する
         */
        @JvmStatic
        fun decodeUnicode(input: String): String {
            val regex = """\\u([0-9a-fA-F]{4})""".toRegex()
            var result = input
            regex.findAll(input).forEach { match ->
                val unicode = match.groupValues[1]
                val char = Character.toChars(unicode.toInt(16))[0]
                result = result.replace(match.value, char.toString())
            }
            return result
        }

        /**
         * 一意のUUIDを生成する
         * 既存のUUIDとの重複を防ぐためのチェック機能を備える
         * @param processString UUID文字列を加工する場合に指定する
         * @param checkForConflict 既存のUUIDとの重複チェックを行う場合に指定する。trueを返すと再帰的に再生成する
         */
        @JvmStatic
        fun generateUniqueUUID(
            processString: ((String) -> String)? = null,
            checkForConflict: ((String) -> Boolean)? = null
        ): String {
            val uuid = UUID.randomUUID().toString().lowercase()
            val progressedUuid = processString?.invoke(uuid) ?: uuid
            return if (checkForConflict?.invoke(progressedUuid) == true) {
                generateUniqueUUID(processString, checkForConflict)
            } else {
                progressedUuid
            }
        }
    }
}
