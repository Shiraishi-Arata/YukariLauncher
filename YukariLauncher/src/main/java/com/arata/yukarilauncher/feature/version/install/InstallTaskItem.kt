package com.arata.yukarilauncher.feature.version.install

import android.app.Activity
import java.io.File

/**
 * InstallTaskのラッパークラス。より詳細な情報を記録するために使用する
 * @see InstallTask
 * @property selectedVersion 選択されたバージョン
 * @property isMod Modかどうか
 * @property task インストールタスク
 * @property endTask 終了時タスク
 */
class InstallTaskItem(
    val selectedVersion: String,
    val isMod: Boolean,
    val task: InstallTask,
    val endTask: EndTask?
) {
    /**
     * インストールタスクアイテムの内容を文字列として返す
     * @return 文字列表現
     */
    override fun toString(): String {
        return "InstallTaskItem{selectedVersion='$selectedVersion', isMod='$isMod'}"
    }

    /**
     * インストール終了時に実行されるタスクのインターフェース
     */
    fun interface EndTask {
        /**
         * このタスクを使用してModLoaderのインストールを実行する
         * @param activity 現在のActivity。JRE選択ダイアログやJavaGUI画面への切り替えに使用
         * @param file 前のタスクの実行後に出力されたファイル
         */
        @Throws(Throwable::class)
        fun endTask(activity: Activity, file: File)
    }
}