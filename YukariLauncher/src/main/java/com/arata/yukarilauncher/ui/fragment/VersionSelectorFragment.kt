package com.arata.yukarilauncher.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentVersionBinding
import com.arata.yukarilauncher.ui.subassembly.versionlist.VersionSelectedListener
import com.arata.yukarilauncher.ui.subassembly.versionlist.VersionType
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.value.JMinecraftVersionList
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.task.AsyncVersionList

/**
 * Minecraftバージョン選択フラグメント
 */
class VersionSelectorFragment : FragmentWithAnim(R.layout.fragment_version) {

    companion object {
        const val TAG: String = "FileSelectorFragment"
    }

    private lateinit var binding: FragmentVersionBinding

    private var release: TabLayout.Tab? = null
    private var snapshot: TabLayout.Tab? = null
    private var beta: TabLayout.Tab? = null
    private var alpha: TabLayout.Tab? = null
    private var aprilFools: TabLayout.Tab? = null

    private var versionType: VersionType? = null

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVersionBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * ビュー作成後の初期化処理を行います。
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindTab()

        binding.apply {

            AsyncVersionList().getVersionList(
                object : AsyncVersionList.VersionDoneListener {
                    override fun onVersionDone(versions: JMinecraftVersionList) {

                        requireActivity().runOnUiThread {
                            binding.version.setVersionType(versionType)
                            binding.version.setFilterString(
                                binding.searchVersion.text?.toString() ?: ""
                            )
                        }
                    }
                },
                false
            )

            refresh(versionTab.getTabAt(versionTab.selectedTabPosition))

            versionTab.addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    refresh(tab)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            searchVersion.doAfterTextChanged { text ->
                version.setFilterString(text?.toString() ?: "")
            }

            returnButton.setOnClickListener {
                YLTools.onBackPressed(requireActivity())
            }

            refreshButton.setOnClickListener {

                refreshButton.isEnabled = false

                version.setVersionType(versionType)

                version.setFilterString(searchVersion.text?.toString() ?: "")

                refreshButton.postDelayed({
                    refreshButton.isEnabled = true
                }, 500)
            }

            version.setVersionSelectedListener(object : VersionSelectedListener() {
                override fun onVersionSelected(versionName: String?) {
                    if (versionName == null) {
                        Tools.backToMainMenu(requireActivity())
                    } else {
                        val bundle = Bundle()
                        bundle.putString(
                            InstallGameFragment.BUNDLE_MC_VERSION,
                            versionName
                        )

                        YLTools.swapFragmentWithAnim(
                            this@VersionSelectorFragment,
                            InstallGameFragment::class.java,
                            InstallGameFragment.TAG,
                            bundle
                        )
                    }
                }
            })
        }
    }

    /**
     * タブ選択に応じて表示を更新する
     */
    private fun refresh(tab: TabLayout.Tab?) {
        setVersionType(tab)

        binding.version.setVersionType(versionType)
    }

    /**
     * タブに対応するバージョン種別を設定する
     */
    private fun setVersionType(tab: TabLayout.Tab?) {
        versionType = when (tab) {
            release -> VersionType.RELEASE
            snapshot -> VersionType.SNAPSHOT
            beta -> VersionType.BETA
            alpha -> VersionType.ALPHA
            aprilFools -> VersionType.APRIL_FOOLS
            else -> VersionType.RELEASE
        }
    }

    /**
     * タブをバインドする
     */
    private fun bindTab() {
        binding.apply {

            release = versionTab.newTab().setText(getString(R.string.generic_release))
            snapshot = versionTab.newTab().setText(getString(R.string.version_snapshot))
            beta = versionTab.newTab().setText(getString(R.string.version_beta))
            alpha = versionTab.newTab().setText(getString(R.string.version_alpha))
            aprilFools = versionTab.newTab().setText(getString(R.string.version_april_fools))

            versionTab.addTab(release!!)
            versionTab.addTab(snapshot!!)
            versionTab.addTab(beta!!)
            versionTab.addTab(alpha!!)
            versionTab.addTab(aprilFools!!)

            versionTab.selectTab(release)
        }
    }

    /**
     * スライドインアニメーションを実行します。
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer
            .apply(AnimPlayer.Entry(binding.versionLayout, Animations.BounceInDown))
            .apply(AnimPlayer.Entry(binding.operateLayout, Animations.BounceInLeft))
    }

    /**
     * スライドアウトアニメーションを実行します。
     */
    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer
            .apply(AnimPlayer.Entry(binding.versionLayout, Animations.FadeOutUp))
            .apply(AnimPlayer.Entry(binding.operateLayout, Animations.FadeOutRight))
    }
}