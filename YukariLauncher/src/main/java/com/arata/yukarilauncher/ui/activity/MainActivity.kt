/**
 * ゲームプレイ中のメインアクティビティ。
 * ゲームのレンダリング、入力処理、設定メニューを管理します。
 */
package com.arata.yukarilauncher.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.MinecraftGLSurface
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.databinding.ActivityGameBinding
import com.arata.yukarilauncher.databinding.ViewControlMenuBinding
import com.arata.yukarilauncher.databinding.ViewGameMenuBinding
import com.arata.yukarilauncher.event.single.RefreshHotbarEvent
import com.arata.yukarilauncher.event.value.HotbarChangeEvent
import com.arata.yukarilauncher.event.value.JvmExitEvent
import com.arata.yukarilauncher.feature.GameService
import com.arata.yukarilauncher.feature.MCOptions
import com.arata.yukarilauncher.feature.ProfileLanguageSelector
import com.arata.yukarilauncher.feature.awt.AWTInputBridge
import com.arata.yukarilauncher.feature.awt.EfficientAndroidLWJGLKeycode
import com.arata.yukarilauncher.feature.background.BackgroundManager
import com.arata.yukarilauncher.feature.background.BackgroundType
import com.arata.yukarilauncher.feature.log.Logger
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.feature.version.VersionInfo
import com.arata.yukarilauncher.launch.LaunchGame
import com.arata.yukarilauncher.listener.SimpleTextWatcher
import com.arata.yukarilauncher.plugins.driver.DriverPluginManager
import com.arata.yukarilauncher.renderer.Renderers
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.setting.LauncherPreferences
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.KeyboardDialog
import com.arata.yukarilauncher.ui.dialog.SelectControlsDialog
import com.arata.yukarilauncher.ui.dialog.SelectMouseDialog
import com.arata.yukarilauncher.ui.fragment.settings.VideoSettingsFragment
import com.arata.yukarilauncher.ui.subassembly.adapter.ObjectSpinnerAdapter
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlButtonMenuListener
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlLayout
import com.arata.yukarilauncher.ui.subassembly.customcontrols.CustomControls
import com.arata.yukarilauncher.ui.subassembly.customcontrols.EditorExitable
import com.arata.yukarilauncher.ui.subassembly.customcontrols.keyboard.LwjglCharSender
import com.arata.yukarilauncher.ui.subassembly.customcontrols.keyboard.TouchCharInput
import com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse.GyroControl
import com.arata.yukarilauncher.ui.subassembly.hotbar.HotbarType
import com.arata.yukarilauncher.ui.subassembly.hotbar.HotbarUtils
import com.arata.yukarilauncher.ui.subassembly.menu.ControlMenu
import com.arata.yukarilauncher.ui.subassembly.menu.MenuUtils
import com.arata.yukarilauncher.ui.subassembly.view.FloatingLoggerWindow
import com.arata.yukarilauncher.ui.subassembly.view.GameMenuViewWrapper
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.anim.AnimUtils
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import java.io.IOException

/**
 * ゲームプレイ中のメインアクティビティ。
 * ゲームのレンダリング、入力処理、設定メニューを管理します。
 */
@Suppress("DEPRECATION")
class MainActivity : BaseActivity(), ControlButtonMenuListener, EditorExitable,
    ServiceConnection, ViewTreeObserver.OnGlobalLayoutListener {

    companion object {
        @JvmField @Volatile
        var GLOBAL_CLIPBOARD: ClipboardManager? = null
        const val INTENT_VERSION = "intent_version"
        @Volatile
        var isInputStackCall = false
        @SuppressLint("StaticFieldLeak")
        private var _binding: ActivityGameBinding? = null
        @JvmField
        var touchCharInput: TouchCharInput? = null

        /** 仮想マウスのオン/オフを切り替えます */
        @JvmStatic
        fun toggleMouse(ctx: Context) {
            if (CallbackBridge.isGrabbing()) return
            _binding?.let { binding ->
                Toast.makeText(
                    ctx,
                    if (binding.mainTouchpad.switchState()) R.string.control_mouseon else R.string.control_mouseoff,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        /** キーボードの表示状態を切り替えます */
        @JvmStatic
        fun switchKeyboardState() {
            _binding?.mainTouchCharInput?.switchKeyboardState()
        }

        /** ゲーム内からのリンクを開きます（ファイル共有またはURL） */
        @JvmStatic
        fun openLink(link: String) {
            val ctx = _binding?.mainTouchpad?.context ?: return
            (ctx as Activity).runOnUiThread {
                try {
                    setUri(ctx, link)
                } catch (th: Throwable) {
                    Tools.showError(ctx, th)
                }
            }
        }

        /** システムクリップボードの内容を取得します */
        @JvmStatic
        fun querySystemClipboard() {
            TaskExecutors.runInUIThread {
                val clipData = GLOBAL_CLIPBOARD?.primaryClip
                if (clipData == null) {
                    AWTInputBridge.nativeClipboardReceived(null, null)
                    return@runInUIThread
                }
                val clipItemText = clipData.getItemAt(0).text
                if (clipItemText == null) {
                    AWTInputBridge.nativeClipboardReceived(null, null)
                    return@runInUIThread
                }
                AWTInputBridge.nativeClipboardReceived(clipItemText.toString(), "plain")
            }
        }

        /** クリップボードにデータを設定します */
        @JvmStatic
        fun putClipboardData(data: String, mimeType: String) {
            TaskExecutors.runInUIThread {
                val clipData = when (mimeType) {
                    "text/plain" -> ClipData.newPlainText("AWT Paste", data)
                    "text/html" -> ClipData.newHtmlText("AWT Paste", data, data)
                    else -> null
                }
                if (clipData != null) GLOBAL_CLIPBOARD?.setPrimaryClip(clipData)
            }
        }

        /** URIを処理してファイル共有またはリンクを開きます */
        private fun setUri(context: Context, input: String) {
            val path = when {
                input.startsWith("file://") -> input.substring(7)
                input.startsWith("file:") -> input.substring(5)
                else -> null
            }
            if (path != null) {
                Logging.i("MainActivity", path)
                FileTools.shareFile(context, File(path))
                Logging.i("In-game Share File/Folder", "Start!")
            } else {
                YLTools.openLink(context, input, "*/*")
            }
        }
    }

    private lateinit var binding: ActivityGameBinding
    private var mGameMenuWrapper: GameMenuViewWrapper? = null
    private var mGyroControl: GyroControl? = null
    private var keyboardDialog: KeyboardDialog? = null
    private var minecraftVersion: Version? = null
    private var mGameMenuBinding: ViewGameMenuBinding? = null
    private var mControlSettingsBinding: ViewControlMenuBinding? = null
    private var mMenuSettingsInitListener: MenuSettingsInitListener? = null
    var isInEditor = false
    private var mInputWatcher: SimpleTextWatcher? = null
    private val mInputPreviewAnim = AnimPlayer()
    private var isKeyboardVisible = false
    private var isVideoBackgroundPlaying = false
    private var floatingLogger: FloatingLoggerWindow? = null

    /** アクティビティ作成時に呼び出されます。ゲームバージョンの取得、レイアウト初期化、サービス起動を行います */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        minecraftVersion = intent.getParcelableExtra(INTENT_VERSION)
            ?: throw RuntimeException("The game version is not selected!")

        MCOptions.setup(this) { minecraftVersion!! }
        if (AllSettings.autoSetGameLanguage.getValue()) {
            ProfileLanguageSelector.setGameLanguage(minecraftVersion!!, AllSettings.gameLanguageOverridden.getValue())
        }

        val gameServiceIntent = Intent(this, GameService::class.java)
        ContextCompat.startForegroundService(this, gameServiceIntent)
        initLayout()
        CallbackBridge.addGrabListener(binding.mainTouchpad)
        CallbackBridge.addGrabListener(binding.mainGameRenderView)
        mGyroControl = GyroControl(this)

        val window = window ?: return
        window.setBackgroundDrawable(
            if (AllSettings.alternateSurface.getValue()) null
            else ColorDrawable(Color.BLACK)
        )
        window.setSustainedPerformanceMode(AllSettings.sustainedPerformance.getValue())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val controlLayout = binding.mainControlLayout
        mControlSettingsBinding = ViewControlMenuBinding.inflate(layoutInflater)
        ControlMenu(this, this, mControlSettingsBinding!!, controlLayout, false)
        mControlSettingsBinding!!.saveAndExport.visibility = View.GONE

        binding.mainControlLayout.setModifiable(false)
        bindService(gameServiceIntent, this, 0)

        mInputWatcher = SimpleTextWatcher { s -> binding.inputPreview.text = s.toString().trim() }
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(this)

        Logger.setLogListener { text ->
            runOnUiThread {
                floatingLogger?.appendLog("$text\n")
            }
        }
    }

    /** レイアウトの初期化を行います。バインディングの設定、背景、コントロール、ゲームレンダリングの準備を行います */
    @SuppressLint("SetTextI18n")
    protected fun initLayout() {
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _binding = binding

        floatingLogger = FloatingLoggerWindow(this)
        floatingLogger?.hide()

        mGameMenuWrapper = GameMenuViewWrapper(this, { onClickedMenu() }, true)
        touchCharInput = binding.mainTouchCharInput

        refreshBackground()
        keyboardDialog = KeyboardDialog(this).setShowSpecialButtons(false)

        binding.mainControlLayout.setMenuListener(this)
        binding.mainDrawerOptions.setScrimColor(Color.TRANSPARENT)
        binding.mainDrawerOptions.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        try {
            val latestLogFile = File(PathManager.DIR_GAME_HOME, "latestlog.txt")
            if (!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw IOException("Failed to create a new log file")
            Logger.begin(latestLogFile.absolutePath)
            GLOBAL_CLIPBOARD = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            binding.mainTouchCharInput.setCharacterSender(LwjglCharSender())

            Logging.i("RdrDebug", "__P_renderer=${minecraftVersion!!.getRenderer()}")
            Renderers.setCurrentRenderer(this, minecraftVersion!!.getRenderer(), false)
            DriverPluginManager.setDriverByName(minecraftVersion!!.getDriver())

            title = "Minecraft ${minecraftVersion!!.getVersionName()}"

            val mVersionInfo = Tools.getVersionInfo(minecraftVersion!!.getVersionName())
            isInputStackCall = mVersionInfo.arguments != null
            CallbackBridge.nativeSetUseInputStackQueue(isInputStackCall)

            Tools.getDisplayMetrics(this)
            CallbackBridge.windowWidth = Tools.getDisplayFriendlyRes(Tools.currentDisplayMetrics.widthPixels, 1f)
            CallbackBridge.windowHeight = Tools.getDisplayFriendlyRes(Tools.currentDisplayMetrics.heightPixels, 1f)

            mGameMenuBinding = ViewGameMenuBinding.inflate(layoutInflater)
            mMenuSettingsInitListener = MenuSettingsInitListener(mGameMenuBinding!!)

            binding.mainNavigationView.removeAllViews()
            binding.mainNavigationView.addView(mGameMenuBinding!!.root)

            binding.mainDrawerOptions.addDrawerListener(mMenuSettingsInitListener!!)
            binding.mainDrawerOptions.closeDrawers()

            binding.mainGameRenderView.setSurfaceReadyListener(object : MinecraftGLSurface.SurfaceReadyListener {
                override fun isReady() {
                    try {
                        if (AllSettings.virtualMouseStart.getValue()) {
                            binding.mainTouchpad.post { binding.mainTouchpad.switchState() }
                        }
                        LaunchGame.runGame(this@MainActivity, minecraftVersion!!, mVersionInfo)
                    } catch (e: Throwable) {
                        Tools.showErrorRemote(e)
                    }
                }
            })

            binding.mainGameRenderView.setOnRenderingStartedListener(object : MinecraftGLSurface.OnRenderingStartedListener {
                override fun isStarted() {
                    stopVideoBackground()
                    BackgroundManager.clearBackgroundImage(binding.backgroundView)
                    Logging.i("Rendering Game", "The game rendering has started, " +
                            "and the background image has been cleared to prevent certain issues from occurring.")
                }
            })

            if (AllSettings.enableLogOutput.getValue()) {
                floatingLogger?.show()
            }

            var mcInfo = ""
            val versionInfo = minecraftVersion!!.getVersionInfo()
            if (versionInfo != null) {
                mcInfo = versionInfo.getInfoString()
            }
            var tipString = StringUtils.insertNewline(
                binding.gameTip.text,
                StringUtils.insertSpace(getString(R.string.game_tip_version), minecraftVersion!!.getVersionName())
            )
            if (mcInfo.isNotEmpty()) {
                tipString = StringUtils.insertNewline(tipString, StringUtils.insertSpace(getString(R.string.game_tip_mc_info), mcInfo))
            }
            binding.gameTip.text = tipString
            AnimUtils.setVisibilityAnim(binding.gameTip, 1000, true, 300, object : AnimUtils.AnimationListener {
                override fun onStart() {}
                override fun onEnd() {
                    AnimUtils.setVisibilityAnim(binding.gameTip, 15000, false, 300, null)
                }
            })
        } catch (e: Throwable) {
            Tools.showError(this, e, true)
        }
    }

    /** コントロールレイアウトを読み込みます */
    private fun loadControls() {
        try {
            binding.mainControlLayout.loadLayout(minecraftVersion!!.getControl())
        } catch (e: IOException) {
            try {
                Logging.w("MainActivity", "Unable to load the control file, loading the default now", e)
                binding.mainControlLayout.loadLayout(null as String?)
            } catch (ioException: IOException) {
                Tools.showError(this, ioException)
            }
        } catch (th: Throwable) {
            Tools.showError(this, th)
        }
        mGameMenuWrapper?.setVisibility(!binding.mainControlLayout.hasMenuButton())
        binding.mainControlLayout.toggleControlVisible()
    }

    /** ウィンドウにアタッチされたときに呼び出されます。ノッチサイズの計算とコントロールの読み込みを行います */
    override fun onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this)
        loadControls()
    }

    /** アクティビティ再開時に呼び出されます。ビデオ背景の再生、ジャイロの有効化、ウィンドウフォーカスの設定を行います */
    override fun onResume() {
        super.onResume()
        if (isVideoBackgroundPlaying) binding.backgroundVideoView.start()
        if (AllStaticSettings.enableGyro) mGyroControl?.enable()
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_FOCUSED, 1)
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 1)
    }

    /** アクティビティ一時停止時に呼び出されます。ジャイロの無効化、フォーカス解除、ビデオ背景の一時停止を行います */
    override fun onPause() {
        mGyroControl?.disable()
        if (CallbackBridge.isGrabbing()) {
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt())
        }
        if (isVideoBackgroundPlaying && binding.backgroundVideoView.isPlaying) {
            binding.backgroundVideoView.pause()
        }
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_FOCUSED, 0)
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 0)
        super.onPause()
    }

    /** ウィンドウフォーカス変更時に呼び出されます */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_FOCUSED, if (hasFocus) 1 else 0)
    }

    /** アクティビティ開始時にウィンドウを可視状態に設定します */
    override fun onStart() {
        super.onStart()
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 1)
    }

    /** アクティビティ停止時にウィンドウを非可視状態に設定します */
    override fun onStop() {
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 0)
        super.onStop()
    }

    /** アクティビティ破棄時にリソースをクリーンアップします */
    override fun onDestroy() {
        super.onDestroy()
        mMenuSettingsInitListener?.closeSpinner()
        CallbackBridge.removeGrabListener(binding.mainTouchpad)
        CallbackBridge.removeGrabListener(binding.mainGameRenderView)
        window?.decorView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
        stopVideoBackground()
        ContextExecutor.clearActivity()
        Logger.setLogListener {}
        floatingLogger?.hide()
        floatingLogger = null
        _binding = null
    }

    /** ゲーム内の背景画像またはビデオをリフレッシュして表示します */
    private fun refreshBackground() {
        val mediaFile = BackgroundManager.getBackgroundImage(BackgroundType.IN_GAME)
        if (mediaFile != null && BackgroundManager.isVideo(mediaFile)) {
            playVideoBackground(mediaFile)
            return
        }
        stopVideoBackground()
        BackgroundManager.setBackgroundImage(this, BackgroundType.IN_GAME, binding.backgroundView, null)
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
            BackgroundManager.setBackgroundImage(this, BackgroundType.IN_GAME, binding.backgroundView, null)
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

    /** 設定変更時（画面回転など）に呼び出されます */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mGyroControl?.updateOrientation()
        Tools.updateWindowSize(this)
        binding.mainGameRenderView.refreshSize()
        runOnUiThread { binding.mainControlLayout.refreshControlButtonPositions() }
    }

    /** onResume完了後に呼び出されます。レンダリングサイズを遅延更新します */
    override fun onPostResume() {
        super.onPostResume()
        TaskExecutors.getUIHandler().postDelayed({ binding.mainGameRenderView.refreshSize() }, 500)
    }

    /** アクティビティ結果を受け取ります（コントロール選択ダイアログなど） */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            try {
                binding.mainControlLayout.loadLayout(null as String?)
            } catch (e: IOException) {
                Logging.e("LoadLayout", Tools.printToString(e))
            }
        }
    }

    /** ノッチを無視するかどうかを返します */
    override fun shouldIgnoreNotch(): Boolean = AllSettings.ignoreNotch.getValue()

    /** グローバルレイアウト変更時にキーボードの表示状態を検出し、入力プレビューの表示を切り替えます */
    override fun onGlobalLayout() {
        val rect = Rect()
        val decorView = window?.decorView ?: return
        decorView.getWindowVisibleDisplayFrame(rect)

        val screenHeight = decorView.height
        if (screenHeight * 2 / 3 > rect.bottom) {
            if (!isKeyboardVisible) {
                binding.mainTouchCharInput.addTextChangedListener(mInputWatcher)
                setInputPreview(true)
            }
            isKeyboardVisible = true
        } else if (isKeyboardVisible) {
            binding.mainTouchCharInput.removeTextChangedListener(mInputWatcher)
            setInputPreview(false)
            isKeyboardVisible = false
        }
    }

    /** 入力プレビューの表示/非表示をアニメーションで切り替えます */
    private fun setInputPreview(show: Boolean) {
        mInputPreviewAnim.clearEntries()
        mInputPreviewAnim.apply(
            AnimPlayer.Entry(binding.inputPreviewLayout, if (show) Animations.FadeIn else Animations.FadeOut)
        )
            .setOnStart { binding.inputPreviewLayout.visibility = View.VISIBLE }
            .setOnEnd { binding.inputPreviewLayout.visibility = if (show) View.VISIBLE else View.GONE }
            .start()
    }

    /** キーイベントをディスパッチします。エディタモードとゲームモードで動作が異なります */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isInEditor) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_DOWN) binding.mainControlLayout.askToExit(this)
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        var handleEvent = binding.mainGameRenderView.processKeyEvent(event)
        if (!handleEvent) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && !binding.mainTouchCharInput.isEnabled()) {
                if (event.action != KeyEvent.ACTION_UP) return true
                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt())
                return true
            }
        }
        return handleEvent
    }

    /** 戻るボタン押下時にフローティングログを非表示にします */
    override fun onBackPressed() {
        if (floatingLogger != null && floatingLogger!!.isVisible) {
            floatingLogger!!.hide()
            return
        }
        super.onBackPressed()
    }

    /** メニューボタンクリック時にドロワーを開閉します */
    override fun onClickedMenu() {
        val drawerLayout = binding.mainDrawerOptions
        val navigationView = binding.mainNavigationView
        val open = drawerLayout.isDrawerOpen(navigationView)
        if (open) drawerLayout.closeDrawer(navigationView)
        else drawerLayout.openDrawer(navigationView)
        navigationView.requestLayout()
    }

    /** エディタを終了し、コントロールレイアウトを再読み込みします */
    override fun exitEditor() {
        try {
            binding.mainControlLayout.loadLayout(null as CustomControls?)
            binding.mainControlLayout.setModifiable(false)
            System.gc()
            binding.mainControlLayout.loadLayout(minecraftVersion!!.getControl())
            mGameMenuWrapper?.setVisibility(!binding.mainControlLayout.hasMenuButton())
        } catch (e: IOException) {
            Tools.showError(this, e)
        }
        binding.mainNavigationView.removeAllViews()
        binding.mainNavigationView.addView(mGameMenuBinding!!.root)
        isInEditor = false
    }

    /** サービス接続時にレンダリングを開始します */
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        MinecraftGLSurface.setCurrentVersionName(minecraftVersion!!.getVersionName())
        binding.mainGameRenderView.start(GameService.isActive(), binding.mainTouchpad)
        GameService.setActive(true)
    }

    /** サービス切断時に呼び出されます */
    override fun onServiceDisconnected(name: ComponentName) {}

    /** JVM終了イベントを処理し、サービスを停止してアクティビティを終了します */
    @Subscribe
    fun event(event: JvmExitEvent) {
        runOnUiThread {
            GameService.setActive(false)
            stopService(Intent(this, GameService::class.java))
            if (AllSettings.quitLauncher.getValue()) {
                YLTools.killProcess()
            } else {
                finish()
            }
        }
    }

    /** モーションイベントがマウスキャプチャ対象かどうかを判定します */
    private fun checkCaptureDispatchConditions(event: MotionEvent): Boolean {
        val eventSource = event.source
        return (eventSource and InputDevice.SOURCE_MOUSE_RELATIVE) != 0 ||
                (eventSource and InputDevice.SOURCE_MOUSE) != 0
    }

    /** トラックボールイベントをマウスキャプチャとしてディスパッチします */
    override fun dispatchTrackballEvent(ev: MotionEvent): Boolean {
        return if (checkCaptureDispatchConditions(ev))
            binding.mainGameRenderView.dispatchCapturedPointerEvent(ev)
        else super.dispatchTrackballEvent(ev)
    }

    /**
     * ゲーム内設定メニューの初期化とイベントハンドリングを行う内部クラス。
     */
    private inner class MenuSettingsInitListener(
        private val binding: ViewGameMenuBinding
    ) : View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        CompoundButton.OnCheckedChangeListener,
        OnSpinnerItemSelectedListener<HotbarType>,
        DrawerLayout.DrawerListener {

        /** メニュー設定リスナーを初期化します。すべてのシークバー、スイッチ、ボタンの初期値を設定します */
        init {
            this.binding.hotbarWidth.max = Tools.currentDisplayMetrics.widthPixels / 2
            this.binding.hotbarHeight.max = Tools.currentDisplayMetrics.heightPixels / 2

            MenuUtils.initSeekBarValue(this.binding.resolutionScaler, AllSettings.resolutionRatio.getValue(), this.binding.resolutionScalerValue, "%")
            this.binding.resolutionScalerPreview.text = VideoSettingsFragment.getResolutionRatioPreview(resources, AllSettings.resolutionRatio.getValue())
            MenuUtils.initSeekBarValue(this.binding.timeLongPressTrigger, AllSettings.timeLongPressTrigger.getValue(), this.binding.timeLongPressTriggerValue, "ms")
            MenuUtils.initSeekBarValue(this.binding.mouseSpeed, AllSettings.mouseSpeed.getValue(), this.binding.mouseSpeedValue, "%")
            MenuUtils.initSeekBarValue(this.binding.gyroSensitivity, AllSettings.gyroSensitivity.getValue(), this.binding.gyroSensitivityValue, "%")
            MenuUtils.initSeekBarValue(this.binding.hotbarHeight, AllSettings.hotbarHeight.value.getValue(), this.binding.hotbarHeightValue, "px")
            MenuUtils.initSeekBarValue(this.binding.hotbarWidth, AllSettings.hotbarWidth.value.getValue(), this.binding.hotbarWidthValue, "px")

            this.binding.openMemoryInfo.isChecked = AllSettings.gameMenuShowMemory.getValue()
            this.binding.openFpsInfo.isChecked = AllSettings.gameMenuShowFPS.getValue()
            this.binding.disableGestures.isChecked = AllSettings.disableGestures.getValue()
            this.binding.disableDoubleTap.isChecked = AllSettings.disableDoubleTap.getValue()
            this.binding.enableGyro.isChecked = AllSettings.enableGyro.getValue()
            this.binding.gyroInvertX.isChecked = AllSettings.gyroInvertX.getValue()
            this.binding.gyroInvertY.isChecked = AllSettings.gyroInvertY.getValue()

            refreshLayoutVisible(this.binding.timeLongPressTriggerLayout, !AllSettings.disableGestures.getValue())
            refreshLayoutVisible(this.binding.gyroLayout, AllSettings.enableGyro.getValue())

            this.binding.forceClose.setOnClickListener(this)
            this.binding.logOutput.setOnClickListener(this)
            this.binding.sendCustomKey.setOnClickListener(this)
            this.binding.hostServer.setOnClickListener(this)
            this.binding.openMemoryInfo.setOnCheckedChangeListener(this)
            this.binding.openMemoryInfoLayout.setOnClickListener(this)
            this.binding.openFpsInfo.setOnCheckedChangeListener(this)
            this.binding.openFpsInfoLayout.setOnClickListener(this)

            this.binding.resolutionScaler.setOnSeekBarChangeListener(this)
            this.binding.resolutionScalerRemove.setOnClickListener(this)
            this.binding.resolutionScalerAdd.setOnClickListener(this)

            this.binding.disableGestures.setOnCheckedChangeListener(this)
            this.binding.disableGesturesLayout.setOnClickListener(this)
            this.binding.disableDoubleTap.setOnCheckedChangeListener(this)
            this.binding.disableDoubleTapLayout.setOnClickListener(this)

            this.binding.timeLongPressTrigger.setOnSeekBarChangeListener(this)
            this.binding.timeLongPressTriggerRemove.setOnClickListener(this)
            this.binding.timeLongPressTriggerAdd.setOnClickListener(this)

            this.binding.mouseSpeed.setOnSeekBarChangeListener(this)
            this.binding.mouseSpeedRemove.setOnClickListener(this)
            this.binding.mouseSpeedAdd.setOnClickListener(this)

            this.binding.customMouse.setOnClickListener(this)
            this.binding.replacementCustomcontrol.setOnClickListener(this)
            this.binding.editControl.setOnClickListener(this)

            this.binding.enableGyro.setOnCheckedChangeListener(this)
            this.binding.enableGyroLayout.setOnClickListener(this)

            this.binding.gyroSensitivity.setOnSeekBarChangeListener(this)
            this.binding.gyroSensitivityRemove.setOnClickListener(this)
            this.binding.gyroSensitivityAdd.setOnClickListener(this)

            this.binding.gyroInvertX.setOnCheckedChangeListener(this)
            this.binding.gyroInvertXLayout.setOnClickListener(this)
            this.binding.gyroInvertY.setOnCheckedChangeListener(this)
            this.binding.gyroInvertYLayout.setOnClickListener(this)

            val hotbarTypeAdapter = ObjectSpinnerAdapter<HotbarType>(
                this.binding.hotbarType
            ) { hotbarType -> getString(hotbarType.nameId) }
            hotbarTypeAdapter.setItems(HotbarType.entries.toMutableList())
            this.binding.hotbarType.setSpinnerAdapter(hotbarTypeAdapter)
            this.binding.hotbarType.setIsFocusable(true)
            this.binding.hotbarType.setOnSpinnerItemSelectedListener(this)
            this.binding.hotbarType.selectItemByIndex(HotbarUtils.getCurrentTypeIndex())

            this.binding.hotbarHeight.setOnSeekBarChangeListener(this)
            this.binding.hotbarHeightRemove.setOnClickListener(this)
            this.binding.hotbarHeightAdd.setOnClickListener(this)
            this.binding.hotbarWidth.setOnSeekBarChangeListener(this)
            this.binding.hotbarWidthRemove.setOnClickListener(this)
            this.binding.hotbarWidthAdd.setOnClickListener(this)
        }

        /** カスタムキー送信ダイアログを表示します */
        private fun dialogSendCustomKey() {
            keyboardDialog!!
                .setOnMultiKeycodeSelectListener { selectedKeycodes ->
                    Task.runTask {
                        selectedKeycodes.forEach { keycode -> sendKeyPress(keycode, true) }
                        null
                    }.ended {
                        try { Thread.sleep(50) } catch (_: InterruptedException) {}
                        selectedKeycodes.forEach { keycode -> sendKeyPress(keycode, false) }
                    }.execute()
                }.show()
        }

        /** LWJGLキーコードに変換してキー入力を送信します */
        private fun sendKeyPress(keycode: Int, isDown: Boolean) {
            println("Test keycode: $keycode")
            val lwjglKeycode = EfficientAndroidLWJGLKeycode.getValueByIndex(keycode).toInt()
            println("Test lwjglKeycode: $lwjglKeycode")
            if (keycode >= LwjglGlfwKeycode.GLFW_KEY_UNKNOWN) {
                CallbackBridge.sendKeyPress(lwjglKeycode, CallbackBridge.getCurrentMods(), isDown)
                CallbackBridge.setModifiers(lwjglKeycode, isDown)
            }
        }

        /** コントロール置き換えダイアログを表示します */
        private fun replacementCustomControls() {
            val dialog = SelectControlsDialog(
                this@MainActivity,
                object : SelectControlsDialog.SelectedListener {
                    override fun onSelected(file: File) {
                        try {
                            this@MainActivity.binding.mainControlLayout.loadLayout(file.absolutePath)
                            mGameMenuWrapper?.setVisibility(!this@MainActivity.binding.mainControlLayout.hasMenuButton())
                        } catch (_: IOException) {}
                    }
                }
            )
            dialog.setTitleText(R.string.replacement_customcontrol)
            dialog.show()
        }

        /** コントロールエディタを開きます */
        private fun openCustomControls() {
            this@MainActivity.binding.mainControlLayout.setModifiable(true)
            this@MainActivity.binding.mainNavigationView.removeAllViews()
            this@MainActivity.binding.mainNavigationView.addView(mControlSettingsBinding!!.root)
            mGameMenuWrapper?.setVisibility(true)
            isInEditor = true
        }

        /** 各種ボタンのクリックイベントを処理します */
        override fun onClick(v: View) {
            when (v) {
                binding.forceClose -> YLTools.dialogForceClose(this@MainActivity)
                binding.logOutput -> floatingLogger?.toggle()
                binding.sendCustomKey -> dialogSendCustomKey()
                binding.hostServer -> {
                    startActivity(Intent(this@MainActivity, HostServerActivity::class.java))
                    this@MainActivity.binding.mainDrawerOptions.closeDrawers()
                }
                binding.openMemoryInfoLayout -> MenuUtils.toggleSwitchState(binding.openMemoryInfo)
                binding.openFpsInfoLayout -> MenuUtils.toggleSwitchState(binding.openFpsInfo)
                binding.resolutionScalerRemove -> MenuUtils.adjustSeekbar(binding.resolutionScaler, -1)
                binding.resolutionScalerAdd -> MenuUtils.adjustSeekbar(binding.resolutionScaler, 1)
                binding.disableGesturesLayout -> MenuUtils.toggleSwitchState(binding.disableGestures)
                binding.disableDoubleTapLayout -> MenuUtils.toggleSwitchState(binding.disableDoubleTap)
                binding.timeLongPressTriggerRemove -> MenuUtils.adjustSeekbar(binding.timeLongPressTrigger, -1)
                binding.timeLongPressTriggerAdd -> MenuUtils.adjustSeekbar(binding.timeLongPressTrigger, 1)
                binding.mouseSpeedRemove -> MenuUtils.adjustSeekbar(binding.mouseSpeed, -1)
                binding.mouseSpeedAdd -> MenuUtils.adjustSeekbar(binding.mouseSpeed, 1)
                binding.customMouse -> SelectMouseDialog(
                    this@MainActivity,
                    object : SelectMouseDialog.MouseSelectedListener {
                        override fun onSelectedListener() {
                            this@MainActivity.binding.mainTouchpad.updateMouseDrawable()
                            this@MainActivity.binding.mainGameRenderView.updateMouseDrawable()
                        }
                    }
                ).show()
                binding.replacementCustomcontrol -> replacementCustomControls()
                binding.editControl -> openCustomControls()
                binding.enableGyroLayout -> MenuUtils.toggleSwitchState(binding.enableGyro)
                binding.gyroSensitivityRemove -> MenuUtils.adjustSeekbar(binding.gyroSensitivity, -1)
                binding.gyroSensitivityAdd -> MenuUtils.adjustSeekbar(binding.gyroSensitivity, 1)
                binding.gyroInvertXLayout -> MenuUtils.toggleSwitchState(binding.gyroInvertX)
                binding.gyroInvertYLayout -> MenuUtils.toggleSwitchState(binding.gyroInvertY)
                binding.hotbarWidthRemove -> MenuUtils.adjustSeekbar(binding.hotbarWidth, -1)
                binding.hotbarWidthAdd -> MenuUtils.adjustSeekbar(binding.hotbarWidth, 1)
                binding.hotbarHeightRemove -> MenuUtils.adjustSeekbar(binding.hotbarHeight, -1)
                binding.hotbarHeightAdd -> MenuUtils.adjustSeekbar(binding.hotbarHeight, 1)
            }
        }

        /** シークバーの進捗が変更されたときに呼び出されます */
        @SuppressLint("SetTextI18n")
        override fun onProgressChanged(s: SeekBar, progress: Int, fromUser: Boolean) {
            updateSeekbarValue(s, !fromUser)
        }

        override fun onStartTrackingTouch(s: SeekBar) {}
        override fun onStopTrackingTouch(s: SeekBar) { updateSeekbarValue(s, true) }

        /** シークバーの値を更新し、必要に応じて設定を保存します */
        private fun updateSeekbarValue(seekbar: SeekBar?, saveValue: Boolean) {
            val progress = seekbar?.progress ?: 0

            when (seekbar) {
                binding.resolutionScaler -> {
                    if (saveValue) AllSettings.resolutionRatio.put(progress).save()
                    MenuUtils.updateSeekbarValue(progress, binding.resolutionScalerValue, "%")
                    binding.resolutionScalerPreview.text = VideoSettingsFragment.getResolutionRatioPreview(resources, progress)
                    AllStaticSettings.scaleFactor = progress / 100f
                    this@MainActivity.binding.mainGameRenderView.refreshSize()
                }
                binding.timeLongPressTrigger -> {
                    if (saveValue) AllSettings.timeLongPressTrigger.put(progress).save()
                    MenuUtils.updateSeekbarValue(progress, binding.timeLongPressTriggerValue, "ms")
                    AllStaticSettings.timeLongPressTrigger = progress
                }
                binding.mouseSpeed -> {
                    if (saveValue) AllSettings.mouseSpeed.put(progress).save()
                    MenuUtils.updateSeekbarValue(progress, binding.mouseSpeedValue, "%")
                }
                binding.gyroSensitivity -> {
                    if (saveValue) AllSettings.gyroSensitivity.put(progress).save()
                    MenuUtils.updateSeekbarValue(progress, binding.gyroSensitivityValue, "%")
                    AllStaticSettings.gyroSensitivity = progress
                }
                binding.hotbarWidth -> {
                    if (saveValue) AllSettings.hotbarWidth.value.put(progress).save()
                    MenuUtils.updateSeekbarValue(progress, binding.hotbarWidthValue, "px")
                    EventBus.getDefault().post(HotbarChangeEvent(progress, binding.hotbarHeight.progress))
                }
                binding.hotbarHeight -> {
                    if (saveValue) AllSettings.hotbarHeight.value.put(progress).save()
                    MenuUtils.updateSeekbarValue(progress, binding.hotbarHeightValue, "px")
                    EventBus.getDefault().post(HotbarChangeEvent(binding.hotbarWidth.progress, progress))
                }
            }
        }

        /** チェックボックスの状態変更を処理し、対応する設定を保存します */
        override fun onCheckedChanged(v: CompoundButton, isChecked: Boolean) {
            when (v) {
                binding.openMemoryInfo -> {
                    AllSettings.gameMenuShowMemory.put(isChecked).save()
                    mGameMenuWrapper?.refreshSettingsState()
                }
                binding.openFpsInfo -> {
                    AllSettings.gameMenuShowFPS.put(isChecked).save()
                    mGameMenuWrapper?.refreshSettingsState()
                }
                binding.disableGestures -> {
                    refreshLayoutVisible(binding.timeLongPressTriggerLayout, !isChecked)
                    AllSettings.disableGestures.put(isChecked).save()
                }
                binding.disableDoubleTap -> {
                    AllSettings.disableDoubleTap.put(isChecked).save()
                    AllStaticSettings.disableDoubleTap = isChecked
                }
                binding.enableGyro -> {
                    refreshLayoutVisible(binding.gyroLayout, isChecked)
                    AllSettings.enableGyro.put(isChecked).save()
                    AllStaticSettings.enableGyro = isChecked
                    mGyroControl?.updateOrientation()
                    if (isChecked) mGyroControl?.enable() else mGyroControl?.disable()
                }
                binding.gyroInvertX -> {
                    AllSettings.gyroInvertX.put(isChecked).save()
                    AllStaticSettings.gyroInvertX = isChecked
                }
                binding.gyroInvertY -> {
                    AllSettings.gyroInvertY.put(isChecked).save()
                    AllStaticSettings.gyroInvertY = isChecked
                }
            }
        }

        /** レイアウトの表示/非表示を切り替えます */
        private fun refreshLayoutVisible(view: View, visible: Boolean) {
            view.visibility = if (visible) View.VISIBLE else View.GONE
        }

        /** ホットバータイプの選択が変更されたときに呼び出されます */
        override fun onItemSelected(i: Int, t: HotbarType?, i1: Int, t1: HotbarType) {
            when (t1) {
                HotbarType.AUTO -> {
                    binding.hotbarWidthLayout.visibility = View.GONE
                    binding.hotbarHeightLayout.visibility = View.GONE
                }
                HotbarType.MANUALLY -> {
                    binding.hotbarWidthLayout.visibility = View.VISIBLE
                    binding.hotbarHeightLayout.visibility = View.VISIBLE
                    binding.hotbarWidth.progress = AllSettings.hotbarWidth.value.getValue()
                    binding.hotbarHeight.progress = AllSettings.hotbarHeight.value.getValue()
                }
            }
            AllSettings.hotbarType.put(t1.valueName).save()
            EventBus.getDefault().post(RefreshHotbarEvent())
        }

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
        override fun onDrawerOpened(drawerView: View) {}
        override fun onDrawerClosed(drawerView: View) {}

        /** ドロワーの状態が変更されたときに呼び出されます */
        override fun onDrawerStateChanged(newState: Int) {
            closeSpinner()
        }

        /** ホットバータイプのスピナーを閉じます */
        fun closeSpinner() {
            binding.hotbarType.dismiss()
        }
    }
}