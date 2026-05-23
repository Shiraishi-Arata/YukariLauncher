package net.kdt.pojavlaunch.utils;

import java.util.List;

public class MathUtils {

    /**
     * https://www.arduino.cc/reference/en/language/functions/math/map/ から移植
     * 値をある範囲から別の範囲にマッピングします。
     */
    public static float map(float x, float in_min, float in_max, float out_min, float out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    /**
     * 2点間の距離を返します。
     */
    public static float dist(float x1, float y1, float x2, float y2) {
        final float x = (x2 - x1);
        final float y = (y2 - y1);
        return (float) Math.hypot(x, y);
    }

    /**
     * targetValueに最も近い（または以上）値を持つオブジェクトTを検索します。
     * @param targetValue ターゲット値
     * @param objects 検索対象のオブジェクトリスト
     * @param valueProvider 各値のプロバイダー
     * @return targetValueに最も近い値を持つオブジェクトをラップしたRankedValue、またはすべてのオブジェクトの値がtargetValue未満の場合はnull
     * @param <T> 検索に使用されるオブジェクト型
     */
    public static <T> RankedValue<T> findNearestPositive(int targetValue, List<T> objects, ValueProvider<T> valueProvider) {
        int delta = Integer.MAX_VALUE;
        T selectedObject = null;
        for(T object : objects) {
            int objectValue = valueProvider.getValue(object);
            if(objectValue < targetValue) continue;

            int currentDelta = objectValue - targetValue;
            if(currentDelta == 0) return new RankedValue<>(object, 0);
            if(currentDelta >= delta) continue;

            selectedObject = object;
            delta = currentDelta;
        }
        if(selectedObject == null) return null;
        return new RankedValue<>(selectedObject, delta);
    }

    public interface ValueProvider<T> {
        int getValue(T object);
    }

    public static final class RankedValue<T> {
        public final T value;
        public final int rank;
        public RankedValue(T value, int rank) {
            this.value = value;
            this.rank = rank;
        }
    }

    /**
     * 2つのオブジェクトのうち、値が低い方を選択します。
     * @param object1 比較対象のオブジェクト1
     * @param object2 比較対象のオブジェクト2
     * @param valueProvider オブジェクトの値プロバイダー
     * @return オブジェクト1の値がオブジェクト2以下の場合はオブジェクト1、そうでない場合はオブジェクト2
     * @param <T> オブジェクトの型
     */
    public static <T> T objectMin(T object1, T object2, ValueProvider<T> valueProvider) {
        if(object1 == null) return object2;
        if(object2 == null) return object1;
        int value1 = valueProvider.getValue(object1);
        int value2 = valueProvider.getValue(object2);
        if(value1 <= value2) {
            return object1;
        } else {
            return object2;
        }
    }
}
