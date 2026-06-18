package com.arata.yukarilauncher.ui.view.colorselector

/** 彩度・明度の矩形選択が変更された際のコールバックインターフェース。 */
interface RectangleSelectionListener {
    /** 彩度・明度が変更されたときに呼び出される。 @param luminosity 明度 @param intensity 彩度（0～1） */
    fun onLuminosityIntensityChanged(luminosity: Float, intensity: Float)
}