package org.jackhuang.hmcl.util.versioning;

import java.util.Objects;

/**
 * @author Glavo
 */
@SuppressWarnings("unchecked")
public final class VersionRange<T extends Comparable<T>> {
    private static final VersionRange<?> EMPTY = new VersionRange<>(null, null);
    private static final VersionRange<?> ALL = new VersionRange<>(null, null);

    /**
     * 空のバージョン範囲を返します。
     */
    public static <T extends Comparable<T>> VersionRange<T> empty() {
        return (VersionRange<T>) EMPTY;
    }

    /**
     * すべてを含むバージョン範囲を返します。
     */
    public static <T extends Comparable<T>> VersionRange<T> all() {
        return (VersionRange<T>) ALL;
    }

    /**
     * 指定された最小値と最大値の間のバージョン範囲を返します。
     * @param minimum 最小値
     * @param maximum 最大値
     */
    public static <T extends Comparable<T>> VersionRange<T> between(T minimum, T maximum) {
        assert minimum.compareTo(maximum) <= 0;
        return new VersionRange<>(minimum, maximum);
    }

    /**
     * 指定された最小値以上のバージョン範囲を返します。
     * @param minimum 最小値
     */
    public static <T extends Comparable<T>> VersionRange<T> atLeast(T minimum) {
        assert minimum != null;
        return new VersionRange<>(minimum, null);
    }

    /**
     * 指定された最大値以下のバージョン範囲を返します。
     * @param maximum 最大値
     */
    public static <T extends Comparable<T>> VersionRange<T> atMost(T maximum) {
        assert maximum != null;
        return new VersionRange<>(null, maximum);
    }

    private final T minimum;
    private final T maximum;

    /**
     * バージョン範囲を構築します。
     * @param minimum 最小値（null可）
     * @param maximum 最大値（null可）
     */
    private VersionRange(T minimum, T maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    /**
     * 最小値を返します。
     */
    public T getMinimum() {
        return minimum;
    }

    /**
     * 最大値を返します。
     */
    public T getMaximum() {
        return maximum;
    }

    /**
     * 空の範囲かどうかを返します。
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * すべてを含む範囲かどうかを返します。
     */
    public boolean isAll() {
        return !isEmpty() && minimum == null && maximum == null;
    }

    /**
     * 指定されたバージョンがこの範囲に含まれるかどうかを判定します。
     */
    public boolean contains(T versionNumber) {
        if (versionNumber == null) return false;
        if (isEmpty()) return false;
        if (isAll()) return true;

        return (minimum == null || minimum.compareTo(versionNumber) <= 0) && (maximum == null || maximum.compareTo(versionNumber) >= 0);
    }

    /**
     * 指定された範囲と重なっているかどうかを判定します。
     */
    public boolean isOverlappedBy(final VersionRange<T> that) {
        if (this.isEmpty() || that.isEmpty())
            return false;

        if (this.isAll() || that.isAll())
            return true;

        if (this.minimum == null)
            return that.minimum == null || that.minimum.compareTo(this.maximum) <= 0;

        if (this.maximum == null)
            return that.maximum == null || that.maximum.compareTo(this.minimum) >= 0;

        return that.contains(minimum) || that.contains(maximum) || (that.minimum != null && contains(that.minimum));
    }

    /**
     * この範囲と指定された範囲の共通部分を返します。
     */
    public VersionRange<T> intersectionWith(VersionRange<T> that) {
        if (this.isAll())
            return that;
        if (that.isAll())
            return this;

        if (!isOverlappedBy(that))
            return empty();

        T newMinimum;
        if (this.minimum == null)
            newMinimum = that.minimum;
        else if (that.minimum == null)
            newMinimum = this.minimum;
        else
            newMinimum = this.minimum.compareTo(that.minimum) >= 0 ? this.minimum : that.minimum;

        T newMaximum;
        if (this.maximum == null)
            newMaximum = that.maximum;
        else if (that.maximum == null)
            newMaximum = this.maximum;
        else
            newMaximum = this.maximum.compareTo(that.maximum) <= 0 ? this.maximum : that.maximum;

        return new VersionRange<>(newMinimum, newMaximum);
    }

    /**
     * この範囲のハッシュコードを返します。
     */
    @Override
    public int hashCode() {
        if (isEmpty())
            return 1121763849;
        if (isAll())
            return -475303149;

        return Objects.hash(minimum) ^ Objects.hash(maximum);
    }

    /**
     * この範囲が指定されたオブジェクトと等しいかどうかを判定します。
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof VersionRange))
            return false;

        VersionRange<T> that = (VersionRange<T>) obj;

        return this.isEmpty() == that.isEmpty() && this.isAll() == that.isAll()
                && Objects.equals(this.minimum, that.minimum)
                && Objects.equals(this.maximum, that.maximum);
    }

    /**
     * この範囲の文字列表現を返します。
     */
    @Override
    public String toString() {
        if (isEmpty())
            return "EMPTY";

        if (isAll())
            return "ALL";

        if (minimum == null)
            return "At most " + maximum;

        if (maximum == null)
            return "At least " + minimum;

        return "[" + minimum + ".." + maximum + "]";
    }
}
