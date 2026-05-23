package com.arata.yukarilauncher.feature.mod.modpack.install

/**
 * 整合包インストール時の追加情報を保持するクラス。
 * インストール開始フラグと整合包ファイルのパスを格納する。
 */
class InstallExtra(
    @JvmField var startInstall: Boolean,
    @JvmField var modpackPath: String
)
