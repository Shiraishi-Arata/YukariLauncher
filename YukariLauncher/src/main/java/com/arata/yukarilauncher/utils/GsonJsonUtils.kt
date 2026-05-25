package com.arata.yukarilauncher.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** Gsonを使用した安全なJSON要素取得ユーティリティを提供するオブジェクト。 */
object GsonJsonUtils {

    /** JsonElementを安全にJsonObjectに変換する。 @param element 入力要素 @return JsonObject、変換不可の場合は null */
    fun getJsonObjectSafe(element: JsonElement?): JsonObject? {
        if (element == null) return null
        if (element.isJsonNull || !element.isJsonObject) return null
        return element.asJsonObject
    }

    /** JsonObjectからメンバー要素を安全に取得する。 @param jsonObject 対象オブジェクト @param memberName メンバー名 @return JsonElement、存在しないまたはnullの場合は null */
    fun getElementSafe(jsonObject: JsonObject?, memberName: String): JsonElement? {
        if (jsonObject == null) return null
        if (!jsonObject.has(memberName)) return null
        val element = jsonObject.get(memberName)
        if (element.isJsonNull) return null
        return element
    }

    /** JsonObjectから子JsonObjectを安全に取得する。 @param jsonObject 対象オブジェクト @param memberName メンバー名 @return JsonObject、存在しない場合は null */
    fun getJsonObjectSafe(jsonObject: JsonObject?, memberName: String): JsonObject? {
        return getJsonObjectSafe(getElementSafe(jsonObject, memberName))
    }

    /** JsonObjectからJsonArrayを安全に取得する。 @param jsonObject 対象オブジェクト @param memberName メンバー名 @return JsonArray、存在しない場合は null */
    fun getJsonArraySafe(jsonObject: JsonObject?, memberName: String): JsonArray? {
        val jsonElement = getElementSafe(jsonObject, memberName)
        if (jsonElement == null || !jsonElement.isJsonArray) return null
        return jsonElement.asJsonArray
    }

    /** JsonObjectから整数値を安全に取得する。 @param jsonObject 対象オブジェクト @param memberName メンバー名 @param onNullValue 取得不可時のデフォルト値 @return 整数値 */
    fun getIntSafe(jsonObject: JsonObject?, memberName: String, onNullValue: Int): Int {
        val jsonElement = getElementSafe(jsonObject, memberName)
        if (jsonElement == null || !jsonElement.isJsonPrimitive) return onNullValue
        return try {
            jsonElement.asInt
        } catch (e: ClassCastException) {
            onNullValue
        }
    }

    /** JsonObjectから文字列を安全に取得する。 @param jsonObject 対象オブジェクト @param memberName メンバー名 @return 文字列、存在しない場合は null */
    fun getStringSafe(jsonObject: JsonObject?, memberName: String): String? {
        val jsonElement = getElementSafe(jsonObject, memberName)
        if (jsonElement == null || !jsonElement.isJsonPrimitive) return null
        return try {
            jsonElement.asString
        } catch (e: ClassCastException) {
            null
        }
    }
}
