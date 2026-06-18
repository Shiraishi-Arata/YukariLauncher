package com.arata.yukarilauncher.feature.download.platform.update

import java.io.File

/**
 * Modのアップデート情報を保持するデータクラス
 * @property modId ModのID
 * @property modName Modの名前
 * @property currentVersion 現在インストールされているバージョン
 * @property latestVersion 最新バージョン
 * @property downloadUrl ダウンロードURL
 * @property fileName ファイル名
 * @property needsUpdate アップデートが必要かどうか
 * @property originalFile 元のファイル（存在する場合）
 */
data class ModUpdate(
    val modId: String,
    val modName: String,
    val currentVersion: String,
    val latestVersion: String,
    val downloadUrl: String,
    val fileName: String,
    val needsUpdate: Boolean,
    val originalFile: File? = null
)