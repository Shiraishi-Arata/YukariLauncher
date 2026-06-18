package com.arata.yukarilauncher.ui.fragment.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebStorage
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.SettingsFragmentLauncherBinding
import com.arata.yukarilauncher.event.single.MainBackgroundChangeEvent
import com.arata.yukarilauncher.event.single.PageOpacityChangeEvent
import com.arata.yukarilauncher.feature.discord.DiscordAccount
import com.arata.yukarilauncher.feature.discord.DiscordPrefs
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import org.greenrobot.eventbus.EventBus

/**
 * ランチャー設定フラグメント
 */
class LauncherSettingsFragment() : AbstractSettingsFragment(R.layout.settings_fragment_launcher, SettingCategory.LAUNCHER) {
    private lateinit var binding: SettingsFragmentLauncherBinding
    private var parentFragment: FragmentWithAnim? = null
    private val blurUpdateHandler = Handler(Looper.getMainLooper())
    private var customStatusListener: ((String?) -> Unit)? = null
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

        binding.discordAddTokenLayout.setOnClickListener {
            val selectedId = DiscordPrefs.getSelectedAccountId()
            if (selectedId != null) {
                AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)
                    .setTitle(R.string.setting_discord_logout)
                    .setMessage(R.string.setting_discord_logout_confirm)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        DiscordPrefs.clearLoginData()
                        // WebViewのlocalStorageも消去（次回ログイン時に古いセッションが復元されるのを防止）
                        try { WebStorage.getInstance().deleteAllData() } catch (_: Exception) {}
                        refreshDiscordAccounts()
                        stopRpcService()
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
            } else {
                showAddTokenDialog()
            }
        }

        refreshDiscordAccounts()

        // カスタムステータス変更時にアカウントリストを更新
        customStatusListener = { _ ->
            refreshDiscordAccounts()
        }
        customStatusListener?.let { DiscordRpcManager.addCustomStatusListener(it) }
    }

    private fun refreshDiscordAccounts() {
        val context = requireContext()
        val accounts = DiscordPrefs.getAccounts()
        val selectedId = DiscordPrefs.getSelectedAccountId()

        // Update login/logout button text based on account state
        if (selectedId != null) {
            binding.discordAddTokenTitle.setText(R.string.setting_discord_logout)
            binding.discordAddTokenSummary.setText(R.string.setting_discord_logout_desc)
        } else {
            binding.discordAddTokenTitle.setText(R.string.setting_discord_add_token)
            binding.discordAddTokenSummary.setText(R.string.setting_discord_add_token_desc)
        }

        binding.discordAccountList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = DiscordAccountAdapter(accounts)
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
        private val accounts: List<DiscordAccount>
    ) : RecyclerView.Adapter<DiscordAccountAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_discord_account, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val account = accounts[position]
            val isSelected = account.id == DiscordPrefs.getSelectedAccountId()
            holder.displayName.text = account.displayName

            val baseUsername = if (account.discriminator != "0") "${account.username}#${account.discriminator}" else "@${account.username}"
            val status = if (isSelected) DiscordRpcManager.getCustomStatus() else null
            holder.username.text = if (status != null) "$baseUsername • $status" else baseUsername

            val avatarUrl = account.avatarUrl
            if (avatarUrl != null) {
                Glide.with(holder.avatar)
                    .load(avatarUrl)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.ic_discord)
                    .error(R.drawable.ic_discord)
                    .into(holder.avatar)
            } else {
                holder.avatar.setImageResource(R.drawable.ic_discord)
            }

            val bannerUrl = account.bannerUrl
            if (bannerUrl != null) {
                Glide.with(holder.banner)
                    .load(bannerUrl)
                    .placeholder(R.drawable.discord_profile_banner)
                    .error(R.drawable.discord_profile_banner)
                    .into(holder.banner)
            } else {
                holder.banner.setImageResource(R.drawable.discord_profile_banner)
            }
        }

        override fun getItemCount() = accounts.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val banner: ImageView = view.findViewById(R.id.profile_banner)
            val avatar: ImageView = view.findViewById(R.id.account_avatar)
            val displayName: TextView = view.findViewById(R.id.account_display_name)
            val username: TextView = view.findViewById(R.id.account_username)
        }
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