package com.arata.yukarilauncher.ui.view.colorselector

/** 色が選択された際のコールバックインターフェース。 */
interface ColorSelectionListener {
    /** 色が選択されたときに呼び出される。 @param color 選択された色（ARGB） */
    fun onColorSelected(color: Int)
}
