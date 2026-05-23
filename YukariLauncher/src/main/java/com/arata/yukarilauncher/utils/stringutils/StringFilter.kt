package com.arata.yukarilauncher.utils.stringutils

import java.util.Locale
import java.util.regex.Pattern

class StringFilter {
    companion object {
        /**
         * 入力文字列が指定された部分文字列を含むかどうかをチェックする
         * @param input 入力文字列
         * @param substring 検索する部分文字列
         * @param caseSensitive 大文字と小文字を区別する場合はtrue
         * @return 部分文字列が含まれていればtrue、そうでなければfalse
         */
        @JvmStatic
        fun containsSubstring(input: String, substring: String, caseSensitive: Boolean): Boolean {
            val adjustedInput = if (caseSensitive) input else input.lowercase(Locale.getDefault())
            val adjustedSubstring =
                if (caseSensitive) substring else substring.lowercase(Locale.getDefault())
            val regex = Pattern.quote(adjustedSubstring)
            val compiledPattern = Pattern.compile(regex)
            val matcher = compiledPattern.matcher(adjustedInput)
            return matcher.find()
        }
    }
}
