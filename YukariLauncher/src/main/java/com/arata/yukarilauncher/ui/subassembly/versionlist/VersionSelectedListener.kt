package com.arata.yukarilauncher.ui.subassembly.versionlist

/**
 * バージョン選択時のコールバックリスナー
 */
abstract class VersionSelectedListener {
    /**
     * バージョンが選択された
     */
    abstract fun onVersionSelected(version: String?)
}