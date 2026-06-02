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
import com.arata.yukarilauncher.feature.mod.modloader.ForgeDownloadTask
import com.arata.yukarilauncher.feature.version.install.Addon
import com.arata.yukarilauncher.ui.fragment.InstallGameFragment.Companion.BUNDLE_MC_VERSION
import com.arata.yukarilauncher.feature.mod.modloader.ForgeUtils
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Future
import java.util.function.Consumer

/**
 * Forgeをダウンロードするためのフラグメントです。
 * Forgeのバージョン一覧を取得し、選択してインストールできます。
 */
class DownloadForgeFragment : ModListFragment() {
    companion object {
        const val TAG: String = "DownloadForgeFragment"
    }

    /**
     * ビューの初期設定を行います。
     */
    override fun refreshCreatedView() {
        setIcon(ContextCompat.getDrawable(fragmentActivity!!, R.drawable.ic_anvil))
        setTitleText("Forge")
        setLink("https://forums.minecraftforge.net/")
        setMCMod("https://www.mcmod.cn/class/30.html")
        setReleaseCheckBoxGone() //隐藏“仅展示正式版”选择框，在这里没有用处
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
     * 指定されたモードでForgeのバージョン一覧を取得します。
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
                val forgeVersions = ForgeUtils.downloadForgeVersions(force)
                processModDetails(forgeVersions)
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                Logging.e("DownloadForge", Tools.printToString(e))
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
     * Forgeのバージョン情報を処理し、アダプターに設定します。
     * @param forgeVersions Forgeのバージョンリスト
     */
    private fun processModDetails(forgeVersions: List<String>?) {
        forgeVersions ?: run {
            empty()
            return
        }

        val mcVersion = arguments?.getString(BUNDLE_MC_VERSION) ?: throw IllegalArgumentException("The Minecraft version is not passed")

        val mForgeVersions: MutableMap<String, MutableList<String>> = HashMap()
        forgeVersions.forEach(Consumer { forgeVersion: String ->
            currentTask?.apply { if (isCancelled) return@Consumer }

            // MinecraftバージョンとForgeバージョンを検索してグループ化
            val dashIndex = forgeVersion.indexOf("-")
            val gameVersion = forgeVersion.substring(0, dashIndex)
            addIfAbsent(mForgeVersions, gameVersion, forgeVersion)
        })

        currentTask?.apply { if (isCancelled) return }

        val mcForgeVersions = mForgeVersions[mcVersion] ?: run {
            empty()
            return
        }

        val adapter = ModVersionListAdapter(R.drawable.ic_anvil, mcForgeVersions)
        adapter.setOnItemClickListener { version: Any ->
            if (isTaskRunning()) return@setOnItemClickListener false

            val versionString = version.toString()
            EventBus.getDefault().postSticky(
                SelectInstallTaskEvent(
                    Addon.FORGE,
                    versionString,
                    ForgeDownloadTask(versionString)
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