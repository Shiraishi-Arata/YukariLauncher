package com.arata.yukarilauncher.feature.version.install

import java.io.File

/**
 * ModLoaderやModのインストールタスクを定義するインターフェース
 */
interface InstallTask {
    /**
     * インストールタスクを実行する
     * @param customName カスタムバージョン名
     * @return インストール後に出力されたファイル。nullの場合は出力なし
     */
    @Throws(Exception::class)
    fun run(customName: String): File?
}