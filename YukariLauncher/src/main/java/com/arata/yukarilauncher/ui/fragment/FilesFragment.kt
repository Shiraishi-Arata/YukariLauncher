package com.arata.yukarilauncher.ui.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentFilesBinding
import com.arata.yukarilauncher.event.sticky.FileSelectorEvent
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.EditTextDialog
import com.arata.yukarilauncher.ui.dialog.FilesDialog
import com.arata.yukarilauncher.ui.dialog.FilesDialog.FilesButton
import com.arata.yukarilauncher.ui.subassembly.filelist.FileItemBean
import com.arata.yukarilauncher.ui.subassembly.filelist.FileSelectedListener
import com.arata.yukarilauncher.ui.subassembly.view.SearchViewWrapper
import com.arata.yukarilauncher.utils.NewbieGuideUtils
import com.arata.yukarilauncher.utils.StoragePermissionsUtils
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.anim.AnimUtils.Companion.setVisibilityAnim
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.utils.file.PasteFile
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.ui.activity.OpenDocumentWithExtension
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.Objects
import java.util.function.Consumer

/**
 * ファイル管理フラグメント
 */
class FilesFragment : FragmentWithAnim(R.layout.fragment_files) {
    companion object {
        const val TAG: String = "FilesFragment"
        const val BUNDLE_LOCK_PATH: String = "bundle_lock_path"
        const val BUNDLE_LIST_PATH: String = "bundle_list_path"
        const val BUNDLE_SHOW_FILE: String = "show_file"
        const val BUNDLE_SHOW_FOLDER: String = "show_folder"
        const val BUNDLE_QUICK_ACCESS_PATHS: String = "quick_access_paths"
        const val BUNDLE_MULTI_SELECT_MODE: String = "multi_select_mode"
        const val BUNDLE_SELECT_FOLDER_MODE: String = "select_folder_mode"
        const val BUNDLE_REMOVE_LOCK_PATH: String = "remove_lock_path"
        const val BUNDLE_TITLE_REMOVE_LOCK_PATH: String = "title_remove_lock_path"
    }

    private lateinit var binding: FragmentFilesBinding
    private lateinit var mSearchViewWrapper: SearchViewWrapper
    private var openDocumentLauncher: ActivityResultLauncher<Any?>? = null
    private var mShowFiles = false
    private var mShowFolders = false
    private var mQuickAccessPaths = false
    private var mMultiSelectMode = false
    private var mSelectFolderMode = false
    private var mRemoveLockPath = false
    private var mTitleRemoveLockPath = false
    private var mLockPath: String? = null
    private var mListPath: String? = null

    /**
     * フラグメント作成時にファイル選択ランチャーを初期化します。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension(null, true)) { uris: List<Uri>? ->
            uris?.let { uriList ->
                val dialog = YLTools.showTaskRunningDialog((requireContext()))
                Task.runTask {
                    uriList.forEach { uri ->
                        FileTools.copyFileInBackground(requireContext(), uri, binding.fileRecyclerView.fullPath.absolutePath)
                    }
                }.ended(TaskExecutors.getAndroidUI()) {
                    Toast.makeText(requireContext(), getString(R.string.file_added), Toast.LENGTH_SHORT).show()
                    binding.fileRecyclerView.refreshPath()
                }.onThrowable { e ->
                    Tools.showErrorRemote(e)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    dialog.dismiss()
                }.execute()
            }
        }
    }

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilesBinding.inflate(layoutInflater)
        mSearchViewWrapper = SearchViewWrapper(this)
        return binding.root
    }

    /**
     * ビュー作成後の初期化処理を行います。
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        parseBundle()
        initViews()

        val storageDirectory = Environment.getExternalStorageDirectory()

        binding.apply {
            fileRecyclerView.apply {
                setShowFiles(mShowFiles)
                setShowFolders(mShowFolders)
                setTitleListener { title: String? -> currentPath.text = removeLockPath(title!!, mTitleRemoveLockPath) }

                setFileSelectedListener(object : FileSelectedListener() {
                    override fun onFileSelected(file: File?, path: String?) {
                        file?.let { showDialog(it) }
                    }

                    override fun onItemLongClick(file: File?, path: String?) {
                        file?.let { if (it.isDirectory) showDialog(it) }
                    }
                })

                setOnMultiSelectListener { itemBeans: List<FileItemBean> ->
                    if (itemBeans.isNotEmpty()) {
                        Task.runTask {
                            val selectedFiles: MutableList<File> = ArrayList()
                            itemBeans.forEach(Consumer { value: FileItemBean ->
                                val file = value.file
                                file?.apply { selectedFiles.add(this) }
                            })
                            selectedFiles
                        }.ended(TaskExecutors.getAndroidUI()) { selectedFiles ->
                            val filesButton = FilesButton()
                            filesButton.setButtonVisibility(true, true, false, false, true, false)
                            filesButton.setDialogText(
                                getString(R.string.file_multi_select_mode_title),
                                getString(R.string.file_multi_select_mode_message, itemBeans.size), null
                            )

                            val filesDialog = FilesDialog(
                                requireContext(),
                                filesButton,
                                Task.runTask(TaskExecutors.getAndroidUI()) {
                                    closeMultiSelect()
                                    refreshPath()
                                },
                                fullPath,
                                selectedFiles!!
                            )
                            filesDialog.setCopyButtonClick { operateView.pasteButton.visibility = View.VISIBLE }
                            filesDialog.show()
                        }.execute()
                    }
                }

                setRefreshListener {
                    setVisibilityAnim(nothingText, isNoFile)
                    if (Objects.equals(fullPath.absolutePath, storageDirectory.absolutePath)) {
                        StoragePermissionsUtils.checkPermissions(
                            activity = requireActivity(),
                            title = R.string.file_external_storage,
                            permissionGranted = null
                        )
                    }
                }
            }

            currentPath.setOnClickListener {
                EditTextDialog.Builder(requireContext())
                    .setTitle(R.string.file_jump_to_path)
                    .setEditText(fileRecyclerView.fullPath.absolutePath)
                    .setAsRequired()
                    .setConfirmListener { editBox, _ ->
                        val path = editBox.text.toString()

                        val file = File(path)
                        if (!path.contains(mLockPath!!) || !file.isDirectory || !file.exists()) {
                            editBox.error = getString(R.string.file_does_not_exist)
                            return@setConfirmListener false
                        }

                        fileRecyclerView.listFileAt(file)
                        true
                    }.showDialog()
            }

            externalStorage.setOnClickListener {
                closeMultiSelect()
                fileRecyclerView.listFileAt(storageDirectory)
            }

            softwarePrivate.setOnClickListener {
                closeMultiSelect()
                fileRecyclerView.listFileAt(requireContext().getExternalFilesDir(null))
            }
            val adapter = fileRecyclerView.adapter
            multiSelectFiles.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                selectAll.apply {
                    this.isChecked = false
                    visibility = if (isChecked) View.VISIBLE else View.GONE
                }
                adapter.setMultiSelectMode(isChecked)
                if (mSearchViewWrapper.isVisible()) mSearchViewWrapper.setVisibility(!isChecked)
            }
            selectAll.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                adapter.selectAllFiles(
                    isChecked
                )
            }

            operateView.returnButton.setOnClickListener {
                if (!mSelectFolderMode) {
                    closeMultiSelect()
                    Tools.removeCurrentFragment(requireActivity())
                } else {
                    EventBus.getDefault().postSticky(FileSelectorEvent(
                        removeLockPath(fileRecyclerView.fullPath.absolutePath, mRemoveLockPath)
                    ))
                    Tools.removeCurrentFragment(requireActivity())
                }
            }
            operateView.addFileButton.setOnClickListener {
                closeMultiSelect()
                openDocumentLauncher?.launch(null)
            }
            operateView.createFolderButton.setOnClickListener {
                closeMultiSelect()
                EditTextDialog.Builder(requireContext())
                    .setTitle(R.string.file_folder_dialog_insert_name)
                    .setAsRequired()
                    .setConfirmListener { editBox, _ ->
                        val name = editBox.text.toString()

                        if (name.contains("/")) {
                            val folderNames = name.split("/")
                            if (folderNames.any {
                                FileTools.isFilenameInvalid(
                                    it,
                                    { illegalCharacters ->
                                        editBox.error = getString(R.string.generic_input_invalid_character, illegalCharacters)
                                    },
                                    { invalidLength ->
                                        editBox.error = getString(R.string.file_invalid_length, invalidLength, 255)
                                    }
                                )
                            }) return@setConfirmListener false
                        } else if (FileTools.isFilenameInvalid(editBox)) {
                            return@setConfirmListener false
                        }

                        val folder = File(fileRecyclerView.fullPath, name)

                        if (folder.exists()) {
                            editBox.error = getString(R.string.file_rename_exitis)
                            return@setConfirmListener false
                        }

                        val success = folder.mkdirs()
                        if (success) {
                            fileRecyclerView.listFileAt(folder)
                        } else {
                            fileRecyclerView.refreshPath()
                        }
                        true
                    }.showDialog()
            }
            operateView.pasteButton.apply {
                setOnClickListener {
                    PasteFile.getInstance()
                        .pasteFiles(
                            requireActivity(),
                            fileRecyclerView.fullPath,
                            null,
                            Task.runTask(TaskExecutors.getAndroidUI()) {
                                closeMultiSelect()
                                visibility = View.GONE
                                fileRecyclerView.refreshPath()
                            }
                        )
                }
            }
            operateView.searchButton.setOnClickListener {
                closeMultiSelect()
                mSearchViewWrapper.setVisibility()
            }
            operateView.refreshButton.setOnClickListener {
                closeMultiSelect()
                fileRecyclerView.refreshPath()
            }

            fileRecyclerView.apply list@{
                mListPath?.let {
                    lockAndListAt(File(mLockPath!!), File(mListPath!!))
                    return@list
                }
                lockAndListAt(File(mLockPath!!), File(mLockPath!!))
            }
        }

        startNewbieGuide()
    }

    /**
     * 初心者ガイドを開始する
     */
    private fun startNewbieGuide() {
        if (NewbieGuideUtils.showOnlyOne("${TAG}${if (mSelectFolderMode) "_select" else ""}")) return
        binding.operateView.apply {
            val fragmentActivity = requireActivity()
            val refresh = NewbieGuideUtils.getSimpleTarget(fragmentActivity, refreshButton, getString(R.string.generic_refresh), getString(R.string.newbie_guide_general_refresh))
            val search = NewbieGuideUtils.getSimpleTarget(fragmentActivity, searchButton, getString(R.string.generic_search), getString(R.string.newbie_guide_file_search))
            val createFolder = NewbieGuideUtils.getSimpleTarget(fragmentActivity, createFolderButton, getString(R.string.file_create_folder), getString(R.string.newbie_guide_file_create_folder))
            if (mSelectFolderMode) {
                TapTargetSequence(fragmentActivity)
                    .targets(refresh, search, createFolder,
                        NewbieGuideUtils.getSimpleTarget(fragmentActivity, returnButton, getString(R.string.file_select_folder), getString(R.string.newbie_guide_file_select)))
                    .start()
            } else {
                TapTargetSequence(fragmentActivity)
                    .targets(refresh, search,
                        NewbieGuideUtils.getSimpleTarget(fragmentActivity, addFileButton, getString(R.string.file_add_file), getString(R.string.newbie_guide_file_import)),
                        createFolder,
                        NewbieGuideUtils.getSimpleTarget(fragmentActivity, returnButton, getString(R.string.generic_close), getString(R.string.newbie_guide_general_close)))
                    .start()
            }
        }
    }

    /**
     * マルチセレクトモードを解除する
     */
    private fun closeMultiSelect() {
        binding.multiSelectFiles.isChecked = false
        binding.selectAll.visibility = View.GONE
    }

    /**
     * ファイル操作ダイアログを表示する
     */
    private fun showDialog(file: File) {
        val filesButton = FilesButton()
        filesButton.setButtonVisibility(true, true, true, true, true, false)
        val message = if (file.isDirectory) {
            getString(R.string.file_folder_message)
        } else {
            getString(R.string.file_message)
        }
        filesButton.setMessageText(message)

        val filesDialog = FilesDialog(requireContext(), filesButton,
            Task.runTask(TaskExecutors.getAndroidUI()) {
                binding.fileRecyclerView.refreshPath()
            },
            binding.fileRecyclerView.fullPath, file)
        filesDialog.setCopyButtonClick { binding.operateView.pasteButton.visibility = View.VISIBLE }
        filesDialog.show()
    }

    /**
     * パスからロックパス部分を取り除く
     */
    private fun removeLockPath(path: String, remove: Boolean): String {
        var string = path
        if (remove) {
            string = path.replace(mLockPath!!, ".")
        }
        return string
    }

    /**
     * ビューを初期化します。
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initViews() {
        binding.apply {
            mSearchViewWrapper.setSearchListener(object : SearchViewWrapper.SearchListener {
                override fun onSearch(string: String?, caseSensitive: Boolean): Int {
                    return fileRecyclerView.searchFiles(string, caseSensitive)
                }
            })
            mSearchViewWrapper.setShowSearchResultsListener(object : SearchViewWrapper.ShowSearchResultsListener {
                override fun onSearch(show: Boolean) {
                    fileRecyclerView.setShowSearchResultsOnly(show)
                }
            })

            if (!mQuickAccessPaths) {
                externalStorage.visibility = View.GONE
                softwarePrivate.visibility = View.GONE
            }
            if (mSelectFolderMode || !mMultiSelectMode) {
                multiSelectFiles.visibility = View.GONE
                selectAll.visibility = View.GONE
            }

            if (mSelectFolderMode) {
                operateView.addFileButton.visibility = View.GONE
                operateView.returnButton.apply {
                    contentDescription = getString(R.string.file_select_folder)
                    setImageDrawable(resources.getDrawable(R.drawable.ic_check, requireActivity().theme))
                }
            }
        }

        binding.operateView.apply {
            pasteButton.setVisibility(if (PasteFile.getInstance().pasteType != null) View.VISIBLE else View.GONE)

            YLTools.setTooltipText(
                returnButton,
                addFileButton,
                createFolderButton,
                pasteButton,
                searchButton,
                refreshButton)
        }
    }

    /**
     * バンドル引数を解析する
     */
    private fun parseBundle() {
        val bundle = arguments ?: return
        mLockPath = bundle.getString(BUNDLE_LOCK_PATH, Environment.getExternalStorageDirectory().absolutePath)
        mListPath = bundle.getString(BUNDLE_LIST_PATH, null)
        mShowFiles = bundle.getBoolean(BUNDLE_SHOW_FILE, true)
        mShowFolders = bundle.getBoolean(BUNDLE_SHOW_FOLDER, true)
        mQuickAccessPaths = bundle.getBoolean(BUNDLE_QUICK_ACCESS_PATHS, true)
        mMultiSelectMode = bundle.getBoolean(BUNDLE_MULTI_SELECT_MODE, true)
        mSelectFolderMode = bundle.getBoolean(BUNDLE_SELECT_FOLDER_MODE, false)
        mRemoveLockPath = bundle.getBoolean(BUNDLE_REMOVE_LOCK_PATH, true)
        mTitleRemoveLockPath = bundle.getBoolean(BUNDLE_TITLE_REMOVE_LOCK_PATH, true)
    }

    /**
     * スライドインアニメーションを実行します。
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(filesLayout, Animations.BounceInDown))
                .apply(AnimPlayer.Entry(operateLayout, Animations.BounceInLeft))
        }
    }

    /**
     * スライドアウトアニメーションを実行します。
     */
    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(filesLayout, Animations.FadeOutUp))
                .apply(AnimPlayer.Entry(operateLayout, Animations.FadeOutRight))
        }
    }
}
