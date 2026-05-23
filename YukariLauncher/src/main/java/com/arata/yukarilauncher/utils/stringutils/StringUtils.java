package com.arata.yukarilauncher.utils.stringutils;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Base64;
import android.widget.Toast;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.task.TaskExecutors;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    /**
     * Object型の引数を文字列に変換し、間にスペースを挿入する
     */
    public static String insertSpace(Object prefixString, Object... suffixString) {
        return insertSpace(prefixString == null ? null : prefixString.toString(),
                Arrays.stream(suffixString).map(Object::toString).toArray(String[]::new));
    }

    /**
     * 文字列の間にスペースを挿入する
     * @param prefixString 最初の文字列
     * @param suffixString それ以降の複数の文字列
     * @return スペースが挿入された文字列 "string1 string2 string3"
     */
    public static String insertSpace(String prefixString, String... suffixString) {
        return insertString(" ", prefixString, suffixString);
    }

    /**
     * Object型の引数を文字列に変換し、間に改行を挿入する
     */
    public static String insertNewline(Object prefixString, Object... suffixString) {
        return insertNewline(prefixString == null ? null : prefixString.toString(),
                Arrays.stream(suffixString).map(Object::toString).toArray(String[]::new));
    }

    /**
     * 文字列の間に改行を挿入する
     * @param prefixString 最初の文字列
     * @param suffixString それ以降の複数の文字列
     * @return 改行が挿入された文字列
     */
    public static String insertNewline(String prefixString, String... suffixString) {
        return insertString("\r\n", prefixString, suffixString);
    }

    /**
     * 文字列の間に指定された区切り文字を挿入する
     */
    public static String insertString(String stringToInsert, String prefixString, String... suffixString) {
        StringJoiner stringJoiner = new StringJoiner(stringToInsert);
        if (prefixString != null) {
            stringJoiner.add(prefixString);
        }
        for (String string : suffixString) {
            stringJoiner.add(string);
        }

        return stringJoiner.toString();
    }

    /**
     * 文字列を指定した方向と数だけシフト（ローテーション）する
     * シフト数は文字列の長さで割った余りに調整される
     */
    public static String shiftString(String input, ShiftDirection direction, int shiftCount) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // シフト数が文字列長の範囲内に収まるように調整する
        int length = input.length();
        shiftCount = shiftCount % length;
        if (shiftCount == 0) {
            return input;
        }

        switch (direction) {
            case LEFT:
                return input.substring(shiftCount) + input.substring(0, shiftCount);
            case RIGHT:
                return input.substring(length - shiftCount) + input.substring(0, length - shiftCount);
            default:
                throw new IllegalArgumentException("Invalid shift direction: " + direction);
        }
    }

    /**
     * @return 文字列がnullの場合は空文字列を、それ以外は元の文字列を返す
     */
    public static String getStringNotNull(String string) {
        if (string == null) return "";
        else return string;
    }

    /**
     * 文字列に中国語（中国語の句読点を含む）が含まれているかどうかをチェックする
     * @param str チェックする文字列
     * @return 中国語が含まれているかどうか
     */
    public static boolean containsChinese(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        Pattern pattern = Pattern.compile("[一-龥|！，。（）《》“”？：；【】]");
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

    /**
     * ISO 8601形式の時刻文字列をフォーマットする（TとZを除去してスペース区切りにする）
     */
    public static String formattingTime(String time) {
        int T = time.indexOf('T');
        int Z = time.indexOf('Z');
        if (T == -1 || Z == -1) return time;
        return StringUtils.insertSpace(time.substring(0, T), time.substring(T + 1, Z));
    }

    /**
     * 日付を指定されたロケールとタイムゾーンでフォーマットする
     */
    public static String formatDate(Date date, Locale locale, TimeZone timeZone) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale);
        formatter.setTimeZone(timeZone);
        return formatter.format(date);
    }

    /**
     * Markdown形式の文字列をHTMLに変換する
     */
    public static String markdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

    /**
     * テキストをクリップボードにコピーし、トースト通知を表示する
     */
    public static void copyText(String label, String text, Context context) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text));
        TaskExecutors.runInUIThread(() -> Toast.makeText(context, context.getString(R.string.generic_copied), Toast.LENGTH_SHORT).show());
    }

    /**
     * Base64エンコードされた文字列をデコードする
     */
    public static String decodeBase64(String rawValue) {
        byte[] decodedBytes = Base64.decode(rawValue, Base64.DEFAULT);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}
