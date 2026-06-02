package com.arata.yukarilauncher.plugins.renderer

import java.io.File

/**
 * ローカルファイルシステムから読み込まれたレンダラープラグイン
 * @param folderPath プラグインが格納されているフォルダのパス
 */
class LocalRendererPlugin(
    id: String,
    displayName: String,
    uniqueIdentifier: String,
    glName: String,
    eglName: String,
    path: String,
    env: Map<String, String>,
    dlopen: List<String>,
    val folderPath: File
) : RendererPlugin(
    id, displayName, uniqueIdentifier, glName, eglName, path, env, dlopen
)