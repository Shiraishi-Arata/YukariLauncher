package com.arata.yukarilauncher.ui.fragment.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.SettingsFragmentLauncherBinding
import com.arata.yukarilauncher.event.single.MainBackgroundChangeEvent
import com.arata.yukarilauncher.event.single.PageOpacityChangeEvent
import com.arata.yukarilauncher.feature.update.UpdateUtils
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.ui.fragment.CustomBackgroundFragment
import com.arata.yukarilauncher.ui.fragment.FragmentWithAnim
import com.arata.yukarilauncher.ui.fragment.settings.wrapper.BaseSettingsWrapper
import com.arata.yukarilauncher.ui.fragment.settings.wrapper.ListSettingsWrapper
import com.arata.yukarilauncher.ui.fragment.settings.wrapper.SeekBarSettingsWrapper
import com.arata.yukarilauncher.ui.fragment.settings.wrapper.SwitchSettingsWrapper
import com.arata.yukarilauncher.utils.CleanUpCache.Companion.start
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.ui.activity.LauncherActivity
import org.greenrobot.eventbus.EventBus

/**
 * ランチャー設定フラグメント
 */
class LauncherSettingsFragment() : AbstractSettingsFragment(R.layout.settings_fragment_launcher, SettingCategory.LAUNCHER) {
    private lateinit var binding: SettingsFragmentLauncherBinding
    private var parentFragment: FragmentWithAnim? = null
    private val blurUpdateHandler = Handler(Looper.getMainLooper())
    private val blurUpdateRunnable = Runnable {
        EventBus.getDefault().post(MainBackgroundChangeEvent())
    }

    constructor(parentFragment: FragmentWithAnim?) : this() {
        this.parentFragment = parentFragment
    }

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsFragmentLauncherBinding.inflate(layoutInflater)
        return binding.root
    }

    /**
     * ビュー作成後の初期化処理を行います。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()

        SwitchSettingsWrapper(
            context,
            AllSettings.checkLibraries,
            binding.checkLibrariesLayout,
            binding.checkLibraries
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.verifyManifest,
            binding.verifyManifestLayout,
            binding.verifyManifest
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.resourceImageCache,
            binding.resourceImageCacheLayout,
            binding.resourceImageCache
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.addFullResourceName,
            binding.addFullResourceNameLayout,
            binding.addFullResourceName
        )

        ListSettingsWrapper(
            context,
            AllSettings.downloadSource,
            binding.downloadSourceLayout,
            binding.downloadSourceTitle,
            binding.downloadSourceValue,
            R.array.download_source_names, R.array.download_source_values
        )

        SeekBarSettingsWrapper(
            context,
            AllSettings.maxDownloadThreads,
            binding.maxDownloadThreadsLayout,
            binding.maxDownloadThreadsTitle,
            binding.maxDownloadThreadsSummary,
            binding.maxDownloadThreadsValue,
            binding.maxDownloadThreads,
            ""
        )


        BaseSettingsWrapper(
            context,
            binding.customBackgroundLayout
        ) {
            parentFragment?.apply {
                YLTools.swapFragmentWithAnim(
                    this,
                    CustomBackgroundFragment::class.java,
                    CustomBackgroundFragment.TAG,
                    null
                )
            }
        }

        SeekBarSettingsWrapper(
            context,
            AllSettings.customBackgroundBlur,
            binding.customBackgroundBlurLayout,
            binding.customBackgroundBlurTitle,
            binding.customBackgroundBlurSummary,
            binding.customBackgroundBlurValue,
            binding.customBackgroundBlur,
            ""
        ).setOnSeekBarProgressChangeListener {
            blurUpdateHandler.removeCallbacks(blurUpdateRunnable)
            blurUpdateHandler.postDelayed(blurUpdateRunnable, 120L)
        }

        SwitchSettingsWrapper(
            context,
            AllSettings.animation,
            binding.animationLayout,
            binding.animation
        )

        SeekBarSettingsWrapper(
            context,
            AllSettings.animationSpeed,
            binding.animationSpeedLayout,
            binding.animationSpeedTitle,
            binding.animationSpeedSummary,
            binding.animationSpeedValue,
            binding.animationSpeed,
            "ms"
        )

        SeekBarSettingsWrapper(
            context,
            AllSettings.pageOpacity,
            binding.pageOpacityLayout,
            binding.pageOpacityTitle,
            binding.pageOpacitySummary,
            binding.pageOpacityValue,
            binding.pageOpacity,
            "%"
        ).setOnSeekBarProgressChangeListener {
            EventBus.getDefault().post(PageOpacityChangeEvent(it))
        }

        SwitchSettingsWrapper(
            context,
            AllSettings.enableLogOutput,
            binding.enableLogOutputLayout,
            binding.enableLogOutput
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.quitLauncher,
            binding.quitLauncherLayout,
            binding.quitLauncher
        )

        BaseSettingsWrapper(
            context,
            binding.cleanUpCacheLayout
        ) {
            start(context)
        }

        BaseSettingsWrapper(
            context,
            binding.checkUpdateLayout
        ) {
            UpdateUtils.checkDownloadedPackage(context, force = true, ignore = false)
        }

        SwitchSettingsWrapper(
            context,
            AllSettings.acceptPreReleaseUpdates,
            binding.acceptPreReleaseUpdatesLayout,
            binding.acceptPreReleaseUpdates
        )

        val notificationPermissionRequest = SwitchSettingsWrapper(
            context,
            AllSettings.notificationPermissionRequest,
            binding.notificationPermissionRequestLayout,
            binding.notificationPermissionRequest
        )
        setupNotificationRequestPreference(notificationPermissionRequest)
    }


    /**
     * ビュー破棄時にブラー更新ハンドラーのコールバックを削除します。
     */
    override fun onDestroyView() {
        blurUpdateHandler.removeCallbacks(blurUpdateRunnable)
        super.onDestroyView()
    }

    /**
     * スライドインアニメーションを実行します。
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.root, Animations.BounceInDown))
    }

    /**
     * 通知権限設定を初期化する
     */
    /**
     * 通知権限設定を初期化します。
     */
    private fun setupNotificationRequestPreference(notificationPermissionRequest: SwitchSettingsWrapper) {
        val activity = requireActivity()
        if (activity is LauncherActivity) {
            if (YLTools.checkForNotificationPermission()) notificationPermissionRequest.setGone()
            notificationPermissionRequest.switchView.setOnCheckedChangeListener { _, _ ->
                activity.askForNotificationPermission {
                    notificationPermissionRequest.mainView.visibility = View.GONE
                }
            }
        } else {
            notificationPermissionRequest.mainView.visibility = View.GONE
        }
    }
}