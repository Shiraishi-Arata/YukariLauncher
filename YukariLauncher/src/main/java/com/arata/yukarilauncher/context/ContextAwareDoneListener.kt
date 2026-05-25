package com.arata.yukarilauncher.context

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.login.PresentedException
import com.arata.yukarilauncher.feature.mod.parser.ModChecker
import com.arata.yukarilauncher.feature.mod.parser.ModInfo
import com.arata.yukarilauncher.feature.mod.parser.ModParser
import com.arata.yukarilauncher.feature.mod.parser.ModParserListener
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.task.AsyncMinecraftDownloader
import com.arata.yukarilauncher.task.ProgressKeeper
import com.arata.yukarilauncher.ui.activity.MainActivity
import com.arata.yukarilauncher.ui.activity.MainActivity.Companion.INTENT_VERSION
import com.arata.yukarilauncher.utils.NotificationUtils
import java.util.concurrent.atomic.AtomicInteger

/** コンテキストを認識するダウンロード完了リスナー。ゲーム起動や通知を処理する。 */
class ContextAwareDoneListener(baseContext: Context, private val mVersion: Version) : AsyncMinecraftDownloader.DoneListener, ContextExecutorTask {
    /** エラーメッセージ */
    private val mErrorString: String = baseContext.getString(R.string.mc_download_failed)

    /** ゲーム起動Intentを作成する。 @param context コンテキスト @return ゲーム起動Intent */
    private fun createGameStartIntent(context: Context): Intent {
        val mainIntent = Intent(context, MainActivity::class.java)
        mainIntent.putExtra(INTENT_VERSION, mVersion)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return mainIntent
    }

    /** タスクを実行する。 */
    private fun executeTask() {
        ProgressKeeper.waitUntilDone { ContextExecutor.executeTask(this) }
    }

    /** ダウンロード完了時の処理。Modのチェックを実施する。 */
    override fun onDownloadDone() {
        val progressCount = AtomicInteger(0)
        ModParser.checkAllMods(mVersion, object : ModParserListener {
            override fun onProgress(@NonNull recentlyParsedModInfo: ModInfo, totalFileCount: Int) {
                val i = progressCount.incrementAndGet()
                ProgressLayout.setProgress(ProgressLayout.CHECKING_MODS, i * 100 / totalFileCount,
                    R.string.mod_check_progress_message, i, totalFileCount)
            }

            override fun onParseEnded(@NonNull modInfoList: List<out ModInfo>) {
                ProgressLayout.clearProgress(ProgressLayout.CHECKING_MODS)
                if (modInfoList.isEmpty()) executeTask()
                else {
                    ContextExecutor.executeTaskWithAllContext { context ->
                        ModChecker().check(context, modInfoList) { modCheckResult ->
                            mVersion.modCheckResult = modCheckResult
                            executeTask()
                            null
                        }
                    }
                }
            }
        })
    }

    /** ダウンロード失敗時の処理。 @param throwable 発生した例外 */
    override fun onDownloadFailed(throwable: Throwable) {
        Tools.showErrorRemote(mErrorString, throwable)
    }

    /** Activityでタスクを実行する。 @param activity アクティビティ */
    override fun executeWithActivity(activity: Activity) {
        try {
            val gameStartIntent = createGameStartIntent(activity)
            activity.startActivity(gameStartIntent)
            if (AllSettings.quitLauncher.getValue()) {
                activity.finish()
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        } catch (e: Throwable) {
            Tools.showError(activity.baseContext, e)
        }
    }

    /** アプリケーションコンテキストでタスクを実行する。 @param context コンテキスト */
    override fun executeWithApplication(context: Context) {
        val gameStartIntent = createGameStartIntent(context)
        NotificationUtils.sendBasicNotification(context,
            R.string.notif_download_finished,
            R.string.notif_download_finished_desc,
            gameStartIntent,
            NotificationUtils.PENDINGINTENT_CODE_GAME_START,
            NotificationUtils.NOTIFICATION_ID_GAME_START
        )
    }
}
