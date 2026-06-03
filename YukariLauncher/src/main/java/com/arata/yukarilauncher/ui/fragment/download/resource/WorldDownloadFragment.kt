package com.arata.yukarilauncher.ui.fragment.download.resource

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.feature.download.enums.Classify
import com.arata.yukarilauncher.feature.download.install.UnpackWorldZipHelper
import com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper.Companion.getWorldPath
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.ui.activity.OpenDocumentWithExtension

/**
 * ワールドデータをダウンロードするためのフラグメントです。
 * オンラインおよびローカルのワールドファイルをインストールできます。
 */
class WorldDownloadFragment(parentFragment: Fragment? = null) : AbstractResourceDownloadFragment(
    parentFragment,
    Classify.WORLD,
    false
) {
    private var openDocumentLauncher: ActivityResultLauncher<Any>? = null

    /**
     * フラグメント作成時にファイル選択ランチャーを初期化します。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension("zip")) { uris: List<Uri>? ->
            if (!uris.isNullOrEmpty()) {
                val uri = uris[0]
                val dialog = YLTools.showTaskRunningDialog(requireContext())
                Task.runTask {
                    val worldFile = FileTools.copyFileInBackground(requireContext(), uri, getWorldPath().absolutePath)
                    runCatching {
                        UnpackWorldZipHelper.unpackFile(worldFile, getWorldPath())
                    }.getOrElse {
                        ContextExecutor.showToast(R.string.download_install_unpack_world_error, Toast.LENGTH_SHORT)
                    }
                }.onThrowable { e ->
                    Tools.showErrorRemote(e)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    dialog.dismiss()
                }.execute()
            }
        }
    }

    /**
     * インストールボタンを初期化し、ローカルワールドファイルの選択を開始します。
     */
    override fun initInstallButton(installButton: Button) {
        installButton.setOnClickListener {
            val suffix = ".zip"
            Toast.makeText(
                requireActivity(),
                String.format(getString(R.string.file_add_file_tip), suffix),
                Toast.LENGTH_SHORT
            ).show()
            openDocumentLauncher?.launch(suffix)
        }
    }
}