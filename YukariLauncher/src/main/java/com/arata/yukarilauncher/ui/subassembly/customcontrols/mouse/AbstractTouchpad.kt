package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

/** タッチパッドの抽象インターフェース。マウス操作のためのモーション適用と状態管理を定義します。 */
interface AbstractTouchpad {
    /** 表示状態。 */
    val displayState: Boolean

    /**
     * モーションベクトルを適用します。
     * @param vector [x, y]のモーションベクトル
     */
    fun applyMotionVector(vector: FloatArray) {
        applyMotionVector(vector[0], vector[1])
    }

    /**
     * モーションベクトルを適用します。
     * @param x X方向の移動量
     * @param y Y方向の移動量
     */
    fun applyMotionVector(x: Float, y: Float)
    /**
     * タッチパッドを有効にします。
     * @param supposed 想定される有効状態
     */
    fun enable(supposed: Boolean)
    /** タッチパッドを無効にします。 */
    fun disable()
}
