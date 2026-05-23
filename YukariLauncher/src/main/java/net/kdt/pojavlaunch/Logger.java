package net.kdt.pojavlaunch;

import androidx.annotation.Keep;

/**
 * 1つのファイルにログを記録するためのシングルトンクラス。
 * シングルトン部分は削除可能ですが、エンドデベロッパーによる実装が必要になります。
 */
@Keep
public class Logger {
    /**
     * 検閲されていない場合、テキストをログファイルに出力します。
     */
    public static native void appendToLog(String text);

    /**
     * ログファイルをリセットし、以前のログを消去します。
     */
    public static native void begin(String logFilePath);

    /**
     * ログをリッスンするための小さなリスナーインターフェース。
     */
    public interface eventLogListener {
        void onEventLogged(String text);
    }

    /**
     * ログリスナーをロガーにリンクします。
     */
    public static native void setLogListener(eventLogListener logListener);
}
