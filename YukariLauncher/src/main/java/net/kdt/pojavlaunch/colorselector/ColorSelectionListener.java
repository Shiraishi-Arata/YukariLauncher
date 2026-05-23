package net.kdt.pojavlaunch.colorselector;

/**
 * 色が選択されたときのリスナーインターフェース。
 */
public interface ColorSelectionListener {
    /**
     * ColorSelectorによって色が選択されたときに呼び出されます。
     * @param color 選択された色
     */
    void onColorSelected(int color);
}
