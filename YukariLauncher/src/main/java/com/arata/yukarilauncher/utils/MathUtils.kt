package com.arata.yukarilauncher.utils

/** 算術演算や値比較に関するユーティリティを提供するオブジェクト。 */
object MathUtils {

    /**
     * 値をある範囲から別の範囲にマッピングする。
     * @param x マッピング元の値
     * @param in_min 入力範囲の最小値
     * @param in_max 入力範囲の最大値
     * @param out_min 出力範囲の最小値
     * @param out_max 出力範囲の最大値
     * @return マッピング後の値
     */
    fun map(x: Float, in_min: Float, in_max: Float, out_min: Float, out_max: Float): Float {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
    }

    /**
     * 2点間のユークリッド距離を計算する。
     * @param x1 1点目のX座標
     * @param y1 1点目のY座標
     * @param x2 2点目のX座標
     * @param y2 2点目のY座標
     * @return 距離
     */
    fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = x2 - x1
        val y = y2 - y1
        return Math.hypot(x.toDouble(), y.toDouble()).toFloat()
    }

    /**
     * 指定された値以上で最も近い値を持つオブジェクトをリストから検索する。
     * @param targetValue 目標値
     * @param objects 検索対象のリスト
     * @param valueProvider 値プロバイダ
     * @return 最も近い値を持つRankedValue、見つからない場合は null
     */
    fun <T> findNearestPositive(targetValue: Int, objects: List<T>, valueProvider: ValueProvider<T>): RankedValue<T>? {
        var delta = Int.MAX_VALUE
        var selectedObject: T? = null
        for (obj in objects) {
            val objectValue = valueProvider.getValue(obj)
            if (objectValue < targetValue) continue

            val currentDelta = objectValue - targetValue
            if (currentDelta == 0) return RankedValue(obj, 0)
            if (currentDelta >= delta) continue

            selectedObject = obj
            delta = currentDelta
        }
        if (selectedObject == null) return null
        return RankedValue(selectedObject, delta)
    }

    /** オブジェクトから整数値を取得するインターフェース。 */
    fun interface ValueProvider<T> {
        /** @param obj 対象オブジェクト @return 整数値 */
        fun getValue(obj: T): Int
    }

    /** 値とそのランク（差）を保持するクラス。 @param value 値 @param rank ランク */
    class RankedValue<T>(val value: T, val rank: Int)

    /**
     * 2つのオブジェクトのうち、値プロバイダが返す値が小さい方を返す。
     * @param object1 比較対象1
     * @param object2 比較対象2
     * @param valueProvider 値プロバイダ
     * @return 値が小さい方のオブジェクト
     */
    fun <T> objectMin(object1: T?, object2: T?, valueProvider: ValueProvider<T>): T? {
        if (object1 == null) return object2
        if (object2 == null) return object1
        val value1 = valueProvider.getValue(object1)
        val value2 = valueProvider.getValue(object2)
        return if (value1 <= value2) object1 else object2
    }
}