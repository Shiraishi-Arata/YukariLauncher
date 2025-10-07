package com.arata.yukarilauncher.ui.fragment.download.addon

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.event.sticky.SelectInstallTaskEvent
import com.arata.yukarilauncher.feature.download.enums.Classify
import com.arata.yukarilauncher.feature.download.enums.Platform
import com.arata.yukarilauncher.feature.download.item.VersionItem
import com.arata.yukarilauncher.feature.download.platform.modrinth.ModrinthCommonUtils
import com.arata.yukarilauncher.feature.log.Logging.e
import com.arata.yukarilauncher.feature.mod.modloader.FabricLikeApiModDownloadTask
import com.arata.yukarilauncher.feature.mod.modloader.ModVersionListAdapter
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.subassembly.modlist.ModListFragment
import com.arata.yukarilauncher.utils.ZHTools
import net.kdt.pojavlaunch.Tools
import com.arata.yukarilauncher.feature.version.install.Addon
import com.arata.yukarilauncher.ui.fragment.InstallGameFragment.Companion.BUNDLE_MC_VERSION
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Future
import java.util.function.Consumer

abstract class DownloadFabricLikeApiModFragment(
    private val addon: Addon,
    private val projectId: String,
    private val webUrl: String,
    private val mcModUrl: String,
    private val icon: Int
) : ModListFragment() {

    override fun refreshCreatedView() {
        setIcon(ContextCompat.getDrawable(fragmentActivity!!, icon))
        setTitleText(addon.addonName)
        setLink(webUrl)
        setMCMod(mcModUrl)
        setReleaseCheckBoxGone()
    }

    override fun initRefresh(): Future<*>? {
        return refresh(false)
    }

    override fun refresh(): Future<*> {
        return refresh(true)
    }

    private fun refresh(force: Boolean): Future<*> {
        return TaskExecutors.getDefault().submit {
            runCatching {
                TaskExecutors.runInUIThread {
                    cancelFailedToLoad()
                    componentProcessing(true)
                }
                val versions = search(force)
                processInfo(versions)
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                e("DownloadFabricLike", Tools.printToString(e))
            }
        }
    }

    private fun search(force: Boolean): List<VersionItem> {
        val mcVersion = arguments?.getString(BUNDLE_MC_VERSION) ?: throw IllegalArgumentException("The Minecraft version is not passed")

        val helper = Platform.MODRINTH.helper
        ModrinthCommonUtils.getInfo(helper.api, Classify.MOD, projectId)?.let { infoItem ->
            currentTask?.apply { if (isCancelled) return emptyList() }
            helper.getVersions(infoItem, force)?.let { versions ->
                val mModVersionsByMinecraftVersion: MutableMap<String, MutableList<VersionItem>> = HashMap()
                versions.forEach(Consumer { versionItem ->
                    for (version in versionItem.mcVersions) {
                        addIfAbsent(mModVersionsByMinecraftVersion, version, versionItem)
                    }
                })
                return mModVersionsByMinecraftVersion[mcVersion] ?: emptyList()
            }
        }
        return emptyList()
    }

    private fun empty() {
        TaskExecutors.runInUIThread {
            componentProcessing(false)
            setFailedToLoad(getString(R.string.version_install_no_versions))
        }
    }

    private fun processInfo(versions: List<VersionItem>) {
        currentTask?.apply { if (isCancelled) return }

        if (versions.isEmpty()) {
            empty()
            return
        }

        currentTask?.apply { if (isCancelled) return }

        val adapter = ModVersionListAdapter(icon, versions)
        adapter.setOnItemClickListener { version ->
            if (isTaskRunning()) return@setOnItemClickListener false
            val apiVersion = version as VersionItem

            EventBus.getDefault().postSticky(
                SelectInstallTaskEvent(
                    addon,
                    apiVersion.title,
                    FabricLikeApiModDownloadTask(addon.addonName, apiVersion)
                )
            )
            ZHTools.onBackPressed(requireActivity())
            true
        }

        currentTask?.apply { if (isCancelled) return }

        TaskExecutors.runInUIThread {
            val recyclerView = recyclerView
            runCatching {
                recyclerView.layoutManager = LinearLayoutManager(fragmentActivity!!)
                recyclerView.adapter = adapter
            }.getOrElse { e ->
                e("Set Adapter", Tools.printToString(e))
            }

            componentProcessing(false)
            recyclerView.scheduleLayoutAnimation()
        }
    }
}