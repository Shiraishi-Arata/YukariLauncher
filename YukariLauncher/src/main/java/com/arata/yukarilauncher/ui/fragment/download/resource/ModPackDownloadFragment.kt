package com.arata.yukarilauncher.ui.fragment.download.resource

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.event.value.InstallLocalModpackEvent
import com.arata.yukarilauncher.feature.download.enums.Classify
import com.arata.yukarilauncher.feature.download.utils.CategoryUtils
import com.arata.yukarilauncher.feature.mod.modpack.install.InstallExtra
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.ZHTools
import com.arata.yukarilauncher.utils.anim.ViewAnimUtils.Companion.setViewAnim
import com.arata.yukarilauncher.utils.file.FileTools
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import org.greenrobot.eventbus.EventBus

class ModPackDownloadFragment(parentFragment: Fragment? = null) : AbstractResourceDownloadFragment(
    parentFragment,
    Classify.MODPACK,
    CategoryUtils.getModPackCategory(),
    true
) {
    private var openDocumentLauncher: ActivityResultLauncher<Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension(null)) { uris: List<Uri>? ->
            uris?.let { uriList ->
                uriList[0].let { result ->
                    if (!isTaskRunning()) {
                        val dialog = ZHTools.showTaskRunningDialog(requireContext())
                        Task.runTask {
                            FileTools.copyFileInBackground(requireContext(), result, PathManager.DIR_CACHE.absolutePath)
                        }.ended(TaskExecutors.getAndroidUI()) { modPackFile ->
                            modPackFile?.let {
                                EventBus.getDefault().post(InstallLocalModpackEvent(InstallExtra(true, it.absolutePath)))
                            }
                        }.onThrowable { e ->
                            Tools.showErrorRemote(e)
                        }.finallyTask(TaskExecutors.getAndroidUI()) {
                            dialog.dismiss()
                        }.execute()
                    }
                }
            }
        }
    }

    override fun initInstallButton(installButton: Button) {
        installButton.setOnClickListener {
            if (!isTaskRunning()) {
                Toast.makeText(requireActivity(), getString(R.string.select_modpack_local_tip), Toast.LENGTH_SHORT).show()
                openDocumentLauncher?.launch(null)
            } else {
                setViewAnim(installButton, Animations.Shake)
                Toast.makeText(requireActivity(), getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT).show()
            }
        }
    }
}