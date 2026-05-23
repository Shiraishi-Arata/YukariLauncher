package net.kdt.pojavlaunch.utils;

import java.util.Map;

public class JSONUtils {
    /**
     * 文字列配列内の全ての${key}プレースホルダーを対応する値で置き換えます。
     * @param args 入力の文字列配列
     * @param keyValueMap キーと値のマップ
     * @return 置き換え後の文字列配列
     */
    public static String[] insertJSONValueList(String[] args, Map<String, String> keyValueMap) {
        for (int i = 0; i < args.length; i++) {
            args[i] = insertSingleJSONValue(args[i], keyValueMap);
        }
        return args;
    }
    
    /**
     * 単一の文字列内の全ての${key}プレースホルダーを対応する値で置き換えます。
     * @param value 入力の文字列
     * @param keyValueMap キーと値のマップ
     * @return 置き換え後の文字列
     */
    public static String insertSingleJSONValue(String value, Map<String, String> keyValueMap) {
        String valueInserted = value;
        for (Map.Entry<String, String> keyValue : keyValueMap.entrySet()) {
            valueInserted = valueInserted.replace("${" + keyValue.getKey() + "}", keyValue.getValue() == null ? "" : keyValue.getValue());
        }
        return valueInserted;
    }
}
