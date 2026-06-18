package com.arata.yukarilauncher.feature.version.install

import android.app.Activity
import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.event.value.InstallGameEvent
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.VersionsManager
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.task.ProgressKeeper
import com.arata.yukarilauncher.task.AsyncMinecraftDownloader
import com.arata.yukarilauncher.task.MinecraftDownloader
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Minecraftゲームのインストールを管理するクラス
 * バニラのダウンロード後、ModやModLoaderのインストールを順次実行する
 * @param activity 現在のActivity
 * @param installEvent インストールイベント
 */
class GameInstaller(
    private val activity: Activity,
    installEvent: InstallGameEvent
) {
    private val realVersion: String = installEvent.minecraftVersion
    private val customVersionName: String = installEvent.customVersionName
    private val taskMap: Map<Addon, InstallTaskItem> = installEvent.taskMap
    private val targetVersionFolder = VersionsManager.getVersionPath(customVersionName)
    private val vanillaVersionFolder = VersionsManager.getVersionPath(realVersion)

    /**
     * ゲームのインストールを実行する
     * Minecraftのダウンロード後、ModやModLoaderのインストールタスクを順次実行する
     */
    fun installGame() {
        Logging.i("Minecraft Downloader", "Start downloading the version: $realVersion")

        if (taskMap.isNotEmpty()) {
            ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.download_install_download_file, 0, 0, 0)
        }

        val mcVersion = AsyncMinecraftDownloader.getListedVersion(realVersion)
        MinecraftDownloader().start(
            mcVersion,
            realVersion,
            object : AsyncMinecraftDownloader.DoneListener {
                /**
                 * Minecraftのダウンロードが完了した時の処理
                 * バニラファイルのコピー、Mod/ModLoaderのインストールを実行する
                 */
                override fun onDownloadDone() {
                    Task.runTask {
                        if (taskMap.isEmpty()) {
                            // アドオンが空の場合、バニラのみのインストールを示す
                            // カスタムバージョンフォルダにバニラの.jsonファイルが確実に存在するようにする
                            // カスタムバージョン名が指定されているか確認する
                            // 実バージョンとカスタム名が同じ場合は、ユーザーが名前を変更していないことを意味し、
                            // 現在インストールされているのは純粋なバニラである
                            // カスタム名がない場合はバージョンファイルをコピーしない
                            if (realVersion != customVersionName && VersionsManager.isVersionExists(realVersion)) {
                                // バニラの.jsonファイルを探す。MinecraftDownloader開始時に既にダウンロード済み
                                val vanillaJsonFile = File(vanillaVersionFolder, "${vanillaVersionFolder.name}.json")
                                if (vanillaJsonFile.exists() && vanillaJsonFile.isFile) {
                                    // バニラの.jsonファイルが存在する場合、直接コピーする
                                    FileUtils.copyFile(vanillaJsonFile, File(targetVersionFolder, "$customVersionName.json"))
                                }
                            }
                            // ModLoaderタスクが空の場合、以降の無意味なModLoaderタスクは完全にスキップする
                            return@runTask null
                        }

                        // ModとModLoaderのタスクを分離する。Modを先にインストールする
                        val modTask: MutableList<InstallTaskItem> = ArrayList()
                        val modloaderTask = AtomicReference<Pair<Addon, InstallTaskItem>>() // 一時的に1つのModLoaderのみ許可
                        taskMap.forEach { (addon, taskItem) ->
                            if (taskItem.isMod) modTask.add(taskItem)
                            else modloaderTask.set(Pair(addon, taskItem))
                        }

                        // Modファイルのダウンロード
                        modTask.forEach { task ->
                            Logging.i("Install Version", "Installing Mod: ${task.selectedVersion}")
                            val file = task.task.run(customVersionName)
                            val endTask = task.endTask
                            file?.let { endTask?.endTask(activity, it) }
                        }

                        modloaderTask.get()?.let { taskPair ->
                            ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.mod_download_progress, taskPair.first.addonName)

                            Logging.i("Install Version", "Installing ModLoader: ${taskPair.second.selectedVersion}")
                            val file = taskPair.second.task.run(customVersionName)
                            return@runTask Pair(file, taskPair.second)
                        }

                        null
                    }.ended ended@{ taskPair ->
                        taskPair?.let { pair ->
                            pair.first?.let {
                                pair.second.endTask?.endTask(activity, it)
                            }
                        }
                    }.onThrowable { e ->
                        Tools.showErrorRemote(e)
                    }.execute()
                }

                /**
                 * Minecraftのダウンロードが失敗した時の処理
                 * @param throwable 発生した例外
                 */
                override fun onDownloadFailed(throwable: Throwable) {
                    Tools.showErrorRemote(throwable)
                    if (taskMap.isNotEmpty()) {
                        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
                    }
                }
            }
        )
    }
}