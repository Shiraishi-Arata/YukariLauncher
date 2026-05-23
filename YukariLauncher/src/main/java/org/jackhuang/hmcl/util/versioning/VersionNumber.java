/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.util.versioning;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

/**
 * org.apache.maven.artifact.versioning.ComparableVersion からコピー
 * Apache License 2.0
 *
 * 将来 org.jenkins-ci:version-number:1.7 に移行する可能性があります
 * @see <a href="http://maven.apache.org/pom.html#Version_Order_Specification">仕様</a>
 */
public final class VersionNumber implements Comparable<VersionNumber> {

    public static final VersionNumber ZERO = asVersion("0");

    /**
     * 文字列からVersionNumberを作成します。
     * @param version バージョン文字列
     */
    public static VersionNumber asVersion(String version) {
        Objects.requireNonNull(version);
        return new VersionNumber(version);
    }

    /**
     * 2つのバージョン文字列を比較します。
     * @param version1 最初のバージョン
     * @param version2 2番目のバージョン
     * @return 比較結果
     */
    public static int compare(String version1, String version2) {
        return asVersion(version1).compareTo(asVersion(version2));
    }

    /**
     * バージョン文字列を正規化します。
     */
    public static String normalize(String str) {
        return new VersionNumber(str).getCanonical();
    }

    /**
     * 文字列が整数のみのバージョン番号かどうかを判定します。
     */
    public static boolean isIntVersionNumber(String version) {
        if (version.isEmpty()) {
            return false;
        }

        int idx = 0;
        boolean cont = true;
        do {
            int dotIndex = version.indexOf('.', idx);
            if (dotIndex == idx || dotIndex == version.length() - 1) {
                return false;
            }

            int endIndex;
            if (dotIndex < 0) {
                cont = false;
                endIndex = version.length();
            } else {
                endIndex = dotIndex;
            }

            if (endIndex - idx > 9)
                // 10^10より大きい数は整数として保存できない
                return false;

            for (int i = idx; i < endIndex; i++) {
                char ch = version.charAt(i);
                if (ch < '0' || ch > '9')
                    return false;
            }

            idx = endIndex + 1;
        } while (cont);

        return true;
    }

    /**
     * 文字列範囲を使用してVersionRangeを作成します。
     */
    public static VersionRange<VersionNumber> between(String minimum, String maximum) {
        return VersionRange.between(asVersion(minimum), asVersion(maximum));
    }

    /**
     * 最小値以上のVersionRangeを作成します。
     */
    public static VersionRange<VersionNumber> atLeast(String minimum) {
        return VersionRange.atLeast(asVersion(minimum));
    }

    /**
     * 最大値以下のVersionRangeを作成します。
     */
    public static VersionRange<VersionNumber> atMost(String maximum) {
        return VersionRange.atMost(asVersion(maximum));
    }

    /**
     * バージョンアイテムリスト内のアイテムを表すインターフェース
     */
    private interface Item {
        int LONG_ITEM = 0;
        int BIGINTEGER_ITEM = 1;
        int STRING_ITEM = 2;
        int LIST_ITEM = 3;

        /**
         * このアイテムを指定されたアイテムと比較します。
         */
        int compareTo(Item item);

        /**
         * アイテムのタイプを返します。
         */
        int getType();

        /**
         * このアイテムがnullと見なされるかどうかを返します。
         */
        boolean isNull();

        /**
         * アイテムの文字列表現をバッファに追加します。
         */
        void appendTo(StringBuilder buffer);
    }

    /**
     * long値として表現可能な数値アイテム
     */
    private static final class LongItem implements Item {
        private final long value;

        public static final LongItem ZERO = new LongItem(0L);

        /**
         * long値アイテムを構築します。
         */
        LongItem(long value) {
            this.value = value;
        }

        @Override
        public int getType() {
            return LONG_ITEM;
        }

        @Override
        public boolean isNull() {
            return value == 0L;
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                return value == 0L ? 0 : 1;
            }

            switch (item.getType()) {
                case LONG_ITEM:
                    long itemValue = ((LongItem) item).value;
                    return Long.compare(value, itemValue);
                case BIGINTEGER_ITEM:
                    return -1;

                case STRING_ITEM:
                    return 1;

                case LIST_ITEM:
                    return 1;

                default:
                    throw new AssertionError("invalid item: " + item.getClass());
            }
        }

        /**
         * 数値をバッファに追加します。
         */
        @Override
        public void appendTo(StringBuilder buffer) {
            buffer.append(value);
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

    /**
     * BigIntegerとして表現される数値アイテム（longの範囲を超える場合）
     */
    private static final class BigIntegerItem implements Item {
        private final BigInteger value;

        /**
         * BigInteger値アイテムを構築します。
         */
        BigIntegerItem(String str) {
            this.value = new BigInteger(str);
        }

        @Override
        public int getType() {
            return BIGINTEGER_ITEM;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                return 1;
            }

            switch (item.getType()) {
                case LONG_ITEM:
                    return 1;
                case BIGINTEGER_ITEM:
                    return value.compareTo(((BigIntegerItem) item).value);

                case STRING_ITEM:
                    return 1;

                case LIST_ITEM:
                    return 1;

                default:
                    throw new AssertionError("invalid item: " + item.getClass());
            }
        }

        /**
         * BigInteger値をバッファに追加します。
         */
        @Override
        public void appendTo(StringBuilder buffer) {
            buffer.append(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    /**
     * バージョンアイテムリスト内の文字列（通常は修飾子）を表します。
     */
    private static final class StringItem implements Item {
        private final String value;

        /**
         * 文字列アイテムを構築します。
         */
        StringItem(String value) {
            this.value = value;
        }

        @Override
        public int getType() {
            return STRING_ITEM;
        }

        @Override
        public boolean isNull() {
            return value.isEmpty();
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                return 1;
            }
            switch (item.getType()) {
                case LONG_ITEM:
                case BIGINTEGER_ITEM:
                    return -1;

                case STRING_ITEM:
                    return value.compareTo(((StringItem) item).value);

                case LIST_ITEM:
                    return -1;

                default:
                    throw new AssertionError("invalid item: " + item.getClass());
            }
        }

        /**
         * 文字列値をバッファに追加します。
         */
        @Override
        public void appendTo(StringBuilder buffer) {
            buffer.append(value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * バージョンリストアイテムを表します。グローバルアイテムリストとサブリスト（
     * バージョン仕様で'-(number)'で始まるもの）の両方に使用されます。
     */
    private static final class ListItem extends ArrayList<Item> implements Item {
        private final Character separator;

        /**
         * 空のリストアイテムを構築します。
         */
        ListItem() {
            this.separator = null;
        }

        /**
         * 指定されたセパレーター文字でリストアイテムを構築します。
         */
        ListItem(char separator) {
            this.separator = separator;
        }

        @Override
        public int getType() {
            return LIST_ITEM;
        }

        @Override
        public boolean isNull() {
            return size() == 0;
        }

        /**
         * 末尾のnullアイテムを削除して正規化します。
         */
        void normalize() {
            for (int i = size() - 1; i >= 0; i--) {
                Item lastItem = get(i);

                if (lastItem.isNull()) {
                    remove(i);
                } else if (!(lastItem instanceof ListItem)) {
                    break;
                }
            }
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                if (size() == 0) {
                    return 0;
                }
                Item first = get(0);
                return first.compareTo(null);
            }
            switch (item.getType()) {
                case LONG_ITEM:
                case BIGINTEGER_ITEM:
                    return -1;

                case STRING_ITEM:
                    return 1;

                case LIST_ITEM:
                    Iterator<Item> left = iterator();
                    Iterator<Item> right = ((ListItem) item).iterator();

                    while (left.hasNext() || right.hasNext()) {
                        Item l = left.hasNext() ? left.next() : null;
                        Item r = right.hasNext() ? right.next() : null;

                        int result = l == null ? (r == null ? 0 : -1 * r.compareTo(l)) : l.compareTo(r);

                        if (result != 0) {
                            return result;
                        }
                    }

                    return 0;

                default:
                    throw new AssertionError("invalid item: " + item.getClass());
            }
        }

        /**
         * リスト内の全アイテムをバッファに追加します。
         */
        @Override
        public void appendTo(StringBuilder buffer) {
            if (separator != null) {
                buffer.append((char) separator);
            }

            final int initLength = buffer.length();

            for (Item item : this) {
                if (buffer.length() > initLength) {
                    if (!(item instanceof ListItem))
                        buffer.append('.');
                }
                item.appendTo(buffer);
            }
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            appendTo(buffer);
            return buffer.toString();
        }
    }

    private static final int MAX_LONGITEM_LENGTH = 18;

    private final String value;
    private final ListItem items;
    private final String canonical;

    /**
     * バージョン文字列をパースします。
     */
    private VersionNumber(String version) {
        this.value = version;

        ListItem list = this.items = new ListItem();

        Deque<Item> stack = new ArrayDeque<>();
        stack.push(list);

        boolean isDigit = false;

        int startIndex = 0;

        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);

            if (c == '.') {
                if (i == startIndex) {
                    list.add(LongItem.ZERO);
                } else {
                    list.add(parseItem(version.substring(startIndex, i)));
                }
                startIndex = i + 1;
            } else if ("!\"#$%&'()*+,-/:;<=>?@[\\]^_`{|}~".indexOf(c) != -1) {
                if (i == startIndex) {
                    list.add(LongItem.ZERO);
                } else {
                    list.add(parseItem(version.substring(startIndex, i)));
                }
                startIndex = i + 1;

                list.add(list = new ListItem(c));
                stack.push(list);
            } else if (c >= '0' && c <= '9') {
                if (!isDigit && i > startIndex) {
                    list.add(parseItem(version.substring(startIndex, i)));
                    startIndex = i;

                    list.add(list = new ListItem());
                    stack.push(list);
                }

                isDigit = true;
            } else {
                if (isDigit && i > startIndex) {
                    list.add(parseItem(version.substring(startIndex, i)));
                    startIndex = i;

                    list.add(list = new ListItem());
                    stack.push(list);
                }

                isDigit = false;
            }
        }

        if (version.length() > startIndex) {
            list.add(parseItem(version.substring(startIndex)));
        }

        while (!stack.isEmpty()) {
            list = (ListItem) stack.pop();
            list.normalize();
        }

        this.canonical = items.toString();
    }

    /**
     * 単純バージョン用のコンストラクタ
     */
    private VersionNumber(String version, ListItem items) {
        this.value = version;
        this.items = items;
        this.canonical = version;
    }

    /**
     * 文字列を解析してItemを生成します。
     */
    private static Item parseItem(String buf) {
        int numberLength = 0;
        boolean leadingZero = true;
        for (int i = 0; i < buf.length(); i++) {
            char ch = buf.charAt(i);
            if (ch >= '0' && ch <= '9') {
                if (ch != '0') {
                    leadingZero = false;
                }

                if (!leadingZero) {
                    numberLength++;
                }
            } else {
                return new StringItem(buf);
            }
        }

        if (numberLength == 0) {
            return LongItem.ZERO;
        } else if (numberLength <= MAX_LONGITEM_LENGTH) {
            return new LongItem(Long.parseLong(buf));
        } else {
            return new BigIntegerItem(buf);
        }
    }

    /**
     * 文字列と比較します。
     */
    public int compareTo(String o) {
        return compareTo(VersionNumber.asVersion(o));
    }

    /**
     * このバージョンを指定されたバージョンと比較します。
     */
    @Override
    public int compareTo(VersionNumber o) {
        return items.compareTo(o.items);
    }

    /**
     * 元のバージョン文字列を返します。
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * 正規化されたバージョン文字列を返します。
     */
    public String getCanonical() {
        return canonical;
    }

    /**
     * このバージョンが指定されたオブジェクトと等しいかどうかを判定します。
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof VersionNumber && canonical.equals(((VersionNumber) o).canonical);
    }

    /**
     * 正規化されたバージョン文字列のハッシュコードを返します。
     */
    @Override
    public int hashCode() {
        return canonical.hashCode();
    }
}
