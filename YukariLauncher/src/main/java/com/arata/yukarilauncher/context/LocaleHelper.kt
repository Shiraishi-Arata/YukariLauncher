package com.arata.yukarilauncher.context

import android.content.Context
import android.content.ContextWrapper
import com.arata.yukarilauncher.setting.Settings
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.setting.LauncherPreferences

class LocaleHelper(context: Context) : ContextWrapper(context) {
    companion object {
        /**
         * ロケールを設定し、必要な初期化処理を実行する
         * パスの初期化と設定のリフレッシュを行い、ラッパーを返す
         * @param context アプリケーションコンテキスト
         * @return ロケール設定済みのContextWrapper
         */
        fun setLocale(context: Context): ContextWrapper {
            PathManager.initContextConstants(context)
            Tools.initStorageConstants(context)
            Settings.refreshSettings()

            LauncherPreferences.loadPreferences()
            return LocaleHelper(context)
        }
    }
}
