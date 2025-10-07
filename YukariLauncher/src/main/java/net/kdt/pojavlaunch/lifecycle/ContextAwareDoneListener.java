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

public class ContextAwareDoneListener implements AsyncMinecraftDownloader.DoneListener, ContextExecutorTask {
    private final String mErrorString;
    private final Version mVersion;

    public ContextAwareDoneListener(Context baseContext, Version version) {
        this.mErrorString = baseContext.getString(R.string.mc_download_failed);
        this.mVersion = version;
    }

    private Intent createGameStartIntent(Context context) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra(INTENT_VERSION, mVersion);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return mainIntent;
    }

    private void executeTask() {
        ProgressKeeper.waitUntilDone(() -> ContextExecutor.executeTask(this));
    }

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

    @Override
    public void onDownloadFailed(Throwable throwable) {
        Tools.showErrorRemote(mErrorString, throwable);
    }

    @Override
    public void executeWithActivity(Activity activity) {
        try {
            Intent gameStartIntent = createGameStartIntent(activity);
            activity.startActivity(gameStartIntent);
            if (AllSettings.getQuitLauncher().getValue()) {
                activity.finish();
                android.os.Process.killProcess(android.os.Process.myPid()); //You should kill yourself, NOW!
            }
        } catch (Throwable e) {
            Tools.showError(activity.getBaseContext(), e);
        }
    }

    @Override
    public void executeWithApplication(Context context) {
        Intent gameStartIntent = createGameStartIntent(context);
        // Since the game is a separate process anyway, it does not matter if it gets invoked
        // from somewhere other than the launcher activity.
        // The only problem may arise if the launcher starts doing something when the user starts the notification.
        // So, the notification is automatically removed once there are tasks ongoing in the ProgressKeeper
        NotificationUtils.sendBasicNotification(context,
                R.string.notif_download_finished,
                R.string.notif_download_finished_desc,
                gameStartIntent,
                NotificationUtils.PENDINGINTENT_CODE_GAME_START,
                NotificationUtils.NOTIFICATION_ID_GAME_START
        );
        // You should keep yourself safe, NOW!
        // otherwise android does weird things...
    }
}
