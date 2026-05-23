package net.kdt.pojavlaunch.customcontrols.mouse;

public interface AbstractTouchpad {
    /**
     * マウスの想定表示状態を取得します（GUI表示中にマウスを表示するかどうか）
     * これはマウスの実際の表示状態を反映しないことに注意してください
     * @return 現在の想定有効状態
     */
    boolean getDisplayState();

    /**
     * 2要素のfloat配列形式のモーションベクターをマウスに適用します。
     * これにより画面上でマウスが移動し、新しいカーソル位置がゲームに送信されます。
     * @param vector ベクターを含む配列
     */
    default void applyMotionVector(float[] vector) {
        applyMotionVector(vector[0], vector[1]);
    }

    /**
     * マウスにモーションベクターをX/Y座標の個別指定で適用します。
     * 画面上でマウスが移動し、新しいカーソル位置がゲームに送信されます。
     * @param x ベクターの相対X座標
     * @param y ベクターの相対Y座標
     */
    void applyMotionVector(float x, float y);

    /**
     * タッチパッドの状態を「有効」に設定します
     * @param supposed trueの場合は想定表示状態を有効に設定しますが、
     *                 内部条件が満たされるまでタッチパッドに反映されません。
     *                 falseの場合は内部条件に関わらずタッチパッドを有効にします。
     */
    void enable(boolean supposed);
    /**
     * タッチパッドの状態を「無効」に設定します。
     */
    void disable();
}
