package com.arata.yukarilauncher.event.sticky

/**
 * ファイル選択結果を通知するイベント
 * @param path 選択されたファイルのパス（nullの場合は選択なし）
 */
data class FileSelectorEvent(val path: String?)