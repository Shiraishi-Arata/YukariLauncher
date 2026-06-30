package com.arata.yukarilauncher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.arata.yukarilauncher.InfoDistributor
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.context.LocaleHelper
import com.arata.yukarilauncher.feature.discord.DiscordRpcManager
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.ui.activity.ErrorActivity
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.plugins.PluginLoader
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.file.FileUtils
import com.arata.yukarilauncher.utils.platform.Architecture
import java.io.File
import java.io.PrintStream
import java.text.DateFormat
import java.util.Date

/**
 * アプリケーションのメインエントリポイントとなるApplicationクラス。
 * アプリケーションの初期化処理、クラッシュレポートの管理、
 * ロケール設定、アーキテクチャ検出などを担当します。
 */
class PojavApplication : Application() {
    companion object {
        /** クラッシュレポートのタグ名。 */
        const val CRASH_REPORT_TAG = "YukariCrashReport"
        /** Discord RPC更新用のブロードキャストアクション。 */
        const val ACTION_RPC_UPDATE = "com.arata.yukarilauncher.action.RPC_UPDATE"
    }

    /**
     * アプリケーション作成時に呼び出されます。
     * 各種初期化処理、クラッシュハンドラの設定、
     * ストレージパスやアーキテクチャ情報の初期化を行います。
     */
    override fun onCreate() {
        ContextExecutor.setApplication(this)

        Thread.setDefaultUncaughtExceptionHandler { thread, th ->
            val storagePermAllowed = (Build.VERSION.SDK_INT >= 29 || ActivityCompat.checkSelfPermission(
                this@PojavApplication,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) && Tools.checkStorageRoot()
            val crashFile = File(
                if (storagePermAllowed) PathManager.DIR_LAUNCHER_LOG else PathManager.DIR_DATA,
                "latestcrash.txt"
            )
            try {
                FileUtils.ensureParentDirectory(crashFile)
                PrintStream(crashFile).use { crashStream ->
                    crashStream.append(InfoDistributor.APP_NAME + " crash report\n")
                    crashStream.append(" - Time: ").append(DateFormat.getDateTimeInstance().format(Date())).append("\n")
                    crashStream.append(" - Device: ").append(Build.PRODUCT).append(" ").append(Build.MODEL).append("\n")
                    crashStream.append(" - Android version: ").append(Build.VERSION.RELEASE).append("\n")
                    crashStream.append(" - Launcher version: ").append(YLTools.getVersionName()).append(" (").append(YLTools.getVersionCode().toString()).append(")").append("\n")
                    crashStream.append(" - Crash stack trace:\n")
                    crashStream.append(android.util.Log.getStackTraceString(th))
                }
            } catch (throwable: Throwable) {
                Logging.e(CRASH_REPORT_TAG, " - Exception attempt saving crash stack trace:", throwable)
                Logging.e(CRASH_REPORT_TAG, " - The crash stack trace was:", th)
            }

            ErrorActivity.showLauncherCrash(this@PojavApplication, crashFile.absolutePath, th)
            YLTools.killProcess()
        }

        try {
            super.onCreate()
            PathManager.DIR_DATA = getDir("files", MODE_PRIVATE).parent
            PathManager.DIR_CACHE = cacheDir
            PathManager.DIR_ACCOUNT_NEW = PathManager.DIR_DATA + "/accounts"
            Tools.DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture()
            if (Architecture.isx86Device() && Architecture.is32BitsDevice()) {
                val originalJNIDirectory = applicationInfo.nativeLibraryDir
                applicationInfo.nativeLibraryDir = originalJNIDirectory.substring(
                    0,
                    originalJNIDirectory.lastIndexOf("/")
                ) + "/x86"
            }
        } catch (throwable: Throwable) {
            val ferrorIntent = Intent(this, ErrorActivity::class.java)
            ferrorIntent.putExtra("throwable", throwable)
            ferrorIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(ferrorIntent)
        }

        PluginLoader.loadAllPlugins(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Discord RPC更新用のブロードキャストレシーバーを登録
        // RECEIVER_NOT_EXPORTED: 同一アプリ内からのみ受信可能（Android 14+の暗黙的ブロードキャスト制限に対応）
        val filter = IntentFilter(ACTION_RPC_UPDATE)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(rpcReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(rpcReceiver, filter)
        }
    }

    /**
     * Discord RPC更新用のブロードキャストレシーバー。
     * :gameプロセスから送信されたRPC更新Intentを受信し、
     * :launcherプロセス（WebSocketが存在するプロセス）でRPCを更新します。
     */
    private val rpcReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val command = intent.getStringExtra("command")
            if (command == "update_launcher") {
                Logging.i("DiscordRPC", "PojavApplication: broadcast received, updating RPC to launcher presence")
                DiscordRpcManager.updateLauncherPresence()
                // quitLauncher=trueの場合、RPC更新後に300ms待機してランチャープロセスを終了
                if (intent.getBooleanExtra("quitLauncher", false)) {
                    try { Thread.sleep(300) } catch (_: InterruptedException) {}
                    Logging.i("DiscordRPC", "PojavApplication: quitLauncher=true, killing process")
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            }
        }
    }

    /**
     * アプリケーション終了時に呼び出されます。
     * ContextExecutorのクリーンアップとブロードキャストレシーバーの登録解除を行います。
     */
    override fun onTerminate() {
        super.onTerminate()
        ContextExecutor.clearApplication()
        try { unregisterReceiver(rpcReceiver) } catch (_: Exception) {}
    }

    /**
     * ベースコンテキストがアタッチされるときに呼び出されます。
     * ロケール設定を適用したコンテキストを使用します。
     * @param base ベースとなるコンテキスト
     */
    override fun attachBaseContext(base: android.content.Context) {
        ContextExecutor.setApplication(this)
        super.attachBaseContext(LocaleHelper.setLocale(base))
    }

    /**
     * 画面の設定が変更されたときに呼び出されます。
     * ロケール設定を再適用します。
     * @param newConfig 新しい設定情報
     */
    override fun onConfigurationChanged(@NonNull newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ContextExecutor.setApplication(this)
        LocaleHelper.setLocale(this)
    }
}
