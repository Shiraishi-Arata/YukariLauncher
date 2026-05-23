package com.arata.yukarilauncher.utils.mouse;

/**
 * カーソルのホットスポット情報を提供するインターフェース
 * カスタムカーソル描画の際にホットスポット座標とベースサイズを取得するために使用する
 */
public interface CursorHotspotAware {
    /**
     * ホットスポットのX座標を取得する
     */
    int getHotspotX();
    /**
     * ホットスポットのY座標を取得する
     */
    int getHotspotY();
    /**
     * ベースとなる幅を取得する
     */
    int getBaseWidth();
    /**
     * ベースとなる高さを取得する
     */
    int getBaseHeight();
}
