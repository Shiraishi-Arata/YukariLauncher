package com.arata.yukarilauncher.feature.log

import androidx.annotation.Keep

/**
 * ネイティブログ出力機能を提供するシングルトンオブジェクト。
 * JNIを介してC++側のログシステムと連携し、ログファイルへの書き込みや
 * ログイベントのリスナー通知を行います。
 */
@Keep
object Logger {
    @JvmStatic var enabled = true

    @JvmStatic external fun appendToLog(text: String)
    @JvmStatic external fun begin(logFilePath: String)
    @JvmStatic external fun setLogListener(logListener: eventLogListener?)

    fun interface eventLogListener {
        fun onEventLogged(text: String)
    }
}