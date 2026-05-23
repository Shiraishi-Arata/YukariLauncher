package net.kdt.pojavlaunch.colorselector;

/**
 * 彩度・明度（SV）矩形領域で選択が行われたときのリスナーインターフェース。
 */
public interface RectangleSelectionListener {
    /**
     * 彩度と明度が変更されたときに呼び出されます。
     * @param luminosity 明度（0〜1）
     * @param intensity 彩度（0〜1）
     */
    void onLuminosityIntensityChanged(float luminosity, float intensity);
}
