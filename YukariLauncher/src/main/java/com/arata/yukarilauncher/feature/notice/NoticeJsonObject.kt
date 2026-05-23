package com.arata.yukarilauncher.feature.notice

import com.google.gson.annotations.SerializedName

/**
 * お知らせのJSONデータを保持するクラス。
 * タイトルと内容を多言語（中国語簡体字、繁体字、英語）で保持する内部クラス Text を持つ。
 */
class NoticeJsonObject(
    val title: Text,
    val content: Text,
    val date: String,
    val numbering: Int
) {
    /**
     * 多言語テキストを保持する内部クラス。
     * @param zhCN 中国語簡体字
     * @param zhTW 中国語繁体字
     * @param enUS 英語
     */
    class Text(
        @SerializedName("zh_cn") val zhCN: String,
        @SerializedName("zh_tw") val zhTW: String,
        @SerializedName("en_us") val enUS: String
    )
}
