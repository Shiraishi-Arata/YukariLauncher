package com.arata.yukarilauncher.ui.fragment.download.addon

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.event.sticky.SelectInstallTaskEvent
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.mod.modloader.ModVersionListAdapter
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.subassembly.modlist.ModListFragment
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.mod.modloader.OptiFineDownloadTask
import com.arata.yukarilauncher.feature.version.install.Addon
import com.arata.yukarilauncher.ui.fragment.InstallGameFragment.Companion.BUNDLE_MC_VERSION
import com.arata.yukarilauncher.feature.mod.modloader.OptiFineUtils
import com.arata.yukarilauncher.feature.mod.modloader.OptiFineUtils.OptiFineVersion
import com.arata.yukarilauncher.feature.mod.modloader.OptiFineUtils.OptiFineVersions
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Future


/**
 * OptiFineをダウンロードするためのフラグメントです。
 * OptiFineのバージョン一覧を取得し、選択してインストールできます。
 */
class DownloadOptiFineFragment : ModListFragment() {
    companion object {
        const val TAG: String = "DownloadOptiFineFragment"
    }

    /**
     * ビューの初期設定を行います。
     */
    override fun refreshCreatedView() {
        setIcon(ContextCompat.getDrawable(fragmentActivity!!, R.drawable.ic_optifine))
        setTitleText("OptiFine")
        setLink("https://www.optifine.net/home")
        setMCMod("https://www.mcmod.cn/class/36.html")
        setReleaseCheckBoxGone()
    }

    /**
     * 初回のデータ更新を非同期で実行します。
     * @return 非同期タスクのFuture
     */
    override fun initRefresh(): Future<*> {
        return refresh(false)
    }

    /**
     * データを強制的に更新します。
     * @return 非同期タスクのFuture
     */
    override fun refresh(): Future<*> {
        return refresh(true)
    }

    /**
     * 指定されたモードでOptiFineのバージョン一覧を取得します。
     * @param force 強制更新するかどうか
     * @return 非同期タスクのFuture
     */
    private fun refresh(force: Boolean): Future<*> {
        return TaskExecutors.getDefault().submit {
            runCatching {
                TaskExecutors.runInUIThread {
                    cancelFailedToLoad()
                    componentProcessing(true)
                }
                val optiFineVersions = OptiFineUtils.downloadOptiFineVersions(force)
                processModDetails(optiFineVersions)
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                Logging.e("DownloadOptiFineFragment", Tools.printToString(e))
            }
        }
    }

    /**
     * 空の状態をUIに反映します。
     */
    private fun empty() {
        TaskExecutors.runInUIThread {
            componentProcessing(false)
            setFailedToLoad(getString(R.string.version_install_no_versions))
        }
    }

    /**
     * OptiFineのバージョン情報を処理し、アダプターに設定します。
     * @param optiFineVersions OptiFineのバージョン情報
     */
    private fun processModDetails(optiFineVersions: OptiFineVersions?) {
        optiFineVersions ?: run {
            empty()
            return
        }

        val mcVersion = arguments?.getString(BUNDLE_MC_VERSION) ?: throw IllegalArgumentException("The Minecraft version is not passed")

        val mOptiFineVersions: MutableMap<String, MutableList<OptiFineVersion>> = HashMap()
        optiFineVersions.optifineVersions!!.forEach { optiFineVersionList: List<OptiFineVersion> ->
            currentTask?.apply { if (isCancelled) return@forEach }

            optiFineVersionList.forEach { optiFineVersion: OptiFineVersion ->
                currentTask?.apply { if (isCancelled) return@forEach }
                addIfAbsent(mOptiFineVersions, optiFineVersion.minecraftVersion!!.removePrefix("Minecraft").trim(), optiFineVersion)
            }
        }

        if (currentTask!!.isCancelled) return

        val mcOptiFineVersions = mOptiFineVersions[mcVersion] ?: mOptiFineVersions[mcVersion] ?: run {
            empty()
            return
        }

        val adapter = ModVersionListAdapter(R.drawable.ic_optifine, mcOptiFineVersions)
        adapter.setOnItemClickListener { version: Any ->
            if (isTaskRunning()) return@setOnItemClickListener false

            val optifineVersion = version as OptiFineVersion
            EventBus.getDefault().postSticky(
                SelectInstallTaskEvent(
                    Addon.OPTIFINE,
                    optifineVersion.versionName!!,
                    OptiFineDownloadTask(optifineVersion)
                )
            )
            YLTools.onBackPressed(requireActivity())
            true
        }

        currentTask?.apply { if (isCancelled) return }

        TaskExecutors.runInUIThread {
            val recyclerView = recyclerView
            runCatching {
                recyclerView.layoutManager = LinearLayoutManager(fragmentActivity!!)
                recyclerView.adapter = adapter
            }.getOrElse { e ->
                Logging.e("Set Adapter", Tools.printToString(e))
            }

            componentProcessing(false)
            recyclerView.scheduleLayoutAnimation()
        }
    }
}
