package com.arata.yukarilauncher.ui.view.colorselector

/** アルファ値（透明度）が選択された際のコールバックインターフェース。 */
interface AlphaSelectionListener {
    /** アルファ値が選択されたときに呼び出される。 @param alpha 選択されたアルファ値（0～255） */
    fun onAlphaSelected(alpha: Int)
}