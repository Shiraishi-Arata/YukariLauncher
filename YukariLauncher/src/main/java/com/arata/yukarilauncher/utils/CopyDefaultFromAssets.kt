package com.arata.yukarilauncher.utils

import android.content.Context
import com.arata.yukarilauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.IOException

class CopyDefaultFromAssets {
    companion object {
        /**
         * アセットからデフォルトファイルをコピーする
         * コントロールマップディレクトリが空の場合、デフォルトの設定ファイルをアセットからコピーする
         */
        @JvmStatic
        @Throws(IOException::class)
        fun copyFromAssets(context: Context?) {
            // デフォルトのコントロールレイアウト
            if (checkDirectoryEmpty(PathManager.DIR_CTRLMAP_PATH)) {
                Tools.copyAssetFile(context, "yukari.json", PathManager.DIR_CTRLMAP_PATH, false)
            }
        }

        /**
         * 指定されたディレクトリが空かどうかをチェックする
         */
        private fun checkDirectoryEmpty(dir: String?): Boolean {
            val controlDir = dir?.let { File(it) }
            val files = controlDir?.listFiles()
            return files?.isEmpty() ?: true
        }
    }
}
