package com.arata.yukarilauncher.ui.subassembly.customprofilepath

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.ItemProfilePathBinding
import com.arata.yukarilauncher.databinding.ViewPathManagerBinding
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathManager
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathManager.setCurrentPathId
import com.arata.yukarilauncher.feature.version.VersionsManager
import com.arata.yukarilauncher.setting.AllSettings.Companion.launcherProfile
import com.arata.yukarilauncher.ui.dialog.EditTextDialog
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.ui.fragment.FilesFragment
import com.arata.yukarilauncher.ui.fragment.FragmentWithAnim
import com.arata.yukarilauncher.utils.StoragePermissionsUtils
import com.arata.yukarilauncher.utils.YLTools

/**
 * プロファイルパス一覧のRecyclerViewアダプター
 */
class ProfilePathAdapter(
    private val fragment: FragmentWithAnim,
    private val view: RecyclerView
) :
    RecyclerView.Adapter<ProfilePathAdapter.ViewHolder>() {
    private val mData: MutableList<ProfileItem> = ArrayList()
    private val radioButtonList: MutableList<RadioButton> = mutableListOf()
    private var currentId: String? = if (StoragePermissionsUtils.checkPermissions()) launcherProfile.getValue() else "default"
    private val managerPopupWindow: PopupWindow = PopupWindow().apply {
        isFocusable = true
        isOutsideTouchable = true
    }

    /**
     * ビューホルダーを生成します。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemProfilePathBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    /**
     * ビューホルダーにプロファイルデータをバインドします。
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setView(mData[position], position)
    }

    /**
     * ビューホルダーがリサイクルされるときにラジオボタンをリストから削除します。
     */
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        radioButtonList.remove(holder.binding.radioButton)
    }

    /**
     * アイテム総数を返します。
     */
    override fun getItemCount(): Int = mData.size

    /**
     * リストデータを更新する
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(data: MutableList<ProfileItem>) {
        this.mData.clear()
        this.mData.addAll(data)
        radioButtonList.apply {
            forEach { radioButton -> radioButton.isChecked = false }
            clear()
        }
        notifyDataSetChanged()
        view.scheduleLayoutAnimation()
    }

    /**
     * データを保存して再描画する
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun refresh() {
        ProfilePathManager.save(mData)
        ProfilePathManager.refreshPath()
        notifyDataSetChanged()
        view.scheduleLayoutAnimation()
    }

    /**
     * ポップアップウィンドウを閉じる
     */
    fun closePopupWindow() {
        managerPopupWindow.dismiss()
    }

    /**
     * アクティブなパスIDを設定する
     */
    private fun setPathId(id: String) {
        currentId = id
        setCurrentPathId(id)
        radioButtonList.forEach { radioButton -> radioButton.isChecked = radioButton.tag.toString() == id }
    }

    /**
     * プロファイルパスアイテムのビューホルダー
     */
    inner class ViewHolder(val binding: ItemProfilePathBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * プロファイルデータをビューに設定する
         */
        fun setView(profileItem: ProfileItem, position: Int) {
            binding.apply {
                radioButtonList.add(
                    radioButton.apply {
                        tag = profileItem.id
                        isChecked = currentId == profileItem.id
                    }
                )
                title.text = profileItem.title
                path.text = profileItem.path
                path.isSelected = true

                val onClickListener = View.OnClickListener {
                    if (VersionsManager.canRefresh() && currentId != profileItem.id) {
                        StoragePermissionsUtils.checkPermissions(
                            activity = fragment.requireActivity(),
                            title = R.string.profiles_path_title,
                            permissionGranted = object : StoragePermissionsUtils.PermissionGranted {
                                override fun granted() {
                                    setPathId(profileItem.id)
                                }

                                override fun cancelled() {}
                            }
                        )
                    }
                }
                root.setOnClickListener(onClickListener)
                radioButton.setOnClickListener(onClickListener)

                operate.setOnClickListener {
                    showPopupWindow(root, profileItem.id == "default", profileItem, position)
                }
            }
        }

        /**
         * プロファイル操作用のポップアップウィンドウを表示する
         */
        private fun showPopupWindow(
            anchorView: View,
            isDefault: Boolean,
            profileItem: ProfileItem,
            itemIndex: Int
        ) {
            val context = anchorView.context

            val viewBinding = ViewPathManagerBinding.inflate(LayoutInflater.from(context)).apply {
                val onClickListener = View.OnClickListener { v ->
                    when (v) {
                        gotoView -> {
                            val bundle = Bundle()
                            bundle.putString(FilesFragment.BUNDLE_LOCK_PATH, Environment.getExternalStorageDirectory().absolutePath)
                            bundle.putString(FilesFragment.BUNDLE_LIST_PATH, profileItem.path)
                            YLTools.swapFragmentWithAnim(
                                fragment,
                                FilesFragment::class.java, FilesFragment.TAG, bundle
                            )
                        }
                        rename -> {
                            EditTextDialog.Builder(context)
                                .setTitle(R.string.generic_rename)
                                .setEditText(profileItem.title)
                                .setAsRequired()
                                .setConfirmListener { editBox, _ ->
                                    val string = editBox.text.toString()

                                    mData[itemIndex].title = string
                                    refresh()
                                    true
                                }.showDialog()
                        }
                        delete -> {
                            TipDialog.Builder(context)
                                .setTitle(context.getString(R.string.profiles_path_delete_title))
                                .setMessage(R.string.profiles_path_delete_message)
                                .setCancelable(false)
                                .setConfirmClickListener {
                                    if (currentId == profileItem.id) {
                                        setPathId("default")
                                    }
                                    mData.removeAt(itemIndex)
                                    refresh()
                                }.showDialog()
                        }
                        else -> {}
                    }
                    managerPopupWindow.dismiss()
                }
                gotoView.setOnClickListener(onClickListener)
                rename.setOnClickListener(onClickListener)
                delete.setOnClickListener(onClickListener)
                if (isDefault) {
                    rename.visibility = View.GONE
                    delete.visibility = View.GONE
                }
            }
            managerPopupWindow.apply {
                viewBinding.root.measure(0, 0)
                this.contentView = viewBinding.root
                this.width = viewBinding.root.measuredWidth
                this.height = viewBinding.root.measuredHeight
                showAsDropDown(anchorView, anchorView.measuredWidth, 0)
            }
        }
    }
}