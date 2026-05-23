package com.arata.yukarilauncher.launch

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.event.single.AccountUpdateEvent
import com.arata.yukarilauncher.feature.accounts.AccountType
import com.arata.yukarilauncher.feature.accounts.AccountUtils
import com.arata.yukarilauncher.feature.accounts.AccountsManager
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.renderer.Renderers
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.support.touch_controller.ControllerProxy
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.LifecycleAwareTipDialog
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.http.NetworkUtils
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.Logger
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.microsoft.PresentedException
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.plugins.FFmpegPlugin
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.services.GameService
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader
import net.kdt.pojavlaunch.tasks.MinecraftDownloader
import net.kdt.pojavlaunch.utils.JREUtils
import net.kdt.pojavlaunch.value.MinecraftAccount
import org.greenrobot.eventbus.EventBus

class LaunchGame {
    companion object {
        /**
         * ゲーム起動前の前処理を行う
         * - ログイン処理とアカウント情報の更新
         * - options.txtファイルをゲームディレクトリにコピー
         * @param context コンテキスト
         * @param version 選択されたバージョン
         */
        @JvmStatic
        fun preLaunch(context: Context, version: Version) {
            val networkAvailable = NetworkUtils.isNetworkAvailable(context)

            fun launch(setOfflineAccount: Boolean = false) {
                version.offlineAccountLogin = setOfflineAccount

                val versionName = version.getVersionName()
                val mcVersion = AsyncMinecraftDownloader.getListedVersion(versionName)
                val listener = ContextAwareDoneListener(context, version)
                // ネットワーク未接続の場合はダウンロードをスキップして直接起動
                if (!networkAvailable) {
                    listener.onDownloadDone()
                } else {
                    MinecraftDownloader().start(mcVersion, versionName, listener)
                }
            }

            fun setGameProgress(pull: Boolean) {
                if (pull) {
                    ProgressKeeper.submitProgress(ProgressLayout.CHECKING_MODS, 0, R.string.mod_check_progress_message, 0, 0, 0)
                    ProgressKeeper.submitProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0, R.string.newdl_downloading_game_files, 0, 0, 0)
                } else {
                    ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT)
                    ProgressLayout.clearProgress(ProgressLayout.CHECKING_MODS)
                }
            }

            if (!networkAvailable) {
                // ネットワーク未接続、ログイン不可だがゲーム起動は許可（同名のオフラインアカウントを作成して起動）
                Toast.makeText(context, context.getString(R.string.account_login_no_network), Toast.LENGTH_SHORT).show()
                launch(true)
                return
            }

            if (AccountUtils.isNoLoginRequired(AccountsManager.currentAccount)) {
                launch()
                return
            }

            AccountsManager.performLogin(
                context, AccountsManager.currentAccount!!,
                { _ ->
                    EventBus.getDefault().post(AccountUpdateEvent())
                    TaskExecutors.runInUIThread {
                        Toast.makeText(context, context.getString(R.string.account_login_done), Toast.LENGTH_SHORT).show()
                    }
                    // ログイン完了、ゲームを正式に起動
                    launch()
                },
                { exception ->
                    val errorMessage = if (exception is PresentedException) exception.toString(context)
                    else exception.message

                    TaskExecutors.runInUIThread {
                        TipDialog.Builder(context)
                            .setTitle(R.string.generic_error)
                            .setMessage("${context.getString(R.string.account_login_skip)}\r\n$errorMessage")
                            .setWarning()
                            .setConfirmClickListener { launch(true) }
                            .setCenterMessage(false)
                            .showDialog()
                    }

                    setGameProgress(false)
                }
            )
            setGameProgress(true)
        }

        /**
         * ゲームを実際に実行する
         * レンダラーの検証、アカウント設定、Javaランタイムの選択、起動情報の表示、ゲームプロセスの開始を行う
         * @param activity アクティビティ
         * @param minecraftVersion 選択されたバージョン
         * @param version バージョン情報
         */
        @Throws(Throwable::class)
        @JvmStatic
        fun runGame(activity: AppCompatActivity, minecraftVersion: Version, version: JMinecraftVersionList.Version) {
            if (!Renderers.isCurrentRendererValid()) {
                Renderers.setCurrentRenderer(activity, AllSettings.renderer.getValue())
            }

            var account = AccountsManager.currentAccount!!
            if (minecraftVersion.offlineAccountLogin) {
                account = MinecraftAccount().apply {
                    this.username = account.username
                    this.accountType = AccountType.LOCAL.type
                }
            }

            val customArgs = minecraftVersion.getJavaArgs().takeIf { it.isNotBlank() } ?: ""

            val javaRuntime = getRuntime(activity, minecraftVersion, version.javaVersion?.majorVersion ?: 8)

            printLauncherInfo(
                minecraftVersion,
                customArgs.takeIf { it.isNotBlank() } ?: "NONE",
                javaRuntime,
                account
            )

            minecraftVersion.modCheckResult?.let { modCheckResult ->
                if (modCheckResult.hasTouchController) {
                    Logger.appendToLog("Mod Perception: TouchController Mod found, attempting to automatically enable control proxy!")
                    ControllerProxy.startProxy(activity)
                    AllStaticSettings.useControllerProxy = true
                }

                if (modCheckResult.hasSodiumOrEmbeddium) {
                    Logger.appendToLog("Mod Perception: Sodium or Embeddium Mod found, attempting to load the disable warning tool later!")
                }
            }

            JREUtils.redirectAndPrintJRELog()

            launch(activity, account, minecraftVersion, javaRuntime, customArgs)

            // 実際には上記の関数でゲームがクラッシュしてもストールするが、念のため
            GameService.setActive(false)
        }

        /**
         * バージョンに適したJavaランタイムを取得する
         * バージョン固有の設定がない場合は、自動的に最適な環境を選択する
         * @param activity アクティビティ
         * @param version バージョン
         * @param targetJavaVersion 必要なJavaバージョン
         * @return ランタイム名
         */
        private fun getRuntime(activity: Activity, version: Version, targetJavaVersion: Int): String {
            val versionRuntime = version.getJavaDir()
                .takeIf { it.isNotEmpty() && it.startsWith(Tools.LAUNCHERPROFILES_RTPREFIX) }
                ?.removePrefix(Tools.LAUNCHERPROFILES_RTPREFIX)
                ?: ""

            if (versionRuntime.isNotEmpty()) return versionRuntime

            // バージョンがJava環境を選択していない場合、自動的に適切な環境を選択
            var runtime = AllSettings.defaultRuntime.getValue()
            val pickedRuntime = MultiRTUtils.read(runtime)
            if (pickedRuntime.javaVersion == 0 || pickedRuntime.javaVersion < targetJavaVersion) {
                runtime = MultiRTUtils.getNearestJreName(targetJavaVersion) ?: run {
                    activity.runOnUiThread {
                        Toast.makeText(activity, activity.getString(R.string.game_autopick_runtime_failed), Toast.LENGTH_SHORT).show()
                    }
                    return runtime
                }
            }
            return runtime
        }

        /**
         * 起動情報をログに出力する
         * @param minecraftVersion Minecraftバージョン
         * @param javaArguments Java引数
         * @param javaRuntime Javaランタイム
         * @param account アカウント情報
         */
        private fun printLauncherInfo(
            minecraftVersion: Version,
            javaArguments: String,
            javaRuntime: String,
            account: MinecraftAccount
        ) {
            var mcInfo = minecraftVersion.getVersionName()
            minecraftVersion.getVersionInfo()?.let { info ->
                mcInfo = info.getInfoString()
            }

            Logger.appendToLog("--------- Start launching the game")
            Logger.appendToLog("Info: Launcher version: ${YLTools.getVersionName()} (${YLTools.getVersionCode()})")
            Logger.appendToLog("Info: Architecture: ${Architecture.archAsString(Tools.DEVICE_ARCHITECTURE)}")
            Logger.appendToLog("Info: Device model: ${StringUtils.insertSpace(Build.MANUFACTURER, Build.MODEL)}")
            Logger.appendToLog("Info: API version: ${Build.VERSION.SDK_INT}")
            Logger.appendToLog("Info: Renderer: ${Renderers.getCurrentRenderer().getRendererName()}")
            Logger.appendToLog("Info: Selected Minecraft version: ${minecraftVersion.getVersionName()}")
            Logger.appendToLog("Info: Minecraft Info: $mcInfo")
            Logger.appendToLog("Info: Game Path: ${minecraftVersion.getGameDir().absolutePath} (Isolation: ${minecraftVersion.isIsolation()})")
            Logger.appendToLog("Info: Custom Java arguments: $javaArguments")
            Logger.appendToLog("Info: Java Runtime: $javaRuntime")
            Logger.appendToLog("Info: Account: ${account.username} (${account.accountType})")
            Logger.appendToLog("---------\r\n")
        }

        /**
         * ゲームプロセスを起動する内部処理
         * メモリチェック、ランタイム解決、クラスパス生成、引数構築を行いJREを起動する
         * @param activity アクティビティ
         * @param account アカウント
         * @param minecraftVersion バージョン
         * @param javaRuntime Javaランタイム
         * @param customArgs カスタム引数
         */
        @Throws(Throwable::class)
        @JvmStatic
        private fun launch(
            activity: AppCompatActivity,
            account: MinecraftAccount,
            minecraftVersion: Version,
            javaRuntime: String,
            customArgs: String
        ) {
            checkMemory(activity)

            val runtime = MultiRTUtils.forceReread(javaRuntime)

            val versionInfo = Tools.getVersionInfo(minecraftVersion)
            val gameDirPath = minecraftVersion.getGameDir()

            // 前処理
            Tools.disableSplash(gameDirPath)
            val launchClassPath = Tools.generateLaunchClassPath(versionInfo, minecraftVersion)

            val launchArgs = LaunchArgs(
                account,
                gameDirPath,
                minecraftVersion,
                versionInfo,
                minecraftVersion.getVersionName(),
                runtime,
                launchClassPath
            ).getAllArgs()

            FFmpegPlugin.discover(activity)

            JREUtils.launchWithUtils(activity, runtime, minecraftVersion, launchArgs, customArgs)
        }

        /**
         * メモリの空き容量をチェックし、不足している場合は警告を表示する
         * @param activity アクティビティ
         */
        private fun checkMemory(activity: AppCompatActivity) {
            var freeDeviceMemory = Tools.getFreeDeviceMemory(activity)
            val freeAddressSpace =
                if (Architecture.is32BitsDevice())
                    Tools.getMaxContinuousAddressSpaceSize()
                else -1
            Logging.i("MemStat",
                "Free RAM: $freeDeviceMemory Addressable: $freeAddressSpace")

            val stringId: Int = if (freeDeviceMemory > freeAddressSpace && freeAddressSpace != -1) {
                freeDeviceMemory = freeAddressSpace
                R.string.address_memory_warning_msg
            } else R.string.memory_warning_msg

            if (AllSettings.ramAllocation.value.getValue() > freeDeviceMemory) {
                val builder = TipDialog.Builder(activity)
                    .setTitle(R.string.generic_warning)
                    .setMessage(activity.getString(stringId, freeDeviceMemory, AllSettings.ramAllocation.value.getValue()))
                    .setWarning()
                    .setCenterMessage(false)
                    .setShowCancel(false)
                if (LifecycleAwareTipDialog.haltOnDialog(activity.lifecycle, builder)) return
                // ダイアログのライフサイクルが終了した場合、ゲームを実際に起動せずに戻る
                // これにより、Activityが再表示された後に開始する機会を与える
            }
        }
    }
}
