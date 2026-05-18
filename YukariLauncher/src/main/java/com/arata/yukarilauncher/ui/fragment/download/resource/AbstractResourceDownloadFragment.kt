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
import net.kdt.pojavlaunch.Tools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.Future

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

    abstract fun initInstallButton(installButton: Button)

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

            // 打开版本选择弹窗
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

        // 初始化 Spinner
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

    private fun setSpinner(spinner: PowerSpinnerView, adapter: ObjectSpinnerAdapter<*>) {
        spinner.apply {
            setSpinnerAdapter(adapter)
            setIsFocusable(true)
            lifecycleOwner = this@AbstractResourceDownloadFragment
        }
    }

    private fun initSpinnerIndex() {
        binding.apply {
            platformSpinner.selectItemByIndex(recommendedPlatform.ordinal)
            sortSpinner.selectItemByIndex(0)
            categorySpinner.selectItemByIndex(0)
            if (showModloader) modloaderSpinner.selectItemByIndex(0)
        }
    }

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

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        closeSpinner()
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun onSearchFinished() {
        binding.apply {
            setStatusText(false)
            setLoadingLayout(false)
            setRecyclerView(true)
        }
    }

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

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operateLayout, Animations.BounceInLeft))
                .apply(AnimPlayer.Entry(downloadLayout, Animations.BounceInDown))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operateLayout, Animations.FadeOutRight))
                .apply(AnimPlayer.Entry(downloadLayout, Animations.FadeOutUp))
        }
    }

    private fun setStatusText(shouldShow: Boolean) {
        setVisibilityAnim(binding.statusText, shouldShow)
    }

    private fun setLoadingLayout(shouldShow: Boolean) {
        setVisibilityAnim(binding.loadingLayout, shouldShow)
    }

    private fun setRecyclerView(shouldShow: Boolean) {
        binding.apply {
            recyclerView.visibility = if (shouldShow) View.VISIBLE else View.GONE
            if (shouldShow) recyclerView.scheduleLayoutAnimation()
        }
    }

    private fun <E> setSpinnerListener(spinnerView: PowerSpinnerView, func: (E) -> Unit) {
        spinnerView.setOnSpinnerItemSelectedListener<E> { _, _, _, newItem -> func(newItem) }
    }

    private fun closeSpinner() {
        binding.platformSpinner.dismiss()
        binding.sortSpinner.dismiss()
        binding.categorySpinner.dismiss()
        binding.modloaderSpinner.dismiss()
    }

    /**
     * 清除上一次的搜索状态，然后执行搜索
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
     * 检查当前适配器内的item数量是否为0，如果是，那么执行搜索
     */
    private fun checkSearch() {
        if (mInfoAdapter.itemCount == 0) search()
    }

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

    private fun getBatchDownloadTargetDir(): java.io.File {
        return when (classify) {
            Classify.MOD -> com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.getModsPath()
            Classify.RESOURCE_PACK -> com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.getResourcePackPath()
            Classify.WORLD -> com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.getWorldPath()
            Classify.SHADER_PACK -> com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.getShaderPackPath()
            else -> com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.getModsPath()
        }.apply { mkdirs() }
    }

    private fun installSelectedItem(info: InfoItem, version: VersionItem, target: java.io.File) {
        when (classify) {
            Classify.MOD -> info.platform.helper.installMod(info, version, target, target.absolutePath)
            Classify.RESOURCE_PACK -> info.platform.helper.installResourcePack(info, version, target, target.absolutePath)
            Classify.WORLD -> info.platform.helper.installWorld(info, version, target, target.absolutePath)
            Classify.SHADER_PACK -> info.platform.helper.installShaderPack(info, version, target, target.absolutePath)
            else -> info.platform.helper.installMod(info, version, target, target.absolutePath)
        }
    }

    @Subscribe
    fun event(event: DownloadPageEvent.RecyclerEnableEvent) {
        binding.recyclerView.isEnabled = event.enable
        closeSpinner()
    }

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

    @Subscribe
    fun event(event: DownloadPageEvent.PageDestroyEvent) {
        closeSpinner()
    }

    private inner class SearchApiTask(
        private val mPreviousResult: SearchResult?
    ) : SelfReferencingFuture.FutureInterface {

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
        private val MOD_ITEMS_EMPTY: MutableList<InfoItem> = ArrayList()

        const val ERROR_INTERNAL: Int = 0
        const val ERROR_NO_RESULTS: Int = 1
        const val ERROR_PLATFORM_NOT_SUPPORTED: Int = 2
    }
}
