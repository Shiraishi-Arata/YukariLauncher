/**
 * カスタムコントロールの編集を行うアクティビティ。
 * コントロールレイアウトの編集・保存を提供します。
 */
package com.arata.yukarilauncher.ui.activity

import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.drawerlayout.widget.DrawerLayout
import com.arata.yukarilauncher.databinding.ActivityCustomControlsBinding
import com.arata.yukarilauncher.databinding.ViewControlMenuBinding
import com.arata.yukarilauncher.feature.background.BackgroundManager
import com.arata.yukarilauncher.feature.background.BackgroundType
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.ui.subassembly.menu.ControlMenu
import com.arata.yukarilauncher.ui.subassembly.view.GameMenuViewWrapper
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.EditorExitable
import java.io.File
import java.io.IOException

/**
 * カスタムコントロールの編集を行うアクティビティ。
 * コントロールレイアウトの編集・保存を提供します。
 */
class CustomControlsActivity : BaseActivity(), EditorExitable {

    companion object {
        const val BUNDLE_CONTROL_PATH = "control_path"
    }

    private lateinit var binding: ActivityCustomControlsBinding
    private var mControlPath: String? = null
    private var isVideoBackgroundPlaying = false

    /** アクティビティ作成時に呼び出されます。レイアウトの初期化、コントロールの読み込み、バックキーハンドラを設定します */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseBundle()
        binding = ActivityCustomControlsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val controlLayout = binding.customctrlControllayout
        val drawerLayout = binding.customctrlDrawerlayout
        val drawerNavigationView = binding.customctrlNavigationView

        GameMenuViewWrapper(this, {
            val open = drawerLayout.isDrawerOpen(drawerNavigationView)
            if (open) drawerLayout.closeDrawer(drawerNavigationView)
            else drawerLayout.openDrawer(drawerNavigationView)
        }, false).setVisibility(true)

        refreshBackground()

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        drawerLayout.setScrimColor(Color.TRANSPARENT)

        val controlMenuBinding = ViewControlMenuBinding.inflate(layoutInflater)
        ControlMenu(this, this, controlMenuBinding, controlLayout, true)

        drawerNavigationView.addView(controlMenuBinding.root)
        controlLayout.setModifiable(true)
        try {
            if (mControlPath == null) controlLayout.loadLayout(null as String?)
            else controlLayout.loadLayout(mControlPath!!)
        } catch (e: IOException) {
            Tools.showError(this, e)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.customctrlControllayout.askToExit(this@CustomControlsActivity)
            }
        })
    }

    /** インテントからコントロールパスを解析します */
    private fun parseBundle() {
        val bundle = intent.extras
        if (bundle != null) {
            mControlPath = bundle.getString(BUNDLE_CONTROL_PATH)
        }
    }

    /** ノッチを無視するかどうかを返します */
    override fun shouldIgnoreNotch(): Boolean = AllSettings.ignoreNotch.getValue()

    /** エディタを終了し、アクティビティを閉じます */
    override fun exitEditor() = finish()

    /** アクティビティ再開時にビデオ背景を再生します */
    override fun onResume() {
        super.onResume()
        if (isVideoBackgroundPlaying) {
            binding.backgroundVideoView.start()
        }
    }

    /** アクティビティ一時停止時にビデオ背景を一時停止します */
    override fun onPause() {
        super.onPause()
        if (isVideoBackgroundPlaying && binding.backgroundVideoView.isPlaying) {
            binding.backgroundVideoView.pause()
        }
    }

    /** アクティビティ破棄時にビデオ背景を停止します */
    override fun onDestroy() {
        super.onDestroy()
        stopVideoBackground()
    }

    /** 背景画像またはビデオをリフレッシュして表示します */
    private fun refreshBackground() {
        val mediaFile = BackgroundManager.getBackgroundImage(BackgroundType.CUSTOM_CONTROLS)
        if (mediaFile != null && BackgroundManager.isVideo(mediaFile)) {
            playVideoBackground(mediaFile)
            return
        }
        stopVideoBackground()
        BackgroundManager.setBackgroundImage(this, BackgroundType.CUSTOM_CONTROLS, binding.backgroundView, null)
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
            BackgroundManager.setBackgroundImage(this, BackgroundType.CUSTOM_CONTROLS, binding.backgroundView, null)
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
}
