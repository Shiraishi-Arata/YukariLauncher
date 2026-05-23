package com.arata.yukarilauncher.utils.file

/**
 * ファイル操作の進捗状況を通知するインターフェース
 */
interface FileSearchProgress {
    /**
     * 現在処理済みのファイル数を取得する
     */
    fun getCurrentFileCount(): Long = 0
    /**
     * 全ファイルの合計サイズを取得する
     */
    fun getTotalSize(): Long = 0
    /**
     * 未処理のファイルサイズを取得する
     */
    fun getPendingSize(): Long = 0
}
