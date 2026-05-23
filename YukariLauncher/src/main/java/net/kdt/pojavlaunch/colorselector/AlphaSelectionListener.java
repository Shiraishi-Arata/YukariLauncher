package net.kdt.pojavlaunch.colorselector;

/**
 * アルファ（透明度）値が選択されたときのリスナーインターフェース。
 */
public interface AlphaSelectionListener {
    /**
     * アルファ値が選択されたときに呼び出されます。
     * @param alpha 選択されたアルファ値（0〜255）
     */
    void onAlphaSelected(int alpha);
}
