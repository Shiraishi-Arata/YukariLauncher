package com.arata.yukarilauncher.feature.customprofilepath

/**
 * プロファイルパスのJSONデータを保持するデータクラス。
 * タイトルとパスのペアを格納し、Gsonによるシリアライズ/デシリアライズに使用される。
 */
class ProfilePathJsonObject(@JvmField var title: String, @JvmField var path: String)