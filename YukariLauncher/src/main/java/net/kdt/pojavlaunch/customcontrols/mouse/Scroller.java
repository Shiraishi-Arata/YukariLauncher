package net.kdt.pojavlaunch.customcontrols.mouse;

import org.lwjgl.glfw.CallbackBridge;

public class Scroller {

    private float mScrollOvershootH, mScrollOvershootV;
    private final float mScrollThreshold;
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public Scroller(float mScrollThreshold) {
        this.mScrollThreshold = mScrollThreshold;
    }

    /**
     * スクロールジェスチャーを実行します。
     * @param dx プライマリポインターのベクターX座標
     * @param dy プライマリポインターのベクターY座標
     */
    public void performScroll(float dx, float dy) {
        float hScroll = (dx / mScrollThreshold) + mScrollOvershootH;
        float vScroll = (dy / mScrollThreshold) + mScrollOvershootV;
        int hScrollRound = (int) hScroll, vScrollRound = (int) vScroll;
        if(hScrollRound != 0 || vScrollRound != 0) CallbackBridge.sendScroll(hScroll, vScroll);
        mScrollOvershootH = hScroll - hScrollRound;
        mScrollOvershootV = vScroll - vScrollRound;
    }

    /**
     * スクロールジェスチャーを実行します。
     * @param vector プライマリポインターの相対位置を格納する2成分ベクター
     */
    public void performScroll(float[] vector) {
        performScroll(vector[0], vector[1]);
    }

    /**
     * スクロールオーバーシュート値をリセットします。スクロールオーバーシュートによりスクロールが
     * ぎこちなくなりますが、スクロールジェスチャーの終了時にリセットしないと異常が発生します。
     */
    public void resetScrollOvershoot() {
        mScrollOvershootH = mScrollOvershootV = 0f;
    }
}
