package com.arata.yukarilauncher.ui.fragment.download.addon

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.event.sticky.SelectInstallTaskEvent
import com.arata.yukarilauncher.feature.log.Logging.e
import com.arata.yukarilauncher.feature.mod.modloader.ModVersionListAdapter
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.subassembly.modlist.ModListFragment
import com.arata.yukarilauncher.utils.YLTools
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.modloaders.FabricVersion
import com.arata.yukarilauncher.feature.mod.modloader.FabricLikeUtils
import com.arata.yukarilauncher.ui.fragment.InstallGameFragment.Companion.BUNDLE_MC_VERSION
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Future

/**
 * Fabric系ローダー（Fabric、Quilt等）をダウンロードするための抽象フラグメントです。
 */
abstract class DownloadFabricLikeFragment(val utils: FabricLikeUtils, val icon: Int) : ModListFragment() {

    /**
     * ビューの初期設定を行います。
     */
    override fun refreshCreatedView() {
        setIcon(ContextCompat.getDrawable(fragmentActivity!!, icon))
        setTitleText(utils.name)
        setLink(utils.webUrl)
        setMCMod(utils.mcModUrl)
        setReleaseCheckBoxGone()
    }

    /**
     * 初回のデータ更新を非同期で実行します。
     * @return 非同期タスクのFuture
     */
    override fun initRefresh(): Future<*>? {
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
     * 指定されたモードでデータを更新します。
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
                val gameVersions = utils.downloadGameVersions(force)
                processInfo(gameVersions, force)
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                e("DownloadFabricLike", Tools.printToString(e))
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
     * 取得したバージョン情報を処理し、アダプターに設定します。
     * @param gameVersions ゲームバージョンの配列
     * @param force 強制更新するかどうか
     */
    private fun processInfo(gameVersions: Array<FabricVersion>?, force: Boolean) {
        if (gameVersions.isNullOrEmpty()) {
            empty()
            return
        }

        val mcVersion = arguments?.getString(BUNDLE_MC_VERSION) ?: throw IllegalArgumentException("The Minecraft version is not passed")

        val mFabricVersions: MutableList<FabricVersion> = ArrayList()
        val loaderVersions: Array<FabricVersion>? = utils.downloadLoaderVersions(force)
        gameVersions.forEach {
            currentTask?.apply { if (isCancelled) return }
            if (it.version == mcVersion) {
                mFabricVersions.addAll(loaderVersions!!.toList())
                return@forEach
            }
        }

        currentTask?.apply { if (isCancelled) return }

        if (mFabricVersions.isEmpty()) {
            empty()
            return
        }

        // 整理されたFabricバージョンにアダプターを設定
        val adapter = ModVersionListAdapter(icon, mFabricVersions)
        adapter.setOnItemClickListener { version ->
            if (isTaskRunning()) return@setOnItemClickListener false
            val loaderVersion = (version as FabricVersion).version

            EventBus.getDefault().postSticky(
                SelectInstallTaskEvent(
                    utils.addon,
                    loaderVersion,
                    utils.getDownloadTask(mcVersion, loaderVersion)
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
                e("Set Adapter", Tools.printToString(e))
            }

            componentProcessing(false)
            recyclerView.scheduleLayoutAnimation()
        }
    }
}