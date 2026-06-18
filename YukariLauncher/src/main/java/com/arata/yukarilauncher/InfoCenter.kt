package com.arata.yukarilauncher

import android.content.Context
import com.arata.yukarilauncher.InfoDistributor.APP_NAME

/**
 * アプリケーションの情報センターとなるクラス。
 * QQグループの連絡先やリソース文字列の置換など、
 * アプリケーションに関する基本的な情報提供機能を提供します。
 */
class InfoCenter {
    companion object {
        /** QQグループのID。 */
        const val QQ_GROUP: String = "435667089"

        /**
         * リソース文字列中のアプリ名を実際のアプリ名で置換します。
         * @param context コンテキスト
         * @param resString 置換対象の文字列リソースID
         * @return 置換後の文字列
         */
        @JvmStatic
        fun replaceName(context: Context, resString: Int): String = context.getString(resString, APP_NAME)
    }
}