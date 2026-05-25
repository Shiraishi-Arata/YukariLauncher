package com.arata.yukarilauncher.utils

/** JSONテンプレート内の変数置換ユーティリティを提供するオブジェクト。 */
object JSONUtils {

    /**
     * 文字列配列内の全要素に対してJSON変数置換を行う。
     * @param args 置換対象の文字列配列
     * @param keyValueMap 置換キーと値のマップ
     * @return 置換後の文字列配列
     */
    fun insertJSONValueList(args: Array<String>, keyValueMap: Map<String, String>): Array<String> {
        for (i in args.indices) {
            args[i] = insertSingleJSONValue(args[i], keyValueMap)
        }
        return args
    }

    /**
     * 単一の文字列内の \${key} 形式のプレースホルダーを置換する。
     * @param value 置換対象文字列
     * @param keyValueMap 置換キーと値のマップ
     * @return 置換後の文字列
     */
    fun insertSingleJSONValue(value: String, keyValueMap: Map<String, String>): String {
        var valueInserted = value
        for (keyValue in keyValueMap) {
            valueInserted = valueInserted.replace("\${${keyValue.key}}", keyValue.value ?: "")
        }
        return valueInserted
    }
}
