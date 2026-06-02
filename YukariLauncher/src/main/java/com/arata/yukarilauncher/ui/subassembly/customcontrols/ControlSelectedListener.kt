package com.arata.yukarilauncher.ui.subassembly.customcontrols

import java.io.File

/**
 * コントロール選択時のコールバックリスナー
 */
abstract class ControlSelectedListener {
    /** アイテムが選択された */
    abstract fun onItemSelected(file: File)
    /** アイテムが長押しされた */
    abstract fun onItemLongClick(file: File)
}