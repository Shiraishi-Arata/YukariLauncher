package com.arata.yukarilauncher.feature.notice

/**
 * お知らせ情報を保持するデータクラス。
 * タイトル、内容、日付、番号を格納する。
 */
data class NoticeInfo(
    @JvmField val title: String,
    @JvmField val content: String,
    @JvmField val date: String,
    @JvmField val numbering: Int
)
