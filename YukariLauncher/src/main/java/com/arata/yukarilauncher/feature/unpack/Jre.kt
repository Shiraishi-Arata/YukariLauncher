package com.arata.yukarilauncher.feature.unpack

import com.arata.yukarilauncher.R

/**
 * 内蔵JREの種類を定義する列挙型。
 * 各JREの名称、パス、説明リソースIDを保持する。
 */
enum class Jre(val jreName: String, val jrePath: String, val summary: Int, val downloadUrl: String) {
    JRE_8("Internal-8", "jre-8", R.string.splash_screen_jre8, "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/jre/8.zip"),
    JRE_17("Internal-17", "jre-17", R.string.splash_screen_jre17, "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/jre/17.zip"),
    JRE_21("Internal-21", "jre-21", R.string.splash_screen_jre21, "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/jre/21.zip"),
    JRE_25("Internal-25", "jre-25", R.string.splash_screen_jre25, "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/jre/25.zip")
}