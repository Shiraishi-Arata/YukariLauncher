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
package org.jackhuang.hmcl.util;

import com.arata.yukarilauncher.utils.stringutils.StringUtilsKt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author huangyuhui
 */
public final class StringUtils {

    /**
     * ユーティリティクラスのプライベートコンストラクタ。
     */
    private StringUtils() {
    }

    /**
     * 文字が変数名の開始文字として有効かどうかを判定します。
     */
    private static boolean isVarNameStart(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
    }

    /**
     * 文字が変数名の一部として有効かどうかを判定します。
     */
    private static boolean isVarNamePart(char ch) {
        return isVarNameStart(ch) || (ch >= '0' && ch <= '9');
    }

    /**
     * 指定された位置から変数名の終端を検索します。
     */
    private static int findVarEnd(String str, int offset) {
        if (offset < str.length() - 1 && isVarNameStart(str.charAt(offset))) {
            int end = offset + 1;
            while (end < str.length()) {
                if (!isVarNamePart(str.charAt(end))) {
                    break;
                }
                end++;
            }
            return end;
        }

        return -1;
    }

    /**
     * 文字列をトークン化します。
     * @param str 入力文字列
     * @return トークンのリスト
     */
    public static List<String> tokenize(String str) {
        return tokenize(str, null);
    }

    /**
     * 変数置換を行いながら文字列をトークン化します。
     * 引用符で囲まれた文字列と変数展開を適切に処理します。
     * @param str 入力文字列
     * @param vars 変数マップ
     * @return トークンのリスト
     */
    public static List<String> tokenize(String str, Map<String, String> vars) {
        if (StringUtilsKt.isBlank(str)) {
            return new ArrayList<>();
        }

        if (vars == null) {
            vars = Collections.emptyMap();
        }

        // 引用符とスペースを適切に処理して文字列を分割
        ArrayList<String> parts = new ArrayList<>();
        int varEnd;

        boolean hasValue = false;
        StringBuilder current = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); ) {
            char c = str.charAt(i);
            if (c == '\'') {
                hasValue = true;
                int end = str.indexOf(c, i + 1);
                if (end < 0) {
                    end = str.length();
                }
                current.append(str, i + 1, end);
                i = end + 1;

            } else if (c == '"') {
                hasValue = true;
                i++;
                while (i < str.length()) {
                    c = str.charAt(i++);
                    if (c == '"') {
                        break;
                    } else if (c == '`' && i < str.length()) {
                        c = str.charAt(i++);
                        switch (c) {
                            case 'a':
                                c = '\u0007';
                                break;
                            case 'b':
                                c = '\b';
                                break;
                            case 'f':
                                c = '\f';
                                break;
                            case 'n':
                                c = '\n';
                                break;
                            case 'r':
                                c = '\r';
                                break;
                            case 't':
                                c = '\t';
                                break;
                            case 'v':
                                c = '\u000b';
                                break;
                        }
                        current.append(c);
                    } else if (c == '$' && (varEnd = findVarEnd(str, i)) >= 0) {
                        String key = str.substring(i, varEnd);
                        String value = vars.get(key);
                        if (value != null) {
                            current.append(value);
                        } else {
                            current.append('$').append(key);
                        }

                        i = varEnd;
                    } else {
                        current.append(c);
                    }
                }
            } else if (c == ' ') {
                if (hasValue) {
                    parts.add(current.toString());
                    current.setLength(0);
                    hasValue = false;
                }
                i++;
            } else if (c == '$' && (varEnd = findVarEnd(str, i + 1)) >= 0) {
                hasValue = true;
                String key = str.substring(i + 1, varEnd);
                String value = vars.get(key);
                if (value != null) {
                    current.append(value);
                } else {
                    current.append('$').append(key);
                }

                i = varEnd;
            } else {
                hasValue = true;
                current.append(c);
                i++;
            }
        }
        if (hasValue) {
            parts.add(current.toString());
        }

        return parts;
    }

    /**
     * 文字列間の最長共通部分列（LCS）を計算するためのクラス。
     */
    public static final class LongestCommonSubsequence {
        // 動的計画法の配列を再利用して割り当てを削減
        private final int[][] f;
        private final int maxLengthA;
        private final int maxLengthB;

        /**
         * 最大長を指定してLCS計算インスタンスを構築します。
         * @param maxLengthA 文字列Aの最大長
         * @param maxLengthB 文字列Bの最大長
         */
        public LongestCommonSubsequence(int maxLengthA, int maxLengthB) {
            this.maxLengthA = maxLengthA;
            this.maxLengthB = maxLengthB;
            f = new int[maxLengthA + 1][];
            for (int i = 0; i <= maxLengthA; i++) {
                f[i] = new int[maxLengthB + 1];
            }
        }

        /**
         * 2つの文字列間の最長共通部分列の長さを計算します。
         * @param a 最初の文字列
         * @param b 2番目の文字列
         * @return LCSの長さ
         */
        public int calc(CharSequence a, CharSequence b) {
            if (a.length() > maxLengthA || b.length() > maxLengthB) {
                throw new IllegalArgumentException("Too large length");
            }
            for (int i = 1; i <= a.length(); i++) {
                for (int j = 1; j <= b.length(); j++) {
                    if (a.charAt(i - 1) == b.charAt(j - 1)) {
                        f[i][j] = 1 + f[i - 1][j - 1];
                    } else {
                        f[i][j] = Math.max(f[i - 1][j], f[i][j - 1]);
                    }
                }
            }
            return f[a.length()][b.length()];
        }
    }
}
