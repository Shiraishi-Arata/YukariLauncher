package com.arata.yukarilauncher.ui.view.colorselector

/** 色相が選択された際のコールバックインターフェース。 */
interface HueSelectionListener {
    /** 色相が選択されたときに呼び出される。 @param hue 選択された色相値（0～360） */
    fun onHueSelected(hue: Float)
}