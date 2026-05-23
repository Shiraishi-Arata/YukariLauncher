package net.kdt.pojavlaunch.plugins;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.arata.yukarilauncher.feature.log.Logging;

import java.io.File;

/**
 * FFmpegプラグインを検出して管理するクラス
 */
public class FFmpegPlugin {
    public static boolean isAvailable = false;
    public static String libraryPath;
    public static String executablePath;

    /**
     * FFmpegプラグインがインストールされているかどうかを検出します。
     * パッケージマネージャーからプラグイン情報を取得し、
     * ライブラリパスと実行可能パスを設定します。
     * @param context アプリケーションコンテキスト
     */
    public static void discover(Context context) {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo ffmpegPluginInfo = manager.getPackageInfo("net.kdt.pojavlaunch.ffmpeg", PackageManager.GET_SHARED_LIBRARY_FILES);
            libraryPath = ffmpegPluginInfo.applicationInfo.nativeLibraryDir;
            File ffmpegExecutable = new File(libraryPath, "libffmpeg.so");
            executablePath = ffmpegExecutable.getAbsolutePath();
            // 古いプラグインバージョンにはまだ古い実行可能ファイルの場所がある
            isAvailable = ffmpegExecutable.exists();
        }catch (Exception e) {
            Logging.i("FFmpegPlugin", "Failed to discover plugin", e);
        }
    }
}
