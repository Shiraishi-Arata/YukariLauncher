package com.arata.yukarilauncher.plugins.renderer

/**
 * レンダラープラグインの抽象基底クラス
 * @param id レンダラーID
 * @param displayName 表示名
 * @param uniqueIdentifier 一意識別子
 * @param glName GLライブラリ名
 * @param eglName EGLライブラリ名
 * @param path ライブラリのパス
 * @param env 環境変数
 * @param dlopen dlopenするライブラリのリスト
 */
abstract class RendererPlugin(
    val id: String,
    val displayName: String,
    val uniqueIdentifier: String,
    val glName: String,
    val eglName: String,
    val path: String,
    val env: Map<String, String>,
    val dlopen: List<String>
)