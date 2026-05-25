package com.arata.yukarilauncher.ui.subassembly.version

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.ItemVersionBinding
import com.arata.yukarilauncher.databinding.ViewVersionManagerBinding
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathManager
import com.arata.yukarilauncher.feature.mod.modpack.export.ExportPathPickerDialog
import com.arata.yukarilauncher.feature.mod.modpack.export.ModPackExportHelper
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.feature.version.utils.VersionIconUtils
import com.arata.yukarilauncher.feature.version.VersionsManager
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.EditTextDialog
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.ui.fragment.FilesFragment
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.file.FileDeletionHandler
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.Tools

/**
 * バージョン一覧のRecyclerViewアダプター
 */
class VersionAdapter(
    private val parentFragment: Fragment,
    private val listener: OnVersionItemClickListener
) : RecyclerView.Adapter<VersionAdapter.ViewHolder>() {
    private val versions: MutableList<Version> = ArrayList()
    private val radioButtonList: MutableList<RadioButton> = mutableListOf()
    private var currentVersion: String? = null
    private var managerPopupWindow: PopupWindow = PopupWindow().apply {
        isFocusable = true
        isOutsideTouchable = true
    }

    /**
     * バージョンリストを更新する
     * @return 現在のバージョンのインデックス
     */
    @SuppressLint("NotifyDataSetChanged")
    fun refreshVersions(versions: List<Version>): Int {
        this.versions.clear()
        this.versions.addAll(versions)
        this.radioButtonList.apply {
            forEach { radioButton -> radioButton.isChecked = false }
            clear()
        }
        currentVersion = VersionsManager.getCurrentVersion()?.getVersionPath()?.absolutePath
        val currentIndex = versions.indexOfFirst { it.getVersionPath().absolutePath == currentVersion }
        notifyDataSetChanged()

        return currentIndex
    }

    /**
     * ポップアップウィンドウを閉じる
     */
    fun closePopupWindow() {
        managerPopupWindow.dismiss()
    }

    /**
     * アクティブなバージョンを設定する
     */
    private fun setCurrentVersion(context: Context, version: Version) {
        if (version.isValid()) {
            VersionsManager.saveCurrentVersion(version.getVersionName())
            currentVersion = version.getVersionPath().absolutePath
        } else {
            deleteVersion(version, context.getString(R.string.version_manager_delete_tip_invalid))
        }
        radioButtonList.forEach { radioButton -> radioButton.isChecked = radioButton.tag.toString() == currentVersion }
    }

    /**
     * バージョン削除の確認ダイアログを表示する
     */
    private fun deleteVersion(version: Version, deleteMessage: String) {
        val context = parentFragment.requireActivity()

        TipDialog.Builder(context)
            .setTitle(context.getString(R.string.version_manager_delete))
            .setMessage(deleteMessage)
            .setWarning()
            .setCancelable(false)
            .setConfirmClickListener {
                FileDeletionHandler(
                    context,
                    listOf(version.getVersionPath()),
                    Task.runTask {
                        VersionsManager.refresh("VersionAdapter:deleteVersion")
                    }
                ).start()
            }.showDialog()
    }

    /**
     * ビューホルダーを生成します。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    /**
     * ビューホルダーにバージョンデータをバインドします。
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(versions[position])
    }

    /**
     * ビューホルダーがリサイクルされるときにラジオボタンをリストから削除します。
     */
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        radioButtonList.remove(holder.binding.radioButton)
    }

    /**
     * バージョン総数を返します。
     */
    override fun getItemCount(): Int = versions.size

    /**
     * バージョンアイテムのビューホルダー
     */
    inner class ViewHolder(val binding: ItemVersionBinding) : RecyclerView.ViewHolder(binding.root) {
        private val mContext = binding.root.context

        /**
         * 空でない文字列を情報テキストとして追加する
         */
        private fun String.addInfoIfNotBlank(setRed: Boolean = false) {
            takeIf { it.isNotBlank() }?.let { string ->
                binding.versionInfoLayout.addView(getInfoTextView(string, setRed))
            }
        }

        /**
         * バージョンデータをビューにバインドする
         */
        fun bind(version: Version) {
            binding.apply {
                versionInfoLayout.removeAllViews()
                versionName.isSelected = true

                val versionPath = version.getVersionPath().absolutePath
                radioButtonList.add(
                    radioButton.apply {
                        tag = versionPath
                        isChecked = currentVersion == versionPath
                    }
                )

                versionName.text = version.getVersionName()

                if (!version.isValid()) {
                    mContext.getString(R.string.version_manager_invalid).addInfoIfNotBlank(true)
                }

                if (version.getVersionConfig().isIsolation()) {
                    mContext.getString(R.string.pedit_isolation_enabled).addInfoIfNotBlank()
                }

                version.getVersionInfo()?.let { versionInfo ->
                    versionInfoLayout.addView(getInfoTextView(versionInfo.minecraftVersion))
                    versionInfo.loaderInfo?.forEach { loaderInfo ->
                        loaderInfo.name.addInfoIfNotBlank()
                        loaderInfo.version.addInfoIfNotBlank()
                    }
                }

                favorite.setOnClickListener { _ ->
                    listener.showFavoritesDialog(version.getVersionName())
                }
                favorite.setImageDrawable(
                    ContextCompat.getDrawable(mContext,
                        if (listener.isVersionFavorited(version.getVersionName())) R.drawable.ic_favorite
                        else R.drawable.ic_favorite_border
                    )
                )

                operate.setOnClickListener { _ ->
                    showPopupWindow(operate, version)
                }

                VersionIconUtils(version).start(versionIcon)

                val onClickListener = View.OnClickListener { _ ->
                    setCurrentVersion(mContext, version)
                }
                radioButton.setOnClickListener(onClickListener)
                root.setOnClickListener(onClickListener)
            }
        }

        /**
         * 情報表示用のTextViewを生成する
         */
        private fun getInfoTextView(string: String, setRed: Boolean = false): TextView {
            val textView = TextView(mContext)
            textView.text = string
            val layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, Tools.dpToPx(8f).toInt(), 0)
            textView.layoutParams = layoutParams
            if (setRed) textView.setTextColor(Color.RED)
            return textView
        }

        /**
         * バージョン操作用のポップアップウィンドウを表示する
         */
        private fun showPopupWindow(
            anchorView: View,
            version: Version
        ) {
            val context = parentFragment.requireActivity()

            val viewBinding = ViewVersionManagerBinding.inflate(LayoutInflater.from(context)).apply {
                val onClickListener = View.OnClickListener { v ->
                    when (v) {
                        gotoView -> swapPath(version.getVersionPath().absolutePath)
                        gamePath -> swapPath(version.getGameDir().absolutePath)
                        rename -> VersionsManager.openRenameDialog(context, version)
                        copy -> VersionsManager.openCopyDialog(context, version)
                        exportModpack -> showExportDialog(version)
                        delete -> deleteVersion(version, context.getString(R.string.version_manager_delete_tip, version.getVersionName()))
                        else -> {}
                    }
                    managerPopupWindow.dismiss()
                }
                gotoView.setOnClickListener(onClickListener)
                gamePath.setOnClickListener(onClickListener)
                rename.setOnClickListener(onClickListener)
                copy.setOnClickListener(onClickListener)
                exportModpack.setOnClickListener(onClickListener)
                delete.setOnClickListener(onClickListener)
            }
            managerPopupWindow.apply {
                viewBinding.root.measure(0, 0)
                this.contentView = viewBinding.root
                this.width = viewBinding.root.measuredWidth
                this.height = viewBinding.root.measuredHeight
                showAsDropDown(anchorView, anchorView.measuredWidth, 0)
            }
        }

        /**
         * Modpack書き出しダイアログを表示する
         */
        private fun showExportDialog(version: Version) {
            val context = parentFragment.requireActivity()
            val labels = arrayOf(
                context.getString(R.string.version_manager_export_modpack_modrinth),
                context.getString(R.string.version_manager_export_modpack_curseforge)
            )
            val types = arrayOf(
                ModPackExportHelper.ExportType.MODRINTH,
                ModPackExportHelper.ExportType.CURSEFORGE
            )

            AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .setTitle(R.string.version_manager_export_modpack)
                .setItems(labels) { _, which ->
                    showExportFilterDialog(version, types[which])
                }.show()
        }

        /**
         * 書き出しフィルターダイアログを表示する
         */
        private fun showExportFilterDialog(version: Version, exportType: ModPackExportHelper.ExportType) {
            val context = parentFragment.requireActivity()
            ExportPathPickerDialog(
                context = context,
                rootDir = version.getGameDir(),
                title = context.getString(R.string.version_manager_export_modpack_include_title),
                defaultChecked = emptySet()
            ) { includePaths ->
                if (includePaths.isEmpty()) {
                    Toast.makeText(context, R.string.version_manager_export_modpack_select_required, Toast.LENGTH_SHORT).show()
                    return@ExportPathPickerDialog
                }
                showMetadataDialog(version, exportType, includePaths)
            }.show()
        }

        /**
         * メタデータ入力ダイアログを表示する
         */
        private fun showMetadataDialog(
            version: Version,
            exportType: ModPackExportHelper.ExportType,
            includePaths: Set<String>
        ) {
            val context = parentFragment.requireActivity()
            EditTextDialog.Builder(context)
                .setTitle(R.string.version_manager_export_modpack_name_title)
                .setHintText(version.getVersionName())
                .setEditText(version.getVersionName())
                .setAsRequired()
                .setConfirmListener { nameEditText, _ ->
                    val packName = nameEditText.text.toString()
                    EditTextDialog.Builder(context)
                        .setTitle(R.string.version_manager_export_modpack_version_title)
                        .setHintText(version.getVersionInfo()?.minecraftVersion ?: "1.0.0")
                        .setEditText(version.getVersionInfo()?.minecraftVersion ?: "1.0.0")
                        .setAsRequired()
                        .setConfirmListener { versionEditText, _ ->
                            val packVersion = versionEditText.text.toString()
                            EditTextDialog.Builder(context)
                                .setTitle(R.string.version_manager_export_modpack_author_title)
                                .setHintText("YukariLauncher")
                                .setEditText("YukariLauncher")
                                .setAsRequired()
                                .setConfirmListener { authorEditText, _ ->
                                    executeExport(
                                        version,
                                        exportType,
                                        ModPackExportHelper.ExportOptions(
                                            includePaths = includePaths,
                                            packName = packName,
                                            packVersion = packVersion,
                                            author = authorEditText.text.toString()
                                        )
                                    )
                                    true
                                }.showDialog()
                            true
                        }.showDialog()
                    true
                }.showDialog()
        }

        /**
         * Modpackの書き出しを実行する
         */
        private fun executeExport(
            version: Version,
            exportType: ModPackExportHelper.ExportType,
            options: ModPackExportHelper.ExportOptions
        ) {
            val context = parentFragment.requireActivity()
            Task.runTask {
                ModPackExportHelper.export(version, exportType, options)
            }.setExecutor(TaskExecutors.getDefault())
                .ended(TaskExecutors.getAndroidUI()) { file ->
                    if (file == null || !file.exists()) return@ended
                    Toast.makeText(
                        context,
                        context.getString(R.string.version_manager_export_modpack_success, file.absolutePath),
                        Toast.LENGTH_LONG
                    ).show()
                    FileTools.shareFile(context, file)
                }.onThrowable(TaskExecutors.getAndroidUI()) {
                    Tools.showError(context, it)
                }.execute()
        }

        /**
         * ファイルブラウザで指定パスを開く
         */
        private fun swapPath(path: String) {
            val bundle = Bundle()
            bundle.putString(FilesFragment.BUNDLE_LOCK_PATH, ProfilePathManager.getCurrentPath())
            bundle.putString(FilesFragment.BUNDLE_LIST_PATH, path)
            bundle.putBoolean(FilesFragment.BUNDLE_QUICK_ACCESS_PATHS, false)
            YLTools.swapFragmentWithAnim(
                parentFragment,
                FilesFragment::class.java, FilesFragment.TAG, bundle
            )
        }
    }

    /**
     * バージョンアイテムクリックのコールバックインターフェース
     */
    interface OnVersionItemClickListener {
        /** お気に入りダイアログを表示する */
        fun showFavoritesDialog(versionName: String)
        /** バージョンがお気に入りかどうかを確認する */
        fun isVersionFavorited(versionName: String): Boolean
    }
}
