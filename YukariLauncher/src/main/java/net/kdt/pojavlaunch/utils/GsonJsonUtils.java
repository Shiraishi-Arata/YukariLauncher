package net.kdt.pojavlaunch.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GsonJsonUtils {
    /**
     * JsonElementを安全にJsonObjectに変換します。
     * @param element 入力のJsonElement
     * @return 以下の条件を満たす場合にJsonObjectを返します:
     *         JsonElementがnullでない
     *         JsonElementがJson nullでない
     *         JsonElementがJsonObjectである
     *         それ以外の場合はnull
     */
    public static JsonObject getJsonObjectSafe(JsonElement element) {
        if(element == null) return null;
        if(element.isJsonNull() || !element.isJsonObject()) return null;
        return element.getAsJsonObject();
    }

    /**
     * JsonObjectからJsonElementを安全に取得します。
     * @param jsonObject 入力のJsonObject
     * @param memberName JsonElementのメンバー名
     * @return 以下の条件を満たす場合にJsonElementを返します:
     *         入力のJsonObjectがnullでない
     *         入力のJsonObjectが指定されたmemberNameの要素を含む
     *         JsonElementがJson nullでない
     *         それ以外の場合はnull
     */
    public static JsonElement getElementSafe(JsonObject jsonObject, String memberName) {
        if(jsonObject == null) return null;
        if(!jsonObject.has(memberName)) return null;
        JsonElement element = jsonObject.get(memberName);
        if(element.isJsonNull()) return null;
        return element;
    }

    /**
     * JsonObjectから子JsonObjectを安全に取得します。
     * @param jsonObject 入力のJsonObject
     * @param memberName 出力JsonObjectのメンバー名
     * @return 以下の条件を満たす場合に出力JsonObjectを返します:
     *         入力のJsonObjectがnullでない
     *         入力のJsonObjectが指定されたmemberNameの要素を含む
     *         出力JsonObjectがJson nullでない
     *         出力JsonObjectがJsonObjectである
     *         それ以外の場合はnull
     */
    public static JsonObject getJsonObjectSafe(JsonObject jsonObject, String memberName) {
        return getJsonObjectSafe(getElementSafe(jsonObject, memberName));
    }

    /**
     * JsonObjectからJsonArrayを安全に取得します。
     * @param jsonObject 入力のJsonObject
     * @param memberName JsonArrayのメンバー名
     * @return 以下の条件を満たす場合にJsonArrayを返します:
     *         入力のJsonObjectがnullでない
     *         入力のJsonObjectが指定されたmemberNameの要素を含む
     *         JsonArrayがJson nullでない
     *         JsonArrayがJsonArrayである
     *         それ以外の場合はnull
     */
    public static JsonArray getJsonArraySafe(JsonObject jsonObject, String memberName) {
        JsonElement jsonElement = getElementSafe(jsonObject, memberName);
        if(jsonElement == null || !jsonElement.isJsonArray()) return null;
        return jsonElement.getAsJsonArray();
    }

    /**
     * JsonObjectからint値を安全に取得します。
     * @param jsonObject 入力のJsonObject
     * @param memberName int値のメンバー名
     * @param onNullValue チェックに失敗した場合に返される値
     * @return 以下の条件を満たす場合にint値を返します:
     *         入力のJsonObjectがnullでない
     *         入力のJsonObjectが指定されたmemberNameの要素を含む
     *         int値がJson nullでない
     *         int値が実際の整数である
     *         それ以外の場合はonNullValue
     */
    public static int getIntSafe(JsonObject jsonObject, String memberName, int onNullValue) {
        JsonElement jsonElement = getElementSafe(jsonObject, memberName);
        if(jsonElement == null || !jsonElement.isJsonPrimitive()) return onNullValue;
        try {
            return jsonElement.getAsInt();
        }catch (ClassCastException e) {
            return onNullValue;
        }
    }

    /**
     * JsonObjectからStringを安全に取得します。
     * @param jsonObject 入力のJsonObject
     * @param memberName int値のメンバー名
     * @return 以下の条件を満たす場合にStringを返します:
     *         入力のJsonObjectがnullでない
     *         入力のJsonObjectが指定されたmemberNameの要素を含む
     *         StringがJson nullでない
     *         Stringが実際の文字列である
     *         それ以外の場合はnull
     */
    public static String getStringSafe(JsonObject jsonObject, String memberName) {
        JsonElement jsonElement = getElementSafe(jsonObject, memberName);
        if(jsonElement == null || !jsonElement.isJsonPrimitive()) return null;
        try {
            return jsonElement.getAsString();
        }catch (ClassCastException e) {
            return null;
        }
    }
}
