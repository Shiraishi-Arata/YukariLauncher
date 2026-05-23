package com.arata.yukarilauncher.plugins.renderer

/**
 * レンダラープラグインの設定を表すデータクラス
 * @param pluginVersion プラグインバージョン
 * @param rendererId レンダラーID
 * @param rendererDisplayName 表示名
 * @param glName GLライブラリ名
 * @param eglName EGLライブラリ名
 * @param boatEnv Boat環境変数マップ
 * @param pojavEnv Pojav環境変数マップ
 * @param dlopenList dlopenするライブラリのリスト
 */
data class RendererConfig(
    val pluginVersion: Int,
    var rendererId: String,
    var rendererDisplayName: String,
    var glName: String,
    var eglName: String,
    val boatEnv: Map<String, String>,
    val pojavEnv: Map<String, String>,
    val dlopenList: List<String>?
)
