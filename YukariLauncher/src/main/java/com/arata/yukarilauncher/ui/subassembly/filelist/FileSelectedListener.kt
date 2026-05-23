package com.arata.yukarilauncher.ui.subassembly.filelist

import java.io.File

/**
 * ファイル選択時のコールバックリスナー
 */
abstract class FileSelectedListener {
    /** ファイルが選択された */
    abstract fun onFileSelected(file: File?, path: String?)
    /** アイテムが長押しされた */
    abstract fun onItemLongClick(file: File?, path: String?)
}
