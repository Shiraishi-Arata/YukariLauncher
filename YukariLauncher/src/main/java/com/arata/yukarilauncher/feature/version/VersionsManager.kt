package com.arata.yukarilauncher.feature.version

import android.content.Context
import com.arata.yukarilauncher.InfoDistributor
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.event.single.RefreshVersionsEvent
import com.arata.yukarilauncher.event.single.RefreshVersionsEvent.MODE.END
import com.arata.yukarilauncher.event.single.RefreshVersionsEvent.MODE.START
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.favorites.FavoritesVersionUtils
import com.arata.yukarilauncher.feature.version.utils.VersionInfoUtils
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.EditTextDialog
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.utils.stringutils.SortStrings
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.arata.yukarilauncher.Tools
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * すべてのバージョンを管理する
 * @see Version
 */
object VersionsManager {
    private val versions = CopyOnWriteArrayList<Version>()

    /**
     * @return 現在のゲーム情報
     */
    lateinit var currentGameInfo: CurrentGameInfo
        private set

    private val coroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("VersionsManager"))
    private val refreshMutex = Mutex()
    private var isRefreshing: Boolean = false
    private var lastRefreshTime = 0L

    /**
     * @return リフレッシュ可能かどうかをチェックする
     */
    @JvmStatic
    fun canRefresh() = !isRefreshing && YLTools.getCurrentTimeMillis() - lastRefreshTime > 500

    /**
     * @return 全てのバージョンデータ
     */
    fun getVersions() = versions.toList()

    /**
     * バージョンが既に存在するかどうかを確認する
     * @param versionName バージョン名
     * @param checkJson JSONファイルの存在もチェックするかどうか
     * @return 存在する場合はtrue
     */
    fun isVersionExists(versionName: String, checkJson: Boolean = false): Boolean {
        val folder = File(ProfilePathHome.getVersionsHome(), versionName)
        // バージョンフォルダの存在に加えて、バージョンJSONファイルの存在も確認する
        return if (checkJson) File(folder, "${folder.name}.json").exists()
        else folder.exists()
    }

    /**
     * 非同期で現在のバージョンリストをリフレッシュする
     * リフレッシュ完了後、イベントで通知される（UIスレッドでは実行されない）
     * @param tag リフレッシュタスクを開始した呼び出し元の識別子（デバッグ用）
     * @param refreshVersionInfo バージョン情報を再解析するかどうか
     * @see com.arata.yukarilauncher.event.single.RefreshVersionsEvent
     */
    fun refresh(tag: String, refreshVersionInfo: Boolean = false) {
        Logging.i("VersionsManager", "$tag initiated the refresh version task")
        coroutineScope.launch {
            refreshMutex.withLock {
                lastRefreshTime = YLTools.getCurrentTimeMillis()
                handleRefreshOperation(refreshVersionInfo)
            }
        }
    }

    /**
     * リフレッシュ操作の内部処理
     * @param refreshVersionInfo バージョン情報を再解析するかどうか
     */
    private fun handleRefreshOperation(refreshVersionInfo: Boolean) {
        isRefreshing = true
        EventBus.getDefault().post(RefreshVersionsEvent(START))

        versions.clear()

        val versionsHome: String = ProfilePathHome.getVersionsHome()
        File(versionsHome).listFiles()?.forEach { versionFile ->
            runCatching {
                processVersionFile(versionsHome, versionFile, refreshVersionInfo)
            }
        }

        versions.sortWith { o1, o2 ->
            var sort = -SortStrings.compareClassVersions(
                o1.getVersionInfo()?.minecraftVersion ?: o1.getVersionName(),
                o2.getVersionInfo()?.minecraftVersion ?: o2.getVersionName()
            )
            if (sort == 0) sort = SortStrings.compareChar(o1.getVersionName(), o2.getVersionName())
            sort
        }

        currentGameInfo = CurrentGameInfo.refreshCurrentInfo()

        // イベントを使用してリフレッシュ完了を通知
        EventBus.getDefault().post(RefreshVersionsEvent(END))
        isRefreshing = false
    }

    /**
     * バージョンファイルを処理してVersionオブジェクトを生成する
     * @param versionsHome バージョンホームディレクトリ
     * @param versionFile バージョンファイル（ディレクトリ）
     * @param refreshVersionInfo バージョン情報を再解析するかどうか
     */
    private fun processVersionFile(versionsHome: String, versionFile: File, refreshVersionInfo: Boolean) {
        if (versionFile.exists() && versionFile.isDirectory) {
            var isVersion = false

            // バージョンの.jsonファイルの存在で、バージョンかどうかを判定
            val jsonFile = File(versionFile, "${versionFile.name}.json")
            if (jsonFile.exists() && jsonFile.isFile) {
                isVersion = true
                val versionInfoFile = File(getYukariVersionPath(versionFile), "VersionInfo.json")
                if (refreshVersionInfo) FileUtils.deleteQuietly(versionInfoFile)
                if (!versionInfoFile.exists()) {
                    VersionInfoUtils.parseJson(jsonFile)?.save(versionFile)
                }
            }

            val versionConfig = VersionConfig.parseConfig(versionFile)

            val version = Version(
                versionsHome,
                versionFile.absolutePath,
                versionConfig,
                isVersion
            )
            versions.add(version)

            Logging.i("VersionsManager", "Identified and added version: ${version.getVersionName()}, " +
                    "Path: (${version.getVersionPath()}), " +
                    "Info: ${version.getVersionInfo()?.getInfoString()}")
        }
    }

    /**
     * @return 現在のバージョンを取得する
     */
    fun getCurrentVersion(): Version? {
        if (versions.isEmpty()) return null

        fun returnVersionByFirst(): Version? {
            return versions.find { it.isValid() }?.apply {
                // バージョンが有効であることを確認
                saveCurrentVersion(getVersionName())
            }
        }

        return runCatching {
            val versionString = currentGameInfo.version
            getVersion(versionString) ?: run {
                return returnVersionByFirst()
            }
        }.getOrElse { e ->
            Logging.e("Get Current Version", Tools.printToString(e))
            returnVersionByFirst()
        }
    }

    /**
     * @return バージョン名からバージョンの存在を確認する
     */
    fun checkVersionExistsByName(versionName: String?) =
        versionName?.let { name -> versions.any { it.getVersionName() == name } } ?: false

    /**
     * @return Yukariランチャーのバージョン識別フォルダを取得する
     */
    fun getYukariVersionPath(version: Version) = File(version.getVersionPath(), InfoDistributor.LAUNCHER_NAME)

    /**
     * @return ディレクトリからYukariランチャーのバージョン識別フォルダを取得する
     */
    fun getYukariVersionPath(folder: File) = File(folder, InfoDistributor.LAUNCHER_NAME)

    /**
     * @return 名前からYukariランチャーのバージョン識別フォルダを取得する
     */
    fun getYukariVersionPath(name: String) = File(getVersionPath(name), InfoDistributor.LAUNCHER_NAME)

    /**
     * @return 現在のバージョン設定アイコンを取得する
     */
    fun getVersionIconFile(version: Version) = File(getYukariVersionPath(version), "VersionIcon.png")

    /**
     * @return 名前から現在のバージョン設定アイコンを取得する
     */
    fun getVersionIconFile(name: String) = File(getYukariVersionPath(name), "VersionIcon.png")

    /**
     * @return 名前からバージョンフォルダのパスを取得する
     */
    fun getVersionPath(name: String) = File(ProfilePathHome.getVersionsHome(), name)

    /**
     * 現在選択されているバージョンを保存する
     * @param versionName バージョン名
     */
    fun saveCurrentVersion(versionName: String) {
        runCatching {
            currentGameInfo.apply {
                version = versionName
                saveCurrentInfo()
            }
        }.onFailure { e -> Logging.e("Save Current Version", Tools.printToString(e)) }
    }

    /**
     * バージョン名の検証を行う
     * @param context コンテキスト
     * @param newName 新しいバージョン名
     * @param versionInfo バージョン情報
     * @return エラーメッセージ。問題がない場合はnull
     */
    private fun validateVersionName(
        context: Context,
        newName: String,
        versionInfo: VersionInfo?
    ): String? {
        return when {
            isVersionExists(newName, true) ->
                context.getString(R.string.version_install_exists)
            versionInfo?.loaderInfo?.takeIf { it.isNotEmpty() }?.let {
                // ModLoader情報がある場合、バニラと同じ名前への変更を禁止する
                newName == versionInfo.minecraftVersion
            } ?: false ->
                context.getString(R.string.version_install_cannot_use_mc_name)
            else -> null
        }
    }

    /**
     * バージョン名変更ダイアログを開く（UIスレッドで実行する必要あり）
     * @param beforeRename リネーム前の処理
     */
    fun openRenameDialog(context: Context, version: Version, beforeRename: (() -> Unit)? = null) {
        EditTextDialog.Builder(context)
            .setTitle(R.string.version_manager_rename)
            .setEditText(version.getVersionName())
            .setAsRequired()
            .setConfirmListener { editText, _ ->
                val string = editText.text.toString()

                // 元の名前と同じ場合は何もしない
                if (string == version.getVersionName()) return@setConfirmListener true

                if (FileTools.isFilenameInvalid(editText)) {
                    return@setConfirmListener false
                }

                val error = validateVersionName(context, string, version.getVersionInfo())
                error?.let {
                    editText.error = it
                    return@setConfirmListener false
                }

                beforeRename?.invoke()
                renameVersion(version, string)

                true
            }.showDialog()
    }

    /**
     * バージョンをリネームする
     * リネーム名の妥当性チェックはここでは行わない
     */
    private fun renameVersion(version: Version, name: String) {
        val currentVersionName = getCurrentVersion()?.getVersionName()
        // 現在のバージョンがリネーム対象の場合、新しい名前を現在のバージョンとして設定
        if (version.getVersionName() == currentVersionName) saveCurrentVersion(name)

        // お気に入り内のバージョン名を更新
        FavoritesVersionUtils.renameVersion(version.getVersionName(), name)

        val versionFolder = version.getVersionPath()
        val renameFolder = File(ProfilePathHome.getVersionsHome(), name)

        // リネーム先のフォルダが存在する場合は必ず削除する
        // さもなければ問題が発生する
        FileUtils.deleteQuietly(renameFolder)

        val originalName = versionFolder.name

        FileTools.renameFile(versionFolder, renameFolder)

        val versionJsonFile = File(renameFolder, "$originalName.json")
        val versionJarFile = File(renameFolder, "$originalName.jar")
        val renameJsonFile = File(renameFolder, "$name.json")
        val renameJarFile = File(renameFolder, "$name.jar")

        FileTools.renameFile(versionJsonFile, renameJsonFile)
        FileTools.renameFile(versionJarFile, renameJarFile)

        FileUtils.deleteQuietly(versionFolder)

        // リネーム後にリストをリフレッシュ
        refresh("VersionsManager:renameVersion")
    }

    /**
     * バージョンコピーダイアログを開く
     * 選択したバージョンを新しいバージョンとして複製する
     */
    fun openCopyDialog(context: Context, version: Version) {
        val dialog = YLTools.createTaskRunningDialog(context)
        EditTextDialog.Builder(context)
            .setTitle(R.string.version_manager_copy)
            .setMessage(R.string.version_manager_copy_tip)
            .setCheckBoxText(R.string.version_manager_copy_all)
            .setShowCheckBox(true)
            .setEditText(version.getVersionName())
            .setAsRequired()
            .setConfirmListener { editText, checked ->
                val string = editText.text.toString()

                // 元の名前と同じ場合は何もしない
                if (string == version.getVersionName()) return@setConfirmListener true

                if (FileTools.isFilenameInvalid(editText)) {
                    return@setConfirmListener false
                }

                val error = validateVersionName(context, string, version.getVersionInfo())
                error?.let {
                    editText.error = it
                    return@setConfirmListener false
                }

                Task.runTask {
                    copyVersion(version, string, checked)
                }.beforeStart(TaskExecutors.getAndroidUI()) {
                    dialog.show()
                }.onThrowable { e ->
                    Tools.showErrorRemote(e)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    dialog.dismiss()
                    refresh("VersionsManager:openCopyDialog")
                }.execute()
                true
            }.showDialog()
    }

    /**
     * 選択したバージョンを新しいバージョンとしてコピーする
     * @param version 選択したバージョン
     * @param name 新しいバージョンの名前
     * @param copyAllFile 全てのファイルをコピーするかどうか
     */
    private fun copyVersion(version: Version, name: String, copyAllFile: Boolean) {
        val versionsFolder = version.getVersionsFolder()
        val newVersion = File(versionsFolder, name)

        val originalName = version.getVersionName()

        // 新しいバージョンのjsonファイルとjarファイル
        val newJsonFile = File(newVersion, "$name.json")
        val newJarFile = File(newVersion, "$name.jar")

        val originalVersionFolder = version.getVersionPath()
        if (copyAllFile) {
            // 全てのファイルをコピーする場合、元のフォルダ全体を新しいバージョンに複製
            FileUtils.copyDirectory(originalVersionFolder, newVersion)
            // jsonファイルとjarファイルをリネーム
            val jsonFile = File(newVersion, "$originalName.json")
            val jarFile = File(newVersion, "$originalName.jar")
            if (jsonFile.exists()) jsonFile.renameTo(newJsonFile)
            if (jarFile.exists()) jarFile.renameTo(newJarFile)
        } else {
            // 全てのファイルをコピーしない場合、jsonファイルとjarファイルのみをコピーしてリネーム
            val originalJsonFile = File(originalVersionFolder, "$originalName.json")
            val originalJarFile = File(originalVersionFolder, "$originalName.jar")
            newVersion.mkdirs()
            // versions/1.21.3/1.21.3.json -> versions/name/name.json
            if (originalJsonFile.exists()) originalJsonFile.copyTo(newJsonFile)
            // versions/1.21.3/1.21.3.jar -> versions/name/name.jar
            if (originalJarFile.exists()) originalJarFile.copyTo(newJarFile)
        }

        // バージョン設定ファイルを保存
        version.getVersionConfig().copy().let { config ->
            config.setVersionPath(newVersion)
            config.setIsolationType(VersionConfig.IsolationType.ENABLE)
            config.saveWithThrowable()
        }
    }

    /**
     * 名前からVersionを取得する
     * @param name バージョン名
     * @return 見つかったVersion。存在しない場合はnull
     */
    private fun getVersion(name: String?): Version? {
        name?.let { versionName ->
            return versions.find { it.getVersionName() == versionName }?.takeIf { it.isValid() }
        }
        return null
    }
}