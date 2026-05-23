package com.arata.yukarilauncher.utils.image

/**
 * 画像の幅と高さを保持するデータクラス
 */
class Dimension(@JvmField var width: Int, @JvmField var height: Int) {
    override fun toString(): String {
        return "Dimension{" +
                "width=" + width +
                ", height=" + height +
                '}'
    }
}
