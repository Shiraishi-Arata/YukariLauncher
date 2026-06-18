package com.arata.yukarilauncher.ui.fragment.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebStorage
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.SettingsFragmentLauncherBinding
import com.arata.yukarilauncher.event.single.MainBackgroundChangeEvent
import com.arata.yukarilauncher.event.single.PageOpacityChangeEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import com.arata.yukarilauncher.feature.discord.DiscordAccount
import com.arata.yukarilauncher.feature.discord.DiscordPrefs
import com.arata.yukarilauncher.feature.discord.DiscordProfileCard
import com.arata.yukarilauncher.feature.discord.DiscordRpcManager
import com.arata.yukarilauncher.feature.update.UpdateUtils
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.ui.fragment.CustomBackgroundFragment
import com.arata.yukarilauncher.ui.fragment.FragmentWithAnim
import com.arata.yukarilauncher.ui.fragment.showDiscordLoginDialog
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
    private var customStatusListener: ((String?) -> Unit)? = null
    private var customButtonTextWatchers: List<android.text.TextWatcher>? = null
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

        setupDiscordRpc()
    }

    private fun setupDiscordRpc() {
        val context = requireContext()

        binding.discordRpcSwitch.isChecked = DiscordPrefs.isRpcEnabled()
        binding.discordRpcSwitch.setOnCheckedChangeListener { _, isChecked ->
            DiscordPrefs.setRpcEnabled(isChecked)
            binding.discordAccountSection.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                startRpcService()
            } else {
                stopRpcService()
            }
        }
        binding.discordAccountSection.visibility = if (DiscordPrefs.isRpcEnabled()) View.VISIBLE else View.GONE

        refreshDiscordAccounts()

        // カスタムステータス変更時にアカウントリストを更新
        customStatusListener = { _ ->
            refreshDiscordAccounts()
        }
        customStatusListener?.let { DiscordRpcManager.addCustomStatusListener(it) }

        // カスタムRPCボタンの設定
        binding.customButtonLabel.setText(DiscordPrefs.getCustomButtonLabel())
        binding.customButtonUrl.setText(DiscordPrefs.getCustomButtonUrl())

        val buttonSaveHandler = Handler(Looper.getMainLooper())
        val buttonSaveRunnable = Runnable {
            DiscordPrefs.setCustomButtonLabel(binding.customButtonLabel.text?.toString() ?: "")
            DiscordPrefs.setCustomButtonUrl(binding.customButtonUrl.text?.toString() ?: "")
            DiscordRpcManager.reconnect()
        }

        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                buttonSaveHandler.removeCallbacks(buttonSaveRunnable)
                buttonSaveHandler.postDelayed(buttonSaveRunnable, 1000L)
            }
        }
        binding.customButtonLabel.addTextChangedListener(watcher)
        binding.customButtonUrl.addTextChangedListener(watcher)
        customButtonTextWatchers = listOf(watcher)
    }

    private fun refreshDiscordAccounts() {
        val context = requireContext()
        val accounts = DiscordPrefs.getAccounts()

        binding.discordAccountList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = DiscordAccountAdapter(
                accounts = accounts,
                onLoginClick = { showAddTokenDialog() },
                onLogoutClick = {
                    AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)
                        .setTitle(R.string.setting_discord_logout)
                        .setMessage(R.string.setting_discord_logout_confirm)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            DiscordPrefs.clearLoginData()
                            try { WebStorage.getInstance().deleteAllData() } catch (_: Exception) {}
                            stopRpcService()
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()
                }
            )
        }
    }

    private fun showAddTokenDialog() {
        val activity = requireActivity()
        showDiscordLoginDialog(activity) {
            refreshDiscordAccounts()
        }
    }

    private fun startRpcService() {
        DiscordRpcManager.connect()
    }

    private fun stopRpcService() {
        DiscordRpcManager.disconnect()
    }

    private fun restartRpcService() {
        DiscordRpcManager.reconnect()
    }

    private class DiscordAccountAdapter(
        private val accounts: List<DiscordAccount>,
        private val onLoginClick: (() -> Unit)? = null,
        private val onLogoutClick: (() -> Unit)? = null
    ) : RecyclerView.Adapter<DiscordAccountAdapter.ViewHolder>() {

        private val darkColorScheme = androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF8D42EB),
            onPrimary = Color(0xFFECEBFF),
            surface = Color(0xFF06071B),
            onSurface = Color(0xFFECEBFF),
            surfaceContainerHigh = Color(0xFF161330),
            surfaceContainer = Color(0xFF191733),
            primaryContainer = Color(0xFF191733),
            onPrimaryContainer = Color(0xFFECEBFF),
            secondaryContainer = Color(0xFF251A48)
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val composeView = ComposeView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            return ViewHolder(composeView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (accounts.isEmpty()) {
                holder.composeView.setContent {
                    androidx.compose.material3.MaterialTheme(colorScheme = darkColorScheme) {
                        DiscordProfileCard(
                            account = null,
                            customStatus = null,
                            isSelected = false,
                            showLogin = true,
                            onLoginClick = onLoginClick
                        )
                    }
                }
                return
            }
            val account = accounts[position]
            val isSelected = account.id == DiscordPrefs.getSelectedAccountId()
            val status = if (isSelected) DiscordRpcManager.getCustomStatus() else null
            val hasSelectedAccount = DiscordPrefs.getSelectedAccountId() != null
            holder.composeView.setContent {
                androidx.compose.material3.MaterialTheme(colorScheme = darkColorScheme) {
                    DiscordProfileCard(
                        account = account,
                        customStatus = status,
                        isSelected = isSelected,
                        showLogin = !hasSelectedAccount,
                        onLoginClick = if (!hasSelectedAccount) onLoginClick else null,
                        onLogoutClick = if (isSelected) onLogoutClick else null
                    )
                }
            }
        }

        override fun getItemCount() = if (accounts.isEmpty()) 1 else accounts.size

        class ViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)
    }


    /**
     * ビュー破棄時にブラー更新ハンドラーのコールバックを削除します。
     */
    override fun onResume() {
        super.onResume()
        refreshDiscordAccounts()
    }

    override fun onDestroyView() {
        blurUpdateHandler.removeCallbacks(blurUpdateRunnable)
        customStatusListener?.let { DiscordRpcManager.removeCustomStatusListener(it) }
        customStatusListener = null
        customButtonTextWatchers?.let { watchers ->
            watchers.forEach {
                binding.customButtonLabel.removeTextChangedListener(it)
                binding.customButtonUrl.removeTextChangedListener(it)
            }
        }
        customButtonTextWatchers = null
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