package com.arata.yukarilauncher.ui.fragment.download.addon

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.event.sticky.SelectInstallTaskEvent
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.mod.modloader.ModVersionListAdapter
import com.arata.yukarilauncher.feature.mod.modloader.NeoForgeDownloadTask
import com.arata.yukarilauncher.feature.mod.modloader.NeoForgeUtils.Companion.downloadNeoForgeVersions
import com.arata.yukarilauncher.feature.mod.modloader.NeoForgeUtils.Companion.downloadNeoForgedForgeVersions
import com.arata.yukarilauncher.feature.mod.modloader.NeoForgeUtils.Companion.formatGameVersion
import com.arata.yukarilauncher.feature.version.install.Addon
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.fragment.InstallGameFragment.Companion.BUNDLE_MC_VERSION
import com.arata.yukarilauncher.ui.subassembly.modlist.ModListFragment
import com.arata.yukarilauncher.utils.YLTools
import net.kdt.pojavlaunch.Tools
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Future
import java.util.function.Consumer

/**
 * NeoForgeをダウンロードするためのフラグメントです。
 * NeoForgeのバージョン一覧を取得し、選択してインストールできます。
 */
class DownloadNeoForgeFragment : ModListFragment() {
    companion object {
        const val TAG: String = "DownloadNeoForgeFragment"
    }

    /**
     * ビューの初期設定を行います。
     */
    override fun refreshCreatedView() {
        setIcon(ContextCompat.getDrawable(fragmentActivity!!, R.drawable.ic_neoforge))
        setTitleText("NeoForge")
        setLink("https://neoforged.net/")
        setMCMod("https://www.mcmod.cn/class/11433.html")
        setReleaseCheckBoxGone() // 「安定版のみ表示」チェックボックスを非表示にする（ここでは不要）
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
     * 指定されたモードでNeoForgeのバージョン一覧を取得します。
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
                processModDetails(loadVersionList(force))
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                Logging.e("DownloadNeoForgeFragment", Tools.printToString(e))
            }
        }
    }

    /**
     * NeoForgeとNeoForgedForgeの両方のバージョンリストを読み込みます。
     * @param force 強制更新するかどうか
     * @return 統合されたバージョンリスト
     */
    @Throws(Exception::class)
    fun loadVersionList(force: Boolean): List<String> {
        val versions: MutableList<String> = ArrayList()
        versions.addAll(downloadNeoForgedForgeVersions(force))
        versions.addAll(downloadNeoForgeVersions(force))

        versions.reverse()

        return versions
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
     * NeoForgeのバージョン情報を処理し、アダプターに設定します。
     * @param neoForgeVersions NeoForgeのバージョンリスト
     */
    private fun processModDetails(neoForgeVersions: List<String>?) {
        neoForgeVersions ?: run {
            empty()
            return
        }

        val mcVersion = arguments?.getString(BUNDLE_MC_VERSION) ?: throw IllegalArgumentException("The Minecraft version is not passed")

        val mNeoForgeVersions: MutableMap<String, MutableList<String>> = HashMap()
        neoForgeVersions.forEach(Consumer { neoForgeVersion: String ->
            currentTask?.apply { if (isCancelled) return@Consumer }
            // MinecraftバージョンとNeoForgeバージョンを検索してグループ化
            val gameVersion = if (neoForgeVersion == "47.1.82") {
                return@Consumer
            } else {
                formatGameVersion(neoForgeVersion)
            }
            addIfAbsent(mNeoForgeVersions, gameVersion, neoForgeVersion)
        })

        currentTask?.apply { if (isCancelled) return }

        val mcNeoForgeVersions = mNeoForgeVersions[mcVersion] ?: run {
            empty()
            return
        }

        val adapter = ModVersionListAdapter(R.drawable.ic_neoforge, mcNeoForgeVersions)
        adapter.setOnItemClickListener { version: Any? ->
            if (isTaskRunning()) return@setOnItemClickListener false

            val versionString = version.toString()
            EventBus.getDefault().postSticky(
                SelectInstallTaskEvent(
                    Addon.NEOFORGE,
                    versionString,
                    NeoForgeDownloadTask(versionString)
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
