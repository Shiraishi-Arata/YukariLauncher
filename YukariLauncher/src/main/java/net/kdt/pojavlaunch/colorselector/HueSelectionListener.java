package net.kdt.pojavlaunch.colorselector;

/**
 * 色相が選択されたときのリスナーインターフェース。
 */
public interface HueSelectionListener {
    /**
     * 色相が選択されたときに呼び出されます。
     * @param hue 選択された色相値（0〜360）
     */
   void onHueSelected(float hue);
}
