package com.arata.yukarilauncher.feature.log

import androidx.annotation.Keep

/**
 * ネイティブログ出力機能を提供するシングルトンオブジェクト。
 * JNIを介してC++側のログシステムと連携し、ログファイルへの書き込みや
 * ログイベントのリスナー通知を行います。
 */
@Keep
object Logger {
    /**
     * 指定されたテキストをログに追記します。
     * @param text 追記するログテキスト
     */
    @JvmStatic external fun appendToLog(text: String)
    @JvmStatic external fun begin(logFilePath: String)
    @JvmStatic external fun setLogListener(logListener: eventLogListener?)

    /**
     * ログイベントを受け取るための関数型インターフェース。
     */
    fun interface eventLogListener {
        /**
         * ログが記録されたときに呼び出されます。
         * @param text 記録されたログテキスト
         */
        fun onEventLogged(text: String)
    }
}