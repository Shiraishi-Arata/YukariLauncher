/**
 * ランチャーのメインアクティビティ。
 * フラグメント管理、ゲーム起動、アカウント管理、設定などを担当します。
 */
package com.arata.yukarilauncher.ui.activity

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.palette.graphics.Palette
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.InfoDistributor
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.databinding.ActivityLauncherBinding
import com.arata.yukarilauncher.event.single.LaunchGameEvent
import com.arata.yukarilauncher.event.single.MainBackgroundChangeEvent
import com.arata.yukarilauncher.event.single.PageOpacityChangeEvent
import com.arata.yukarilauncher.event.single.SwapToLoginEvent
import com.arata.yukarilauncher.event.sticky.MinecraftVersionValueEvent
import com.arata.yukarilauncher.event.value.AddFragmentEvent
import com.arata.yukarilauncher.event.value.DownloadProgressKeyEvent
import com.arata.yukarilauncher.event.value.InstallGameEvent
import com.arata.yukarilauncher.event.value.InstallLocalModpackEvent
import com.arata.yukarilauncher.event.value.LocalLoginEvent
import com.arata.yukarilauncher.event.value.MicrosoftLoginEvent
import com.arata.yukarilauncher.event.value.OtherLoginEvent
import com.arata.yukarilauncher.feature.accounts.AccountType
import com.arata.yukarilauncher.feature.accounts.AccountsManager
import com.arata.yukarilauncher.feature.accounts.LocalAccountUtils
import com.arata.yukarilauncher.feature.background.BackgroundManager
import com.arata.yukarilauncher.feature.background.BackgroundType
import com.arata.yukarilauncher.feature.download.item.ModLoaderWrapper
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.mod.modpack.install.InstallExtra
import com.arata.yukarilauncher.feature.mod.modpack.install.InstallLocalModPack
import com.arata.yukarilauncher.feature.mod.modpack.install.ModPackInfo
import com.arata.yukarilauncher.feature.mod.modpack.install.ModPackUtils
import com.arata.yukarilauncher.feature.update.UpdateUtils
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.feature.version.VersionsManager
import com.arata.yukarilauncher.feature.version.install.GameInstaller
import com.arata.yukarilauncher.feature.version.install.InstallTask
import com.arata.yukarilauncher.plugins.renderer.RendererPlugin
import com.arata.yukarilauncher.plugins.renderer.RendererPluginManager
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.EditTextDialog
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.ui.fragment.AccountFragment
import com.arata.yukarilauncher.ui.fragment.BaseFragment
import com.arata.yukarilauncher.ui.fragment.DownloadFragment
import com.arata.yukarilauncher.ui.fragment.DownloadModFragment
import com.arata.yukarilauncher.ui.fragment.SettingsFragment
import com.arata.yukarilauncher.ui.subassembly.settingsbutton.ButtonType
import com.arata.yukarilauncher.ui.subassembly.settingsbutton.OnTypeChangeListener
import com.arata.yukarilauncher.ui.subassembly.settingsbutton.SettingsButtonWrapper
import com.arata.yukarilauncher.ui.subassembly.view.DraggableViewWrapper
import com.arata.yukarilauncher.utils.StoragePermissionsUtils
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.anim.ViewAnimUtils
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.utils.image.ImageUtils
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import com.arata.yukarilauncher.launch.LaunchGame
import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.login.MicrosoftBackgroundLogin
import com.arata.yukarilauncher.ui.activity.OpenDocumentWithExtension
import com.arata.yukarilauncher.ui.fragment.MainMenuFragment
import com.arata.yukarilauncher.setting.LauncherPreferences
import com.arata.yukarilauncher.task.ProgressKeeper
import com.arata.yukarilauncher.task.TaskCountListener
import com.arata.yukarilauncher.feature.ProgressServiceKeeper
import com.arata.yukarilauncher.task.AsyncVersionList
import com.arata.yukarilauncher.utils.NotificationUtils
import com.arata.yukarilauncher.value.JMinecraftVersionList
import com.arata.yukarilauncher.value.MinecraftAccount
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random
import java.util.concurrent.Future

/**
 * ランチャーのメインアクティビティ。
 * フラグメント管理、ゲーム起動、アカウント管理、設定などを担当します。
 */
@Suppress("UNUSED")
class LauncherActivity : BaseActivity() {
    private val noticeAnimPlayer = AnimPlayer()

    /** Modインストーラを起動するためのActivityResultLauncher */
    @JvmField
    val modInstallerLauncher: ActivityResultLauncher<Any> =
        registerForActivityResult(OpenDocumentWithExtension("jar")) { uris ->
            if (uris != null && uris.isNotEmpty()) {
                Tools.launchModInstaller(this, uris[0])
            }
        }

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var mSettingsButtonWrapper: SettingsButtonWrapper
    private var mProgressServiceKeeper: ProgressServiceKeeper? = null
    private var mNotificationManager: NotificationManager? = null
    private var checkNotice: Future<*>? = null
    private var isVideoBackgroundPlaying = false

    /** フラグメント再開時に設定ボタンの種類を更新するコールバック */
    private val mFragmentCallbackListener = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            mSettingsButtonWrapper.setButtonType(
                if (f is MainMenuFragment) ButtonType.SETTINGS else ButtonType.HOME
            )
        }
    }

    /** ダブル起動を防止するタスクカウントリスナー */
    private val mDoubleLaunchPreventionListener = TaskCountListener { taskCount ->
        if (taskCount > 0) {
            TaskExecutors.runInUIThread {
                mNotificationManager?.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START)
            }
        }
    }

    private var mRequestNotificationPermissionLauncher: ActivityResultLauncher<String>? = null
    private var mRequestNotificationPermissionRunnable: WeakReference<Runnable>? = null

    /** ページ不透明度変更イベントを処理します */
    @Subscribe
    fun event(event: PageOpacityChangeEvent) {
        setPageOpacity(event.progress)
    }

    /** メイン背景変更イベントを処理します */
    @Subscribe
    fun event(event: MainBackgroundChangeEvent) {
        refreshBackground()
        setPageOpacity(AllSettings.pageOpacity.getValue())
    }

    /** ログイン画面への切り替えイベントを処理します */
    @Subscribe
    fun event(event: SwapToLoginEvent) {
        val currentFragment = getCurrentFragment()
        if (currentFragment == null || getVisibleFragment(AccountFragment.TAG) != null) return
        YLTools.swapFragmentWithAnim(currentFragment, AccountFragment::class.java, AccountFragment.TAG, null)
    }

    /** ゲーム起動イベントを処理します。バージョンとアカウントの確認後、ゲームを起動します */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun event(event: LaunchGameEvent) {
        if (binding.progressLayout.hasProcesses()) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
            return
        }

        val version = VersionsManager.getCurrentVersion()
        if (version == null) {
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show()
            return
        }

        if (AccountsManager.allAccounts.isEmpty()) {
            Toast.makeText(this, R.string.account_no_saved_accounts, Toast.LENGTH_LONG).show()
            EventBus.getDefault().post(SwapToLoginEvent())
            return
        }

        val rendererPlugin = RendererPluginManager.getConfigurablePluginOrNull(version.getRenderer())
        if (rendererPlugin != null) {
            StoragePermissionsUtils.checkPermissions(
                this,
                R.string.generic_warning,
                getString(R.string.permissions_storage_for_renderer_config, rendererPlugin.displayName, InfoDistributor.APP_NAME),
                object : StoragePermissionsUtils.PermissionGranted {
                    override fun granted() { launchGame(version) }
                    override fun cancelled() { launchGame(version) }
                }
            )
            return
        }

        launchGame(version)
    }

    /** Microsoftログインのリダイレクトイベントを処理します */
    @Subscribe
    fun event(event: MicrosoftLoginEvent) {
        MicrosoftBackgroundLogin(false, event.uri.getQueryParameter("code")!!).performLogin(
            this, null,
            AccountsManager.doneListener,
            AccountsManager.errorListener
        )
    }

    /** その他ログイン方式のイベントを処理します */
    @Subscribe
    fun event(event: OtherLoginEvent) {
        Task.runTask {
            event.account.save()
            Logging.i("Account", "Saved the account : ${event.account.username}")
            null
        }.onThrowable { e ->
            Logging.e("Account", "Failed to save the account : $e")
        }.finallyTask {
            AccountsManager.doneListener.onLoginDone(event.account)
        }.execute()
    }

    /** ローカルアカウントログインイベントを処理します */
    @Subscribe
    fun event(event: LocalLoginEvent) {
        val userName = event.userName
        val localAccount = MinecraftAccount().apply {
            username = userName
            accountType = AccountType.LOCAL.type
            try {
                save()
                Logging.i("Account", "Saved the account : $username")
            } catch (e: IOException) {
                Logging.e("Account", "Failed to save the account : $e")
            }
        }
        AccountsManager.doneListener.onLoginDone(localAccount)
    }

    /** ローカルModパックのインストールイベントを処理します */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun event(event: InstallLocalModpackEvent) {
        val installExtra = event.installExtra
        if (!installExtra.startInstall) return

        if (binding.progressLayout.hasProcesses()) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
            return
        }

        val dirGameModpackFile = File(installExtra.modpackPath)
        val info = ModPackUtils.determineModpack(dirGameModpackFile)
        if (info.type == ModPackUtils.ModPackEnum.UNKNOWN) {
            InstallLocalModPack.showUnSupportDialog(this)
        }

        val modPackName = info.name ?: FileTools.getFileNameWithoutExtension(dirGameModpackFile)

        EditTextDialog.Builder(this)
            .setTitle(R.string.version_install_new)
            .setEditText(modPackName)
            .setAsRequired()
            .setConfirmListener { editText, _ ->
                val customName = editText.text.toString()
                if (FileTools.isFilenameInvalid(editText)) return@setConfirmListener false
                if (VersionsManager.isVersionExists(customName, true)) {
                    editText.error = getString(R.string.version_install_exists)
                    return@setConfirmListener false
                }
                Task.runTask {
                    val modLoaderWrapper = InstallLocalModPack.installModPack(this@LauncherActivity, info.type, dirGameModpackFile, customName)
                    if (modLoaderWrapper != null) {
                        val downloadTask = modLoaderWrapper.getDownloadTask()
                        if (downloadTask != null) {
                            runOnUiThread {
                                Toast.makeText(this@LauncherActivity, getString(R.string.modpack_prepare_mod_loader_installation), Toast.LENGTH_SHORT).show()
                            }
                            Logging.i("Install Version", "Installing ModLoader: ${modLoaderWrapper.modLoaderVersion}")
                            val file = downloadTask.run(customName)
                            if (file != null) return@runTask kotlin.Pair(modLoaderWrapper, file)
                        }
                    }
                    null
                }.beforeStart(TaskExecutors.getAndroidUI()) {
                    ProgressLayout.setProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.generic_waiting)
                }.ended { filePair ->
                    if (filePair != null) {
                        ModPackUtils.startModLoaderInstall(filePair.first, this@LauncherActivity, filePair.second, customName)
                    }
                }.onThrowable(TaskExecutors.getAndroidUI()) { e ->
                    Tools.showErrorRemote(this@LauncherActivity, R.string.modpack_install_download_failed, e)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
                }.execute()
                true
            }.showDialog()
    }

    /** ゲームのインストールイベントを処理します */
    @Subscribe
    fun event(event: InstallGameEvent) {
        GameInstaller(this, event).installGame()
    }

    /** ダウンロード進捗キーの監視・監視解除イベントを処理します */
    @Subscribe
    fun event(event: DownloadProgressKeyEvent) {
        if (event.observe) {
            binding.progressLayout.observe(event.progressKey)
        } else {
            binding.progressLayout.unObserve(event.progressKey)
        }
    }

    /** フラグメント追加イベントを処理します */
    @Subscribe
    @Synchronized
    fun event(event: AddFragmentEvent) {
        val currentFragment = getCurrentFragment()
        if (currentFragment != null) {
            try {
                val activityCallBack = event.fragmentActivityCallback
                if (activityCallBack != null) {
                    activityCallBack.callBack(currentFragment.requireActivity())
                }
                YLTools.addFragment(
                    currentFragment,
                    event.fragmentClass,
                    event.fragmentTag,
                    event.bundle
                )
            } catch (e: Exception) {
                Logging.e("LauncherActivity", "Failed attempt to jump to a new Fragment!", e)
            }
        }
    }

    /** アクティビティ作成時に呼び出されます。レイアウトの初期化、フラグメント管理、通知権限の確認を行います */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        processFragment()
        processViews()

        mRequestNotificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isAllowed ->
            if (!isAllowed) handleNoNotificationPermission()
            else {
                val runnable = Tools.getWeakReference(mRequestNotificationPermissionRunnable)
                runnable?.run()
            }
        }
        checkNotificationPermission()

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener)
        mProgressServiceKeeper = ProgressServiceKeeper(this)
        ProgressKeeper.addTaskCountListener(mProgressServiceKeeper!!)
        ProgressKeeper.addTaskCountListener(binding.progressLayout)

        AsyncVersionList().getVersionList(
            object : AsyncVersionList.VersionDoneListener {
                override fun onVersionDone(versions: JMinecraftVersionList) {
                    EventBus.getDefault().postSticky(MinecraftVersionValueEvent(versions))
                }
            },
            false
        )

        Task.runTask {
            UpdateUtils.checkDownloadedPackage(this@LauncherActivity, false, true)
            null
        }.execute()
    }

    /** フラグメント管理の初期化を行います。戻るボタンの処理と初期フラグメントの設定を行います */
    private fun processFragment() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = getCurrentFragment()
                if (currentFragment is BaseFragment && !currentFragment.onBackPressed()) {
                    return
                }
                if (supportFragmentManager.backStackEntryCount <= 1) {
                    finish()
                } else {
                    supportFragmentManager.popBackStackImmediate()
                }
            }
        })

        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount < 1) {
            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .addToBackStack(MainMenuFragment.TAG)
                .add(R.id.container_fragment, MainMenuFragment::class.java, null, MainMenuFragment.TAG)
                .commit()
        }
    }

    /** ビューの初期化を行います。背景、ボタン、進捗レイアウト、通知の設定を行います */
    private fun processViews() {
        refreshBackground()
        setPageOpacity(AllSettings.pageOpacity.getValue())
        mSettingsButtonWrapper = SettingsButtonWrapper(binding.settingButton)
        mSettingsButtonWrapper.setOnTypeChangeListener(object : OnTypeChangeListener {
            override fun onChange(type: ButtonType) {
                ViewAnimUtils.setViewAnim(binding.settingButton, Animations.Pulse)
            }
        })
        binding.downloadButton.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentById(binding.containerFragment.id)
            if (fragment != null && fragment !is DownloadFragment && fragment !is DownloadModFragment) {
                ViewAnimUtils.setViewAnim(binding.downloadButton, Animations.Pulse)
                YLTools.swapFragmentWithAnim(fragment, DownloadFragment::class.java, DownloadFragment.TAG, null)
            }
        }
        binding.settingButton.setOnClickListener {
            ViewAnimUtils.setViewAnim(binding.settingButton, Animations.Pulse)
            val fragment = supportFragmentManager.findFragmentById(binding.containerFragment.id)
            if (fragment is MainMenuFragment) {
                YLTools.swapFragmentWithAnim(fragment, SettingsFragment::class.java, SettingsFragment.TAG, null)
            } else {
                Tools.backToMainMenu(this)
            }
        }
        binding.appTitleText.text = InfoDistributor.APP_NAME
        binding.appTitleText.isClickable = false
        binding.appTitleText.isFocusable = false
        binding.appTitleText.setOnClickListener(null)

        binding.progressLayout.observe(ProgressLayout.DOWNLOAD_MINECRAFT)
        binding.progressLayout.observe(ProgressLayout.UNPACK_RUNTIME)
        binding.progressLayout.observe(ProgressLayout.INSTALL_RESOURCE)
        binding.progressLayout.observe(ProgressLayout.LOGIN_ACCOUNT)
        binding.progressLayout.observe(ProgressLayout.DOWNLOAD_VERSION_LIST)
        binding.progressLayout.observe(ProgressLayout.CHECKING_MODS)

        binding.noticeGotButton.setOnClickListener {
            AllSettings.noticeDefault.put(false).save()
        }
        DraggableViewWrapper(binding.noticeLayout, object : DraggableViewWrapper.AttributesFetcher {
            override val screenPixels: DraggableViewWrapper.ScreenPixels
                get() = DraggableViewWrapper.ScreenPixels(
                    0, 0,
                    Tools.currentDisplayMetrics.widthPixels - binding.noticeLayout.width,
                    Tools.currentDisplayMetrics.heightPixels - binding.noticeLayout.height
                )
            override fun get(): IntArray {
                return intArrayOf(binding.noticeLayout.x.toInt(), binding.noticeLayout.y.toInt())
            }
            override fun set(x: Int, y: Int) {
                binding.noticeLayout.x = x.toFloat()
                binding.noticeLayout.y = y.toFloat()
            }
        }).init()

        binding.hair.visibility = if (YLTools.checkDate(4, 1)) View.VISIBLE else View.GONE
    }

    /** アクティビティ再開時に呼び出されます。透明度の更新、バージョン一覧のリフレッシュ、ビデオ背景の再生を行います */
    override fun onResume() {
        super.onResume()
        setPageOpacity(AllSettings.pageOpacity.getValue())
        VersionsManager.refresh("LauncherActivity:onResume", false)
        if (isVideoBackgroundPlaying) binding.backgroundVideoView.start()
    }

    /** アクティビティ一時停止時にビデオ背景を一時停止します */
    override fun onPause() {
        super.onPause()
        if (isVideoBackgroundPlaying && binding.backgroundVideoView.isPlaying) {
            binding.backgroundVideoView.pause()
        }
    }

    /** アクティビティ開始時にフラグメントライフサイクルコールバックを登録します */
    override fun onStart() {
        super.onStart()
        supportFragmentManager.registerFragmentLifecycleCallbacks(mFragmentCallbackListener, true)
    }

    /** アクティビティ破棄時にリソースをクリーンアップします */
    override fun onDestroy() {
        super.onDestroy()
        binding.progressLayout.cleanUpObservers()
        ProgressKeeper.removeTaskCountListener(binding.progressLayout)
        mProgressServiceKeeper?.let { ProgressKeeper.removeTaskCountListener(it) }
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener)
        ContextExecutor.clearActivity()
        stopVideoBackground()
    }

    /** ウィンドウへのアタッチ時にノッチサイズを計算します */
    override fun onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this)
    }

    /** ゲームを起動します。ローカルアカウントの使用可否を確認してから起動します */
    private fun launchGame(version: Version) {
        LocalAccountUtils.checkUsageAllowed(object : LocalAccountUtils.CheckResultListener {
            override fun onUsageAllowed() {
                LaunchGame.preLaunch(this@LauncherActivity, version)
            }
            override fun onUsageDenied() {
                if (!AllSettings.localAccountReminders.getValue()) {
                    LaunchGame.preLaunch(this@LauncherActivity, version)
                } else {
                    LocalAccountUtils.openDialog(
                        this@LauncherActivity,
                        { checked ->
                            LocalAccountUtils.saveReminders(checked)
                            LaunchGame.preLaunch(this@LauncherActivity, version)
                        },
                        getString(R.string.account_no_microsoft_account) + getString(R.string.account_purchase_minecraft_account_tip),
                        R.string.account_continue_to_launch_the_game
                    )
                }
            }
        })
    }

    /** メインメニューの背景画像またはビデオをリフレッシュして表示します */
    private fun refreshBackground() {
        val mediaFile = BackgroundManager.getBackgroundImage(BackgroundType.MAIN_MENU)
        if (mediaFile != null && BackgroundManager.isVideo(mediaFile)) {
            playVideoBackground(mediaFile)
            refreshTopBarColor(false)
            return
        }
        stopVideoBackground()
        BackgroundManager.setBackgroundImage(this, BackgroundType.MAIN_MENU, binding.backgroundView) { refreshTopBarColor(it) }
    }

    /** ビデオ背景を再生します */
    private fun playVideoBackground(videoFile: File) {
        binding.backgroundView.setImageDrawable(null)
        binding.backgroundView.visibility = View.GONE
        binding.backgroundVideoView.visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.backgroundVideoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        }
        binding.backgroundVideoView.setVideoPath(videoFile.absolutePath)
        binding.backgroundVideoView.setOnPreparedListener { mp ->
            mp.setVolume(0f, 0f)
            mp.isLooping = true
            binding.backgroundVideoView.start()
            isVideoBackgroundPlaying = true
        }
        binding.backgroundVideoView.setOnErrorListener { _, _, _ ->
            stopVideoBackground()
            BackgroundManager.setBackgroundImage(this, BackgroundType.MAIN_MENU, binding.backgroundView) { refreshTopBarColor(it) }
            true
        }
    }

    /** ビデオ背景を停止し、静止画背景に切り替えます */
    private fun stopVideoBackground() {
        if (isVideoBackgroundPlaying) binding.backgroundVideoView.stopPlayback()
        isVideoBackgroundPlaying = false
        binding.backgroundVideoView.visibility = View.GONE
        binding.backgroundView.visibility = View.VISIBLE
    }

    /** トップバーの背景色を更新します（背景画像からパレットを生成） */
    private fun refreshTopBarColor(loadFromBackground: Boolean) {
        val backgroundMenuTop = ContextCompat.getColor(this, R.color.background_menu_top)
        if (loadFromBackground) {
            val bitmap = ImageUtils.getBitmapFromImageView(binding.backgroundView)
            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                val isDarkMode = YLTools.isDarkMode(this)
                binding.topLayout.setBackgroundColor(
                    if (isDarkMode) palette.darkVibrantSwatch?.rgb ?: backgroundMenuTop
                    else palette.lightVibrantSwatch?.rgb ?: backgroundMenuTop
                )
                val mutedColor = if (isDarkMode) palette.lightMutedSwatch?.rgb ?: 0xFFFFFFFF.toInt()
                else palette.darkMutedSwatch?.rgb ?: 0xFFFFFFFF.toInt()
                binding.appTitleText.setTextColor(mutedColor)
                val colorStateList = ColorStateList.valueOf(mutedColor)
                binding.downloadButton.imageTintList = colorStateList
                binding.settingButton.imageTintList = colorStateList
                return
            }
        }
        binding.topLayout.setBackgroundColor(backgroundMenuTop)
        binding.appTitleText.setTextColor(ContextCompat.getColor(this, R.color.menu_bar_text))
        val colorStateList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
        binding.downloadButton.imageTintList = colorStateList
        binding.settingButton.imageTintList = colorStateList
    }

    /** タグから表示可能なフラグメントを取得します */
    private fun getVisibleFragment(tag: String): Fragment? {
        return checkFragmentAvailability(supportFragmentManager.findFragmentByTag(tag))
    }

    /** IDから表示可能なフラグメントを取得します */
    private fun getVisibleFragment(id: Int): Fragment? {
        return checkFragmentAvailability(supportFragmentManager.findFragmentById(id))
    }

    /** 現在のコンテナのフラグメントを取得します */
    private fun getCurrentFragment(): Fragment? {
        return getVisibleFragment(binding.containerFragment.id)
    }

    /** フラグメントが表示可能かどうかを確認します */
    private fun checkFragmentAvailability(fragment: Fragment?): Fragment? {
        return if (fragment != null && fragment.isVisible) fragment else null
    }

    /** 通知権限を確認し、必要に応じて許可を求めます */
    private fun checkNotificationPermission() {
        if (AllSettings.skipNotificationPermissionCheck.getValue() || YLTools.checkForNotificationPermission()) return
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            showNotificationPermissionReasoning()
            return
        }
        askForNotificationPermission(null)
    }

    /** 通知権限が必要な理由を説明するダイアログを表示します */
    private fun showNotificationPermissionReasoning() {
        TipDialog.Builder(this)
            .setTitle(R.string.notification_permission_dialog_title)
            .setMessage(getString(R.string.notification_permission_dialog_text, InfoDistributor.APP_NAME, InfoDistributor.APP_NAME))
            .setConfirmClickListener { askForNotificationPermission(null) }
            .setCancelClickListener { handleNoNotificationPermission() }
            .showDialog()
    }

    /** 通知権限が得られなかった場合の処理を行います */
    private fun handleNoNotificationPermission() {
        AllSettings.skipNotificationPermissionCheck.put(true).save()
        Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show()
    }

    /** 通知権限を要求します */
    fun askForNotificationPermission(onSuccessRunnable: Runnable?) {
        if (Build.VERSION.SDK_INT < 33) return
        if (onSuccessRunnable != null) {
            mRequestNotificationPermissionRunnable = WeakReference(onSuccessRunnable)
        }
        mRequestNotificationPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** ページの不透明度を設定します */
    private fun setPageOpacity(pageOpacity: Int) {
        val opacity = BigDecimal.valueOf(pageOpacity.toLong()).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val v = opacity.toFloat()
        binding.containerFragment.alpha = v
        val adjustedOpacity = if (BackgroundManager.hasBackgroundImage(BackgroundType.MAIN_MENU))
            opacity.subtract(BigDecimal.valueOf(0.1)).max(BigDecimal.ZERO)
        else
            BigDecimal.ONE
        binding.topLayout.alpha = adjustedOpacity.toFloat()
    }
}
