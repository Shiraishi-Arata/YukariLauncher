package com.arata.yukarilauncher.ui.subassembly.filelist

/**
 * 更新完了時のコールバックインターフェース
 */
fun interface RefreshListener {
    /** 更新が完了した */
    fun onRefresh()
}