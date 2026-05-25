package com.arata.yukarilauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.arata.yukarilauncher.InfoCenter
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentLauncherBinding
import com.arata.yukarilauncher.event.single.AccountUpdateEvent
import com.arata.yukarilauncher.event.single.LaunchGameEvent
import com.arata.yukarilauncher.event.single.RefreshVersionsEvent
import com.arata.yukarilauncher.event.single.RefreshVersionsEvent.MODE.END
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.feature.version.VersionInfo
import com.arata.yukarilauncher.feature.version.VersionsManager
import com.arata.yukarilauncher.feature.version.utils.VersionIconUtils
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.subassembly.account.AccountViewWrapper
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.anim.ViewAnimUtils
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.task.ProgressKeeper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/** ランチャーのメインメニューを表示するフラグメント。バージョン選択、プレイ、設定への導線を提供する。 */
class MainMenuFragment : FragmentWithAnim(R.layout.fragment_launcher) {

    companion object {
        /** フラグメントのタグ。 */
        const val TAG = "MainMenuFragment"
    }

    /** ビューバインディングインスタンス。 */
    private var binding: FragmentLauncherBinding? = null
    /** アカウント情報表示ラッパー。 */
    private var accountViewWrapper: AccountViewWrapper? = null

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        binding = FragmentLauncherBinding.inflate(layoutInflater)
        accountViewWrapper = AccountViewWrapper(this, binding!!.viewAccount)
        accountViewWrapper!!.refreshAccountInfo()
        return binding?.root
    }

    /** ビュー作成後の初期化。各ボタンのクリックリスナーとバージョン情報の更新を行う。 */
    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        binding?.customControlButton?.setOnClickListener {
            YLTools.swapFragmentWithAnim(this, ControlButtonFragment::class.java, ControlButtonFragment.TAG, null)
        }
        binding?.openMainDirButton?.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(FilesFragment.BUNDLE_LIST_PATH, PathManager.DIR_GAME_HOME)
            YLTools.swapFragmentWithAnim(this, FilesFragment::class.java, FilesFragment.TAG, bundle)
        }
        binding?.installJarButton?.setOnClickListener { runInstallerWithConfirmation(false) }
        binding?.installJarButton?.setOnLongClickListener {
            runInstallerWithConfirmation(true)
            true
        }
        binding?.shareLogsButton?.setOnClickListener { YLTools.shareLogs(requireActivity()) }
        binding?.hostServerButton?.setOnClickListener {
            YLTools.swapFragmentWithAnim(this, HostServerFragment::class.java, HostServerFragment.TAG, null)
        }

        binding?.version?.setOnClickListener {
            if (!isTaskRunning()) {
                YLTools.swapFragmentWithAnim(this, VersionsListFragment::class.java, VersionsListFragment.TAG, null)
            } else {
                ViewAnimUtils.setViewAnim(binding!!.version!!, Animations.Shake)
                TaskExecutors.runInUIThread {
                    Toast.makeText(requireContext(), R.string.version_manager_task_in_progress, Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding?.managerProfileButton?.setOnClickListener {
            if (!isTaskRunning()) {
                ViewAnimUtils.setViewAnim(binding!!.managerProfileButton!!, Animations.Pulse)
                YLTools.swapFragmentWithAnim(this, VersionManagerFragment::class.java, VersionManagerFragment.TAG, null)
            } else {
                ViewAnimUtils.setViewAnim(binding!!.managerProfileButton!!, Animations.Shake)
                TaskExecutors.runInUIThread {
                    Toast.makeText(requireContext(), R.string.version_manager_task_in_progress, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding?.playButton?.setOnClickListener { EventBus.getDefault().post(LaunchGameEvent()) }

        binding?.versionName?.isSelected = true
        binding?.versionInfo?.isSelected = true

        refreshCurrentVersion()
    }

    /** 現在選択中のバージョン情報をUIに反映する。 */
    private fun refreshCurrentVersion() {
        val version = VersionsManager.getCurrentVersion()

        val versionInfoVisibility: Int
        if (version != null) {
            binding?.versionName?.text = version.getVersionName()
            val versionInfo = version.getVersionInfo()
            if (versionInfo != null) {
                binding?.versionInfo?.text = versionInfo.getInfoString()
                versionInfoVisibility = View.VISIBLE
            } else {
                versionInfoVisibility = View.GONE
            }

            VersionIconUtils(version).start(binding!!.versionIcon)
            binding?.managerProfileButton?.visibility = View.VISIBLE
        } else {
            binding?.versionName?.setText(R.string.version_no_versions)
            binding?.managerProfileButton?.visibility = View.GONE
            versionInfoVisibility = View.GONE
        }
        binding?.versionInfo?.visibility = versionInfoVisibility
    }

    /** バージョン一覧の更新完了イベントを受信し、UIを更新する。 */
    @Subscribe
    fun event(event: RefreshVersionsEvent) {
        if (event.mode == END) {
            TaskExecutors.runInUIThread { refreshCurrentVersion() }
        }
    }

    /** アカウント情報の更新イベントを受信し、アカウント表示を更新する。 */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun event(event: AccountUpdateEvent) {
        accountViewWrapper?.refreshAccountInfo()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    /** 確認ダイアログ付きでMod/Jarインストーラーを実行する。 */
    private fun runInstallerWithConfirmation(isCustomArgs: Boolean) {
        if (ProgressKeeper.taskCount == 0)
            Tools.installMod(requireActivity(), isCustomArgs)
        else
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
    }

    /** スライドインアニメーションを適用する。 */
    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer
            .apply(AnimPlayer.Entry(binding!!.launcherMenu, Animations.BounceInDown))
            .apply(AnimPlayer.Entry(binding!!.playLayout, Animations.BounceInLeft))
            .apply(AnimPlayer.Entry(binding!!.playButtonsLayout, Animations.BounceEnlarge))
    }

    /** スライドアウトアニメーションを適用する。 */
    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer
            .apply(AnimPlayer.Entry(binding!!.launcherMenu, Animations.FadeOutUp))
            .apply(AnimPlayer.Entry(binding!!.playLayout, Animations.FadeOutRight))
            .apply(AnimPlayer.Entry(binding!!.playButtonsLayout, Animations.BounceShrink))
    }
}
