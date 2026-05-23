package net.kdt.pojavlaunch;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.arata.yukarilauncher.utils.YLTools.getVersionCode;
import static com.arata.yukarilauncher.utils.YLTools.getVersionName;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.arata.yukarilauncher.InfoDistributor;
import com.arata.yukarilauncher.context.ContextExecutor;
import com.arata.yukarilauncher.context.LocaleHelper;
import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.ui.activity.ErrorActivity;
import com.arata.yukarilauncher.utils.path.PathManager;
import com.arata.yukarilauncher.utils.YLTools;

import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

/**
 * アプリケーションクラス。アプリケーションのライフサイクルを管理し、クラッシュレポートのハンドリングや初期設定を行います。
 */
public class PojavApplication extends Application {
	public static final String CRASH_REPORT_TAG = "YukariCrashReport";

	/**
	 * アプリケーションが作成されたときに呼ばれます。
	 * 未捕捉例外のハンドラを設定し、パスマネージャーを初期化します。
	 */
	@Override
	public void onCreate() {
		ContextExecutor.setApplication(this);

		// 未捕捉例外のハンドラを設定し、クラッシュレポートをファイルに保存します
		Thread.setDefaultUncaughtExceptionHandler((thread, th) -> {
			boolean storagePermAllowed = (Build.VERSION.SDK_INT >= 29 || ActivityCompat.checkSelfPermission(PojavApplication.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) && Tools.checkStorageRoot();
			File crashFile = new File(storagePermAllowed ? PathManager.DIR_LAUNCHER_LOG : PathManager.DIR_DATA, "latestcrash.txt");
			try {
				// 一部のデバイスではエラーを表示できないため、ファイルに書き込みます
				FileUtils.ensureParentDirectory(crashFile);
				PrintStream crashStream = new PrintStream(crashFile);
				crashStream.append(InfoDistributor.APP_NAME + " crash report\n");
				crashStream.append(" - Time: ").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n");
				crashStream.append(" - Device: ").append(Build.PRODUCT).append(" ").append(Build.MODEL).append("\n");
				crashStream.append(" - Android version: ").append(Build.VERSION.RELEASE).append("\n");
				crashStream.append(" - Launcher version: ").append(getVersionName()).append(" (").append(String.valueOf(getVersionCode())).append(")").append("\n");
				crashStream.append(" - Crash stack trace:\n");
				crashStream.append(Log.getStackTraceString(th));
				crashStream.close();
			} catch (Throwable throwable) {
				Logging.e(CRASH_REPORT_TAG, " - Exception attempt saving crash stack trace:", throwable);
				Logging.e(CRASH_REPORT_TAG, " - The crash stack trace was:", th);
			}

			ErrorActivity.showLauncherCrash(PojavApplication.this, crashFile.getAbsolutePath(), th);
			YLTools.killProcess();
		});
		
		try {
			super.onCreate();
			// パスマネージャーのディレクトリを初期化します
			PathManager.DIR_DATA = getDir("files", MODE_PRIVATE).getParent();
			PathManager.DIR_CACHE = getCacheDir();
			PathManager.DIR_ACCOUNT_NEW = PathManager.DIR_DATA + "/accounts";
			Tools.DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture();
			// Asus x86ベースのZenfone用にx86ライブラリディレクトリを強制します
			if(Architecture.isx86Device() && Architecture.is32BitsDevice()){
				String originalJNIDirectory = getApplicationInfo().nativeLibraryDir;
				getApplicationInfo().nativeLibraryDir = originalJNIDirectory.substring(0,
												originalJNIDirectory.lastIndexOf("/"))
												.concat("/x86");
			}
		} catch (Throwable throwable) {
			Intent ferrorIntent = new Intent(this, ErrorActivity.class);
			ferrorIntent.putExtra("throwable", throwable);
			ferrorIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
			startActivity(ferrorIntent);
		}

		// ランチャーUIにダークモードを強制します
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
	}

	/**
	 * アプリケーションが終了するときに呼ばれます。
	 * アプリケーションコンテキストをクリアします。
	 */
	@Override
	public void onTerminate() {
		super.onTerminate();
		ContextExecutor.clearApplication();
	}

	/**
	 * ベースコンテキストがアタッチされたときに呼ばれます。
	 * アプリケーションコンテキストを設定し、ロケールを適用します。
	 */
	@Override
    protected void attachBaseContext(Context base) {
		ContextExecutor.setApplication(this);
        super.attachBaseContext(LocaleHelper.Companion.setLocale(base));
    }

    /**
     * 設定が変更されたときに呼ばれます（画面の向き変更など）。
     * アプリケーションコンテキストとロケールを更新します。
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
		ContextExecutor.setApplication(this);
		LocaleHelper.Companion.setLocale(this);
    }
}
