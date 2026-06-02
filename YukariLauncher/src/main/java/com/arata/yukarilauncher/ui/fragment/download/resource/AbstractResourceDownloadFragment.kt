package com.arata.yukarilauncher.ui.fragment.download.resource

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Button
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentDownloadResourceBinding
import com.arata.yukarilauncher.event.value.DownloadPageEvent
import com.arata.yukarilauncher.event.value.DownloadPageEvent.PageSwapEvent.Companion.IN
import com.arata.yukarilauncher.event.value.DownloadPageEvent.PageSwapEvent.Companion.OUT
import com.arata.yukarilauncher.feature.download.Filters
import com.arata.yukarilauncher.feature.download.InfoAdapter
import com.arata.yukarilauncher.feature.download.SelfReferencingFuture
import com.arata.yukarilauncher.feature.download.enums.Category
import com.arata.yukarilauncher.feature.download.enums.Classify
import com.arata.yukarilauncher.feature.download.enums.ModLoader
import com.arata.yukarilauncher.feature.download.enums.Platform
import com.arata.yukarilauncher.feature.download.enums.Sort
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.item.ModLikeVersionItem
import com.arata.yukarilauncher.feature.download.item.ModVersionItem
import com.arata.yukarilauncher.feature.download.item.SearchResult
import com.arata.yukarilauncher.feature.download.item.VersionItem
import com.arata.yukarilauncher.feature.download.platform.PlatformNotSupportedException
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.BatchModDownloadConfirmDialog
import com.arata.yukarilauncher.ui.dialog.SelectVersionDialog
import com.arata.yukarilauncher.ui.fragment.FragmentWithAnim
import com.arata.yukarilauncher.ui.subassembly.adapter.ObjectSpinnerAdapter
import com.arata.yukarilauncher.ui.subassembly.versionlist.VersionSelectedListener
import com.arata.yukarilauncher.feature.version.VersionsManager
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.anim.AnimUtils.Companion.setVisibilityAnim
import com.skydoves.powerspinner.PowerSpinnerView
import com.arata.yukarilauncher.Tools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.Future

/**
 * リソースダウンロード用の抽象フラグメントです。
 * プラットフォームからの検索、フィルタリング、バッチダウンロード機能を提供します。
 */
abstract class AbstractResourceDownloadFragment(
    parentFragment: Fragment?,
    private val classify: Classify,
    private val categoryList: List<Category>,
    private val showModloader: Boolean,
    private val recommendedPlatform: Platform = Platform.CURSEFORGE
) : FragmentWithAnim(R.layout.fragment_download_resource) {
    private lateinit var binding: FragmentDownloadResourceBinding

    private lateinit var mPlatformAdapter: ObjectSpinnerAdapter<Platform>
    private lateinit var mSortAdapter: ObjectSpinnerAdapter<Sort>
    private lateinit var mCategoryAdapter: ObjectSpinnerAdapter<Category>
    private lateinit var mModLoaderAdapter: ObjectSpinnerAdapter<ModLoader>
    private var mCurrentPlatform: Platform = Platform.CURSEFORGE
    private val mFilters: Filters = Filters()

    private val mInfoAdapter = InfoAdapter(parentFragment,
        object : InfoAdapter.CallSearchListener {
            override fun isLastPage() = mLastPage

            override fun loadMoreResult() {
                mTaskInProgress?.let { return }
                mTaskInProgress = SelfReferencingFuture(SearchApiTask(mCurrentResult))
                    .startOnExecutor(TaskExecutors.getDefault())
            }
        })

    private var mTaskInProgress: Future<*>? = null
    private var mCurrentResult: SearchResult? = null
    protected var mLastPage = false
    private var selectMode = false
    private val selectedMods: MutableMap<String, InfoItem> = LinkedHashMap()

    /**
     * インストールボタンの初期化を行います。
     * @param installButton インストールボタン
     */
    abstract fun initInstallButton(installButton: Button)

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadResourceBinding.inflate(layoutInflater)

        mPlatformAdapter = ObjectSpinnerAdapter(binding.platformSpinner) { platform -> platform.pName }
        mSortAdapter = ObjectSpinnerAdapter(binding.sortSpinner) { sort -> getString(sort.resNameID) }
        mCategoryAdapter = ObjectSpinnerAdapter(binding.categorySpinner) { category -> getString(category.resNameID) }
        mModLoaderAdapter = ObjectSpinnerAdapter(binding.modloaderSpinner) { modloader ->
            if (modloader == ModLoader.ALL) getString(R.string.generic_all)
            else modloader.loaderName
        }

        return binding.root
    }

    /**
     * ビュー作成後の初期化処理を行います。
     * スピナーの設定、検索ボタン、フィルターの初期化を行います。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                layoutAnimation = LayoutAnimationController(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fade_downwards)
                )
                addOnScrollListener(object : OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val lm = layoutManager as LinearLayoutManager
                        val lastPosition = lm.findLastVisibleItemPosition()
                        setVisibilityAnim(backToTop, lastPosition >= 12)
                    }
                })
                adapter = mInfoAdapter
            }

            backToTop.setOnClickListener { recyclerView.smoothScrollToPosition(0) }

            searchView.setOnClickListener { search() }
            nameEdit.doAfterTextChanged { text ->
                mFilters.name = text?.toString() ?: ""
            }
            nameEdit.setOnEditorActionListener { _, _, _ ->
                search()
                nameEdit.clearFocus()
                false
            }

            // バージョン選択ダイアログを開く
            selectedMcVersionView.setOnClickListener {
                val selectVersionDialog = SelectVersionDialog(requireContext())
                selectVersionDialog.setOnVersionSelectedListener(object : VersionSelectedListener() {
                    override fun onVersionSelected(version: String?) {
                        selectedMcVersionView.text = version
                        mFilters.mcVersion = version
                        selectVersionDialog.dismiss()
                    }
                })
                selectVersionDialog.show()
            }
            selectedMcVersionView.setOnLongClickListener {
                selectedMcVersionView.text = null
                true
            }
        }

        // スピナーを初期化
        mPlatformAdapter.setItems(Platform.entries)
        mSortAdapter.setItems(Sort.entries)
        mCategoryAdapter.setItems(categoryList)
        mModLoaderAdapter.setItems(ModLoader.entries)

        binding.apply {
            initInstallButton(binding.installButton)
            batchSelectButton.visibility = if (classify == Classify.MODPACK) View.GONE else View.VISIBLE
            batchSelectButton.setOnClickListener {
                if (selectedMods.isNotEmpty()) {
                    resolveAndConfirmSelectedMods()
                } else {
                    selectMode = !selectMode
                    applySelectionState()
                }
            }
            resetSelectedModsButton.setOnClickListener {
                selectedMods.clear()
                applySelectionState()
            }

            setSpinner(platformSpinner, mPlatformAdapter)
            setSpinnerListener<Platform>(platformSpinner) {
                if (mCurrentPlatform == it) return@setSpinnerListener
                mCurrentPlatform = it
                search()
            }

            setSpinner(sortSpinner, mSortAdapter)
            setSpinnerListener<Sort>(sortSpinner) { mFilters.sort = it }

            setSpinner(categorySpinner, mCategoryAdapter)
            setSpinnerListener<Category>(binding.categorySpinner) { mFilters.category = it }

            modloaderLayout.visibility = if (showModloader) {
                setSpinner(modloaderSpinner, mModLoaderAdapter)
                setSpinnerListener<ModLoader>(modloaderSpinner) {
                    mFilters.modloader = it.takeIf { loader -> loader != ModLoader.ALL }
                }
                View.VISIBLE
            } else {
                mFilters.modloader = null
                View.GONE
            }

            initSpinnerIndex()
            applyCurrentVersionFilter()

            reset.setOnClickListener {
                nameEdit.setText("")
                initSpinnerIndex()
                binding.selectedMcVersionView.text = null
                mFilters.mcVersion = null
                if (showModloader) mFilters.modloader = null
            }

            returnButton.setOnClickListener { YLTools.onBackPressed(requireActivity()) }
        }

        checkSearch()
        applySelectionState()
    }

    /**
     * スピナーの設定を行います。
     * @param spinner スピナービュー
     * @param adapter スピナーアダプター
     */
    private fun setSpinner(spinner: PowerSpinnerView, adapter: ObjectSpinnerAdapter<*>) {
        spinner.apply {
            setSpinnerAdapter(adapter)
            setIsFocusable(true)
            lifecycleOwner = this@AbstractResourceDownloadFragment
        }
    }

    /**
     * スピナーの初期インデックスを設定します。
     */
    private fun initSpinnerIndex() {
        binding.apply {
            platformSpinner.selectItemByIndex(recommendedPlatform.ordinal)
            sortSpinner.selectItemByIndex(0)
            categorySpinner.selectItemByIndex(0)
            if (showModloader) modloaderSpinner.selectItemByIndex(0)
        }
    }

    /**
     * 現在のMinecraftバージョンをフィルターに適用します。
     */
    private fun applyCurrentVersionFilter() {
        val versionInfo = VersionsManager.getCurrentVersion()?.getVersionInfo() ?: return
        val mcVersion = versionInfo.minecraftVersion
        if (mcVersion.isNotBlank()) {
            binding.selectedMcVersionView.text = mcVersion
            mFilters.mcVersion = mcVersion
        }

        if (!showModloader) return
        val matchedLoader = versionInfo.loaderInfo
            ?.mapNotNull { loader -> ModLoader.entries.firstOrNull { it.loaderName.equals(loader.name, true) } }
            ?.firstOrNull { it != ModLoader.ALL }
            ?: return

        mFilters.modloader = matchedLoader
        binding.modloaderSpinner.selectItemByIndex(matchedLoader.ordinal)
    }

    /**
     * フラグメント開始時にEventBusを登録します。
     */
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    /**
     * フラグメント停止時にスピナーを閉じ、EventBusの登録を解除します。
     */
    override fun onStop() {
        closeSpinner()
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    /**
     * 検索完了時のUI更新処理を行います。
     */
    private fun onSearchFinished() {
        binding.apply {
            setStatusText(false)
            setLoadingLayout(false)
            setRecyclerView(true)
        }
    }

    /**
     * 検索エラー時のUI更新処理を行います。
     * @param error エラーコード
     */
    private fun onSearchError(error: Int) {
        binding.apply {
            statusText.text = when (error) {
                ERROR_INTERNAL -> getString(R.string.download_search_failed)
                ERROR_PLATFORM_NOT_SUPPORTED -> getString(R.string.download_search_platform_not_supported)
                else -> getString(R.string.download_search_no_result)
            }
        }
        setLoadingLayout(false)
        setRecyclerView(false)
        setStatusText(true)
    }

    /**
     * スライドインアニメーションを実行します。
     * @param animPlayer アニメーションプレイヤー
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operateLayout, Animations.BounceInLeft))
                .apply(AnimPlayer.Entry(downloadLayout, Animations.BounceInDown))
        }
    }

    /**
     * スライドアウトアニメーションを実行します。
     * @param animPlayer アニメーションプレイヤー
     */
    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operateLayout, Animations.FadeOutRight))
                .apply(AnimPlayer.Entry(downloadLayout, Animations.FadeOutUp))
        }
    }

    /**
     * ステータステキストの表示を切り替えます。
     * @param shouldShow 表示するかどうか
     */
    private fun setStatusText(shouldShow: Boolean) {
        setVisibilityAnim(binding.statusText, shouldShow)
    }

    /**
     * ローディングレイアウトの表示を切り替えます。
     * @param shouldShow 表示するかどうか
     */
    private fun setLoadingLayout(shouldShow: Boolean) {
        setVisibilityAnim(binding.loadingLayout, shouldShow)
    }

    /**
     * RecyclerViewの表示を切り替えます。
     * @param shouldShow 表示するかどうか
     */
    private fun setRecyclerView(shouldShow: Boolean) {
        binding.apply {
            recyclerView.visibility = if (shouldShow) View.VISIBLE else View.GONE
            if (shouldShow) recyclerView.scheduleLayoutAnimation()
        }
    }

    /**
     * スピナーのアイテム選択リスナーを設定します。
     * @param spinnerView スピナービュー
     * @param func 選択時のコールバック
     */
    private fun <E> setSpinnerListener(spinnerView: PowerSpinnerView, func: (E) -> Unit) {
        spinnerView.setOnSpinnerItemSelectedListener<E> { _, _, _, newItem -> func(newItem) }
    }

    /**
     * 全てのスピナーを閉じます。
     */
    private fun closeSpinner() {
        binding.platformSpinner.dismiss()
        binding.sortSpinner.dismiss()
        binding.categorySpinner.dismiss()
        binding.modloaderSpinner.dismiss()
    }

    /**
     * 前回の検索状態をクリアしてから検索を実行します。
     */
    private fun search() {
        setStatusText(false)
        setRecyclerView(false)
        setLoadingLayout(true)
        binding.recyclerView.scrollToPosition(0)

        if (mTaskInProgress != null) {
            mTaskInProgress!!.cancel(true)
            mTaskInProgress = null
        }
        this.mLastPage = false
        mTaskInProgress = SelfReferencingFuture(SearchApiTask(null))
            .startOnExecutor(TaskExecutors.getDefault())
    }

    /**
     * アダプター内のアイテム数が0の場合に検索を実行します。
     */
    private fun checkSearch() {
        if (mInfoAdapter.itemCount == 0) search()
    }

    /**
     * 選択状態をUIに反映します。
     */
    private fun applySelectionState() {
        mInfoAdapter.setSelectMode(selectMode) { item ->
            if (selectedMods.containsKey(item.projectId)) selectedMods.remove(item.projectId)
            else selectedMods[item.projectId] = item
            mInfoAdapter.setSelectedProjectIds(selectedMods.keys)
            applySelectionState()
        }
        mInfoAdapter.setSelectedProjectIds(selectedMods.keys)
        binding.batchSelectButton.text = if (selectedMods.isEmpty()) {
            if (selectMode) getString(R.string.download_batch_cancel_select)
            else getString(R.string.generic_select)
        } else {
            "${getString(R.string.download_install)} (${selectedMods.size})"
        }
        binding.resetSelectedModsButton.visibility = if (selectedMods.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * 選択されたModの依存関係を解決し、確認ダイアログを表示します。
     */
    private fun resolveAndConfirmSelectedMods() {
        val currentVersion = VersionsManager.getCurrentVersion()?.getVersionInfo()
        val currentMcVersion = currentVersion?.minecraftVersion
        val currentLoaders = currentVersion?.loaderInfo?.map { it.name }?.toSet() ?: emptySet()
        if (currentMcVersion.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.version_manager_no_installed_version, Toast.LENGTH_SHORT).show()
            return
        }
        setLoadingLayout(true)
        TaskExecutors.getDefault().submit {
            runCatching {
                val resolved = selectedMods.values.mapNotNull { info ->
                    val versions = info.platform.helper.getModVersions(info, false) ?: return@mapNotNull null
                    val selectedVersion = pickLatestCompatibleVersion(versions, currentMcVersion, currentLoaders) ?: return@mapNotNull null
                    BatchModDownloadConfirmDialog.ResolvedDownloadItem(
                        info = info,
                        version = selectedVersion,
                        dependencies = resolveDependencies(selectedVersion, currentMcVersion, currentLoaders),
                        selectedDirectly = true
                    )
                }
                TaskExecutors.runInUIThread {
                    setLoadingLayout(false)
                    if (resolved.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.download_search_no_result, Toast.LENGTH_SHORT).show()
                        return@runInUIThread
                    }
                    BatchModDownloadConfirmDialog(requireContext(), resolved) {
                        val items = it.distinctBy { item -> item.info.projectId }
                        items.forEach { item ->
                            val targetDir = getBatchDownloadTargetDir()
                            val target = java.io.File(targetDir, item.version.fileName)
                            installSelectedItem(item.info, item.version, target)
                        }
                        selectedMods.clear()
                        selectMode = false
                        applySelectionState()
                    }.show()
                }
            }.onFailure { e ->
                Logging.e("BatchDownload", Tools.printToString(e))
                TaskExecutors.runInUIThread {
                    setLoadingLayout(false)
                    Toast.makeText(requireContext(), R.string.download_search_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 指定されたバージョンの依存関係を解決します。
     * @param version バージョン情報
     * @param mcVersion Minecraftバージョン
     * @param loaderNames ローダー名のセット
     * @return 解決されたダウンロードアイテムのリスト
     */
    private fun resolveDependencies(version: VersionItem, mcVersion: String, loaderNames: Set<String>): List<BatchModDownloadConfirmDialog.ResolvedDownloadItem> {
        if (version !is ModVersionItem) return emptyList()
        return version.dependencies.mapNotNull { dependency ->
            val dVersions = dependency.platform.helper.getModVersions(dependency, false) ?: return@mapNotNull null
            val selected = pickLatestCompatibleVersion(dVersions, mcVersion, loaderNames) ?: return@mapNotNull null
            BatchModDownloadConfirmDialog.ResolvedDownloadItem(
                info = dependency,
                version = selected,
                dependencies = emptyList(),
                selectedDirectly = selectedMods.containsKey(dependency.projectId)
            )
        }
    }

    /**
     * 互換性のある最新バージョンを選択します。
     * @param versions バージョンリスト
     * @param mcVersion Minecraftバージョン
     * @param loaderNames ローダー名のセット
     * @return 互換性のある最新バージョン、見つからない場合はnull
     */
    private fun pickLatestCompatibleVersion(
        versions: List<VersionItem>,
        mcVersion: String,
        loaderNames: Set<String>
    ): VersionItem? {
        return versions
            .filter { version -> version.mcVersions.any { it == mcVersion } }
            .filter { version ->
                if (version is ModLikeVersionItem && version.modloaders.isNotEmpty()) {
                    version.modloaders.any { loaderNames.contains(it.loaderName) }
                } else true
            }
            .maxByOrNull { it.uploadDate.time }
    }

    /**
     * バッチダウンロードの保存先ディレクトリを取得します。
     * @return 保存先ディレクトリ
     */
    private fun getBatchDownloadTargetDir(): java.io.File {
        return when (classify) {
            Classify.MOD -> com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.getModsPath()
            Classify.RESOURCE_PACK -> com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.getResourcePackPath()
            Classify.WORLD -> com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.getWorldPath()
            Classify.SHADER_PACK -> com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.getShaderPackPath()
            else -> com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.getModsPath()
        }.apply { mkdirs() }
    }

    /**
     * 選択されたアイテムをインストールします。
     * @param info アイテム情報
     * @param version バージョン情報
     * @param target インストール先ファイル
     */
    private fun installSelectedItem(info: InfoItem, version: VersionItem, target: java.io.File) {
        when (classify) {
            Classify.MOD -> info.platform.helper.installMod(info, version, target, target.absolutePath)
            Classify.RESOURCE_PACK -> info.platform.helper.installResourcePack(info, version, target, target.absolutePath)
            Classify.WORLD -> info.platform.helper.installWorld(info, version, target, target.absolutePath)
            Classify.SHADER_PACK -> info.platform.helper.installShaderPack(info, version, target, target.absolutePath)
            else -> info.platform.helper.installMod(info, version, target, target.absolutePath)
        }
    }

    /**
     * RecyclerViewの有効/無効を切り替えるイベントを処理します。
     * @param event RecyclerView有効化イベント
     */
    @Subscribe
    fun event(event: DownloadPageEvent.RecyclerEnableEvent) {
        binding.recyclerView.isEnabled = event.enable
        closeSpinner()
    }

    /**
     * ページ切り替えイベントを処理し、対応するアニメーションを実行します。
     * @param event ページ切り替えイベント
     */
    @Subscribe
    fun event(event: DownloadPageEvent.PageSwapEvent) {
        closeSpinner()

        if (event.index == classify.type) {
            when (event.classify) {
                IN -> slideIn()
                OUT -> slideOut()
                else -> {}
            }
        }
    }

    /**
     * ページ破棄イベントを処理し、スピナーを閉じます。
     * @param event ページ破棄イベント
     */
    @Subscribe
    fun event(event: DownloadPageEvent.PageDestroyEvent) {
        closeSpinner()
    }

    /**
     * 検索APIを呼び出す内部タスククラスです。
     */
    private inner class SearchApiTask(
        private val mPreviousResult: SearchResult?
    ) : SelfReferencingFuture.FutureInterface {

        /**
         * 非同期で検索を実行します。
         * @param myFuture 自身のFuture
         */
        override fun run(myFuture: Future<*>) {
            runCatching {
                val result: SearchResult? = mCurrentPlatform.helper.search(classify, mFilters, mPreviousResult ?: SearchResult())

                TaskExecutors.runInUIThread {
                    if (myFuture.isCancelled) return@runInUIThread
                    mTaskInProgress = null

                    when {
                        result == null -> {
                            onSearchError(ERROR_INTERNAL)
                        }
                        result.isLastPage -> {
                            if (result.infoItems.isEmpty()) {
                                onSearchError(ERROR_NO_RESULTS)
                            } else {
                                mLastPage = true
                                mInfoAdapter.setItems(result.infoItems)
                                onSearchFinished()
                                return@runInUIThread
                            }
                        }
                        else -> {
                            onSearchFinished()
                        }
                    }

                    if (result == null) {
                        mInfoAdapter.setItems(MOD_ITEMS_EMPTY)
                        return@runInUIThread
                    } else {
                        mInfoAdapter.setItems(result.infoItems)
                        mCurrentResult = result
                    }
                }
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    mInfoAdapter.setItems(MOD_ITEMS_EMPTY)
                    Logging.e("SearchTask", Tools.printToString(e))
                    if (e is PlatformNotSupportedException) {
                        onSearchError(ERROR_PLATFORM_NOT_SUPPORTED)
                    } else {
                        onSearchError(ERROR_NO_RESULTS)
                    }
                }
            }
        }
    }

    companion object {
        /**
         * 空のアイテムリストです。
         */
        private val MOD_ITEMS_EMPTY: MutableList<InfoItem> = ArrayList()

        const val ERROR_INTERNAL: Int = 0
        const val ERROR_NO_RESULTS: Int = 1
        const val ERROR_PLATFORM_NOT_SUPPORTED: Int = 2
    }
}