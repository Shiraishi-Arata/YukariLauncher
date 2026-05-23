package com.arata.yukarilauncher

import android.content.Context
import com.arata.yukarilauncher.InfoDistributor.APP_NAME

class InfoCenter {
    companion object {
        const val QQ_GROUP: String = "435667089"

        /**
         * リソース文字列中のアプリ名を実際のアプリ名で置換する。
         * @param context コンテキスト
         * @param resString 置換対象の文字列リソースID
         * @return 置換後の文字列
         */
        @JvmStatic
        fun replaceName(context: Context, resString: Int): String = context.getString(resString, APP_NAME)
    }
}
