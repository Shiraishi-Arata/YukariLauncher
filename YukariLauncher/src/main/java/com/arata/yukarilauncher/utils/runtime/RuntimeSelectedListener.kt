package com.arata.yukarilauncher.utils.runtime

/**
 * ランタイム選択時のコールバックインターフェース
 * JRE名が選択された際に呼び出されるリスナー
 */
fun interface RuntimeSelectedListener {
    /**
     * JREが選択された時に呼び出される
     * @param jreName 選択されたJREの名前。nullの場合は自動選択
     */
    fun onSelected(jreName: String?)
}