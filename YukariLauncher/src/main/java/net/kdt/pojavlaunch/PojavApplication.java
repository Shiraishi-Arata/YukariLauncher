package net.kdt.pojavlaunch;

import android.app.Application;
import android.content.Context;

import com.arata.yukarilauncher.R;

import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;
import net.kdt.pojavlaunch.utils.NotificationUtils;

/**
 * Pojavアプリケーションのエントリポイント。アプリケーションの初期化を担当します。
 */
public class PojavApplication extends Application {

    /**
     * アプリケーション起動時の初期化を行います。
     * コントローラの設定、コンテキスト管理、通知チャンネルなどをセットアップします。
     */
    @Override
    public void onCreate() {
        super.onCreate();

        Context ctx = getApplicationContext();

        // コントローラの設定を初期化
        try {
            net.kdt.pojavlaunch.Tools.copyFromAssets(ctx.getAssets(), "controlmap.json", ctx.getFilesDir().getAbsolutePath());
        } catch (Exception e) {
            // ファイルが存在しない場合は無視
        }

        // コンテキストマネージャ、進捗状況、通知を初期化
        ContextExecutor.init();
        ProgressKeeper.init(new ProgressServiceKeeper(ctx));
        NotificationUtils.initNotificationChannel(ctx);

        // 言語設定を適用
        if (BuildConfig.SETTINGS_LANGUAGE != null && !BuildConfig.SETTINGS_LANGUAGE.isEmpty()) {
            setLanguage(ctx, BuildConfig.SETTINGS_LANGUAGE);
        }
    }

    /**
     * アプリケーションの言語設定を変更します。
     */
    private void setLanguage(Context ctx, String language) {
        android.content.res.Configuration config = new android.content.res.Configuration();
        java.util.Locale locale = new java.util.Locale(language);
        android.os.LocaleList localeList = new android.os.LocaleList(locale);
        config.setLocales(localeList);
        ctx.getResources().updateConfiguration(config, ctx.getResources().getDisplayMetrics());
    }
}
