package com.arata.yukarilauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.angcyo.tablayout.DslTabLayout
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentVersionsListBinding
import com.arata.yukarilauncher.databinding.ItemFavoriteFolderBinding
import com.arata.yukarilauncher.databinding.ViewSingleActionPopupBinding
import com.arata.yukarilauncher.event.single.RefreshVersionsEvent
import com.arata.yukarilauncher.event.single.RefreshVersionsEvent.MODE.END
import com.arata.yukarilauncher.event.single.RefreshVersionsEvent.MODE.START
import com.arata.yukarilauncher.event.sticky.FileSelectorEvent
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathManager
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.feature.version.VersionsManager
import com.arata.yukarilauncher.feature.version.favorites.FavoritesVersionUtils
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.EditTextDialog
import com.arata.yukarilauncher.ui.dialog.FavoritesVersionDialog
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.ui.layout.AnimRelativeLayout
import com.arata.yukarilauncher.ui.subassembly.customprofilepath.ProfileItem
import com.arata.yukarilauncher.ui.subassembly.customprofilepath.ProfilePathAdapter
import com.arata.yukarilauncher.ui.subassembly.version.VersionAdapter
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.StoragePermissionsUtils
import com.arata.yukarilauncher.utils.YLTools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.UUID

/**
 * バージョン一覧フラグメント
 */
class VersionsListFragment : FragmentWithAnim(R.layout.fragment_versions_list) {
    companion object {
        const val TAG: String = "VersionsListFragment"
    }

    private lateinit var binding: FragmentVersionsListBinding
    private var versionsAdapter: VersionAdapter? = null
    private var profilePathAdapter: ProfilePathAdapter? = null
    private val mFavoritesFolderViewList: MutableList<View> = ArrayList()
    private var versionSearchKeyword: String = ""

    private val mFavoritesActionPopupWindow: PopupWindow = PopupWindow().apply {
        isFocusable = true
        isOutsideTouchable = true
    }

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val selectorEvent = EventBus.getDefault().getStickyEvent(FileSelectorEvent::class.java)

        selectorEvent?.let { event ->
            event.path?.let { path ->
                if (path.isNotEmpty() && !ProfilePathManager.containsPath(path)) {
                    EditTextDialog.Builder(requireContext())
                        .setTitle(R.string.profiles_path_create_new_title)
                        .setAsRequired()
                        .setConfirmListener { editBox, _ ->
                            ProfilePathManager.addPath(ProfileItem(UUID.randomUUID().toString(), editBox.text.toString(), path))
                            refresh()
                            true
                        }.showDialog()
                }
            }
            EventBus.getDefault().removeStickyEvent(event)
        }
        binding = FragmentVersionsListBinding.inflate(layoutInflater)
        return binding.root
    }

    /**
     * ビュー作成後の初期化処理を行います。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            installNew.setOnClickListener {
                YLTools.swapFragmentWithAnim(this@VersionsListFragment, VersionSelectorFragment::class.java, VersionSelectorFragment.TAG, null)
            }

            fun refreshFavoritesFolderTab(index: Int) {
                if (index < 0) return

                val keys = FavoritesVersionUtils.getFavoritesStructure().keys.toList()
                when (index) {
                    0 -> refreshVersions(true)
                    else -> {
                        val folderIndex = index - 1
                        if (folderIndex in keys.indices) {
                            refreshVersions(false, keys[folderIndex])
                        } else {
                            refreshVersions(true)
                        }
                    }
                }
            }

            favoritesFolderTab.observeIndexChange { _, toIndex, reselect, fromUser ->
                if (fromUser && !reselect) {
                    refreshFavoritesFolderTab(toIndex)
                }
            }

            favoritesActions.setOnClickListener { showFavoritesActionPopupWindow(it) }
            versionSearchInput.doAfterTextChanged {
                versionSearchKeyword = it?.toString()?.trim().orEmpty()
                refreshFavoritesFolderTab(favoritesFolderTab.currentItemIndex)
            }

            versionsAdapter = VersionAdapter(this@VersionsListFragment, object : VersionAdapter.OnVersionItemClickListener {
                override fun showFavoritesDialog(versionName: String) {
                    if (FavoritesVersionUtils.getFavoritesStructure().isNotEmpty()) {
                        FavoritesVersionDialog(requireActivity(), versionName) {
                            refreshFavoritesFolderTab(favoritesFolderTab.currentItemIndex)
                        }.show()
                    } else Toast.makeText(requireActivity(), getString(R.string.version_manager_favorites_dialog_no_folders), Toast.LENGTH_SHORT).show()
                }

                override fun isVersionFavorited(versionName: String): Boolean {
                    if (favoritesFolderTab.currentItemIndex != 0) {
                        return true
                    }
                    return FavoritesVersionUtils.getFavoritesStructure().values.any { it.contains(versionName) }
                }
            })

            versions.apply {
                layoutAnimation = LayoutAnimationController(
                    AnimationUtils.loadAnimation(view.context, R.anim.fade_downwards)
                )
                layoutManager = LinearLayoutManager(requireContext())
                this.adapter = versionsAdapter
            }

            profilePathAdapter = ProfilePathAdapter(this@VersionsListFragment, profilesPath)
            profilesPath.apply {
                layoutAnimation = LayoutAnimationController(
                    AnimationUtils.loadAnimation(view.context, R.anim.fade_downwards)
                )
                layoutManager = LinearLayoutManager(requireContext())
                this.adapter = profilePathAdapter
            }

            refreshButton.setOnClickListener {
                refreshButton.isEnabled = false
                refresh(refreshVersions = true, refreshVersionInfo = true)
            }
            createPathButton.setOnClickListener {
                StoragePermissionsUtils.checkPermissions(
                    activity = requireActivity(),
                    title = R.string.profiles_path_create_new,
                    permissionGranted = object : StoragePermissionsUtils.PermissionGranted {
                        override fun granted() {
                            val bundle = Bundle()
                            bundle.putBoolean(FilesFragment.BUNDLE_SELECT_FOLDER_MODE, true)
                            bundle.putBoolean(FilesFragment.BUNDLE_SHOW_FILE, false)
                            bundle.putBoolean(FilesFragment.BUNDLE_REMOVE_LOCK_PATH, false)
                            YLTools.swapFragmentWithAnim(this@VersionsListFragment, FilesFragment::class.java, FilesFragment.TAG, bundle)
                        }

                        override fun cancelled() {}
                    }
                )
            }
            returnButton.setOnClickListener { YLTools.onBackPressed(requireActivity()) }
        }

        refresh()
    }

    /**
     * ビューを更新する
     */
    private fun refresh(refreshVersions: Boolean = false, refreshVersionInfo: Boolean = false) {
        ProfilePathManager.refreshPath()

        if (refreshVersions) VersionsManager.refresh("VersionListFragment:refresh", refreshVersionInfo)
        else refreshFavoritesFolderAndVersions()

        val path: MutableList<ProfileItem> = ArrayList<ProfileItem>().apply {
            add(ProfileItem("default", getString(R.string.profiles_path_default), PathManager.DIR_GAME_HOME))
            addAll(ProfilePathManager.getAllPath())
        }

        profilePathAdapter?.updateData(path)
    }

    /**
     * バージョン一覧を更新する
     */
    private fun refreshVersions(all: Boolean = true, favoritesFolder: String? = null) {
        versionsAdapter?.let {
            val versions = VersionsManager.getVersions()

            fun getVersions(): List<Version> {
                val sourceVersions = if (all) versions else {
                    val folderName = favoritesFolder ?: ""
                    val folderVersions = FavoritesVersionUtils.getValidVersions(folderName)
                    ArrayList<Version>().apply {
                        versions.forEach { version ->
                            if (folderVersions.contains(version.getVersionName())) {
                                add(version)
                            }
                        }
                    }
                }

                if (versionSearchKeyword.isEmpty()) return sourceVersions
                val keyword = versionSearchKeyword.lowercase()
                return sourceVersions.filter { version ->
                    version.getVersionName().lowercase().contains(keyword)
                }
            }

            val currentIndex = it.refreshVersions(getVersions())
            binding.versions.scrollToPosition(currentIndex)
            binding.versions.scheduleLayoutAnimation()
        }
    }

    /**
     * お気に入りフォルダとバージョン一覧を更新する
     */
    private fun refreshFavoritesFolderAndVersions() {
        binding.favoritesFolderTab.setCurrentItem(0)
        refreshVersions()

        mFavoritesFolderViewList.forEach { view ->
            binding.favoritesFolderTab.removeView(view)
        }
        mFavoritesFolderViewList.clear()

        fun createView(folderName: String): AnimRelativeLayout {
            return ItemFavoriteFolderBinding.inflate(layoutInflater).apply {
                name.text = folderName
                root.layoutParams = DslTabLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                root.setOnLongClickListener {
                    showFavoritesDeletePopupWindow(root, folderName)
                    true
                }
            }.root
        }

        FavoritesVersionUtils.getFavoritesStructure().forEach { (folderName, _) ->
            val view = createView(folderName)
            mFavoritesFolderViewList.add(view)
            binding.favoritesFolderTab.addView(view)
        }
    }

    /**
     * アクションポップアップを表示する
     */
    private fun refreshActionPopupWindow(anchorView: View, binding: ViewBinding) {
        mFavoritesActionPopupWindow.apply {
            binding.root.measure(0, 0)
            this.contentView = binding.root
            this.width = binding.root.measuredWidth
            this.height = binding.root.measuredHeight
            showAsDropDown(anchorView)
        }
    }

    /**
     * お気に入り追加ポップアップを表示する
     */
    private fun showFavoritesActionPopupWindow(anchorView: View) {
        refreshActionPopupWindow(anchorView, ViewSingleActionPopupBinding.inflate(LayoutInflater.from(requireActivity())).apply {
            icon.setImageDrawable(
                ContextCompat.getDrawable(requireActivity(), R.drawable.ic_add)
            )
            text.setText(R.string.version_manager_favorites_add_category)
            text.setOnClickListener {
                EditTextDialog.Builder(requireActivity())
                    .setTitle(R.string.version_manager_favorites_write_folder_name)
                    .setAsRequired()
                    .setConfirmListener { editText, _ ->
                        FavoritesVersionUtils.addFolder(editText.text.toString())
                        refreshFavoritesFolderAndVersions()
                        true
                    }.showDialog()
                mFavoritesActionPopupWindow.dismiss()
            }
        })
    }

    /**
     * お気に入り削除ポップアップを表示する
     */
    private fun showFavoritesDeletePopupWindow(anchorView: View, folderName: String) {
        refreshActionPopupWindow(anchorView, ViewSingleActionPopupBinding.inflate(LayoutInflater.from(requireActivity())).apply {
            icon.setImageDrawable(
                ContextCompat.getDrawable(requireActivity(), R.drawable.ic_menu_delete_forever)
            )
            text.setText(R.string.version_manager_favorites_remove_folder_title)
            text.setOnClickListener {
                TipDialog.Builder(requireActivity())
                    .setTitle(R.string.version_manager_favorites_remove_folder_title)
                    .setMessage(R.string.version_manager_favorites_remove_folder_message)
                    .setWarning()
                    .setConfirmClickListener {
                        FavoritesVersionUtils.removeFolder(folderName)
                        refreshFavoritesFolderAndVersions()
                    }.showDialog()
                mFavoritesActionPopupWindow.dismiss()
            }
        })
    }

    /**
     * バージョン更新イベントを処理します。
     */
    @Subscribe
    fun event(event: RefreshVersionsEvent) {
        binding.apply {
            TaskExecutors.runInUIThread {
                when (event.mode) {
                    START -> {
                        refreshButton.isEnabled = false
                        favoritesFolderTab.isEnabled = false
                        versions.visibility = View.GONE
                        refreshVersions.visibility = View.VISIBLE
                    }
                    END -> {
                        refreshFavoritesFolderAndVersions()
                        favoritesFolderTab.isEnabled = true
                        refreshButton.isEnabled = true
                        versions.visibility = View.VISIBLE
                        refreshVersions.visibility = View.GONE
                    }
                }
                closeAllPopupWindow()
            }
        }
    }

    /**
     * すべてのポップアップを閉じる
     */
    private fun closeAllPopupWindow() {
        versionsAdapter?.closePopupWindow()
        profilePathAdapter?.closePopupWindow()
        mFavoritesActionPopupWindow.dismiss()
    }

    /**
     * フラグメント開始時にEventBusを登録します。
     */
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    /**
     * フラグメント停止時にEventBusの登録を解除します。
     */
    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    /**
     * フラグメント一時停止時にすべてのポップアップを閉じます。
     */
    override fun onPause() {
        super.onPause()
        closeAllPopupWindow()
    }

    /**
     * スライドインアニメーションを実行します。
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(versionsListLayout, Animations.BounceInUp))
                .apply(AnimPlayer.Entry(versionTopBar, Animations.BounceInDown))
                .apply(AnimPlayer.Entry(operateLayout, Animations.BounceInLeft))
        }
    }

    /**
     * スライドアウトアニメーションを実行します。
     */
    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(versionsListLayout, Animations.FadeOutDown))
                .apply(AnimPlayer.Entry(versionTopBar, Animations.FadeOutUp))
                .apply(AnimPlayer.Entry(operateLayout, Animations.FadeOutRight))
        }
    }
}
