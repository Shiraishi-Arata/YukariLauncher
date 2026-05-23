package net.kdt.pojavlaunch.lifecycle;

import static net.kdt.pojavlaunch.MainActivity.INTENT_VERSION;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.kdt.mcgui.ProgressLayout;
import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.context.ContextExecutor;
import com.arata.yukarilauncher.feature.mod.parser.ModChecker;
import com.arata.yukarilauncher.feature.mod.parser.ModInfo;
import com.arata.yukarilauncher.feature.mod.parser.ModParser;
import com.arata.yukarilauncher.feature.mod.parser.ModParserListener;
import com.arata.yukarilauncher.feature.version.Version;
import com.arata.yukarilauncher.setting.AllSettings;

import net.kdt.pojavlaunch.MainActivity;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.utils.NotificationUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * コンテキストに応じたダウンロード完了リスナー。アクティビティが利用可能な場合は直接ゲームを起動し、
 * 利用できない場合は通知を送信します。
 */
public class ContextAwareDoneListener implements AsyncMinecraftDownloader.DoneListener, ContextExecutorTask {
    private final String mErrorString;
    private final Version mVersion;

    /**
     * コンストラクタ。
     * @param baseContext 基本コンテキスト（エラーメッセージの取得に使用）
     * @param version ゲームバージョン
     */
    public ContextAwareDoneListener(Context baseContext, Version version) {
        this.mErrorString = baseContext.getString(R.string.mc_download_failed);
        this.mVersion = version;
    }

    /**
     * ゲーム起動インテントを作成します。
     */
    private Intent createGameStartIntent(Context context) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra(INTENT_VERSION, mVersion);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return mainIntent;
    }

    /**
     * タスクが完了するのを待ってから、コンテキストに応じた処理を実行します。
     */
    private void executeTask() {
        ProgressKeeper.waitUntilDone(() -> ContextExecutor.executeTask(this));
    }

    /**
     * ダウンロード完了時にModをチェックし、その後タスクを実行します。
     */
    @Override
    public void onDownloadDone() {
        AtomicInteger progressCount = new AtomicInteger(0);
        ModParser.checkAllMods(mVersion, new ModParserListener() {
            @Override
            public void onProgress(@NonNull ModInfo recentlyParsedModInfo, int totalFileCount) {
                int i = progressCount.incrementAndGet();
                ProgressLayout.setProgress(ProgressLayout.CHECKING_MODS, i * 100 / totalFileCount,
                        R.string.mod_check_progress_message, i, totalFileCount);
            }

            @Override
            public void onParseEnded(@NonNull List<? extends ModInfo> modInfoList) {
                ProgressLayout.clearProgress(ProgressLayout.CHECKING_MODS);
                if (modInfoList.isEmpty()) executeTask();
                else {
                    ContextExecutor.executeTaskWithAllContext(context -> new ModChecker().check(context, modInfoList, modCheckResult -> {
                        mVersion.setModCheckResult(modCheckResult);
                        executeTask();
                        return null;
                    }));
                }
            }
        });
    }

    /**
     * ダウンロード失敗時にエラーを表示します。
     */
    @Override
    public void onDownloadFailed(Throwable throwable) {
        Tools.showErrorRemote(mErrorString, throwable);
    }

    /**
     * アクティビティが利用可能な場合、ゲームを直接起動します。
     */
    @Override
    public void executeWithActivity(Activity activity) {
        try {
            Intent gameStartIntent = createGameStartIntent(activity);
            activity.startActivity(gameStartIntent);
            if (AllSettings.getQuitLauncher().getValue()) {
                activity.finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        } catch (Throwable e) {
            Tools.showError(activity.getBaseContext(), e);
        }
    }

    /**
     * アクティビティが利用できない場合、通知でゲーム起動を知らせます。
     */
    @Override
    public void executeWithApplication(Context context) {
        Intent gameStartIntent = createGameStartIntent(context);
        NotificationUtils.sendBasicNotification(context,
                R.string.notif_download_finished,
                R.string.notif_download_finished_desc,
                gameStartIntent,
                NotificationUtils.PENDINGINTENT_CODE_GAME_START,
                NotificationUtils.NOTIFICATION_ID_GAME_START
        );
    }
}
