package com.arata.yukarilauncher.plugins.renderer

/**
 * APKから読み込まれたレンダラープラグイン
 * @param packageName プラグインのパッケージ名
 */
class ApkRendererPlugin(
    id: String,
    displayName: String,
    uniqueIdentifier: String,
    glName: String,
    eglName: String,
    path: String,
    env: Map<String, String>,
    dlopen: List<String>,
    val packageName: String
) : RendererPlugin(
    id, displayName, uniqueIdentifier, glName, eglName, path, env, dlopen
)