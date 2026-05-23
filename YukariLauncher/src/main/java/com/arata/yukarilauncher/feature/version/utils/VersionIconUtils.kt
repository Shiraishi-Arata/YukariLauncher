package com.arata.yukarilauncher.feature.version.utils

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.feature.version.VersionsManager
import org.apache.commons.io.FileUtils

/**
 * バージョンのアイコンを自動設定、またはカスタムアイコンをリセットするためのクラス
 * @property version 対象のVersion
 */
class VersionIconUtils(
    private val version: Version
) {
    private val iconFile = VersionsManager.getVersionIconFile(version)

    /**
     * バージョンに応じたデフォルトアイコンを識別して設定する（バニラ、ModLoaderのカバー画像など）
     * カスタムアイコンが存在する場合は優先的に設定される
     * @return カスタムアイコンが設定されているかどうか（リセット操作に使用）
     */
    fun start(imageView: ImageView): Boolean {
        val context = imageView.context

        var isIconSet = false
        var isCustomIcon = false

        iconFile.let { icon ->
            if (icon.exists()) {
                Glide.with(imageView)
                    .load(icon)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imageView)
                isIconSet = true
                isCustomIcon = true
            }
        }

        version.getVersionInfo()?.let { versionInfo ->
            versionInfo.loaderInfo?.forEach { loaderInfo ->
                if (!isIconSet) {
                    getLoaderIcon(loaderInfo.name)?.let { icon ->
                        imageView.setImageDrawable(ContextCompat.getDrawable(context, icon))
                        isIconSet = true
                    }
                } else return@forEach
            }
        }

        if (!isIconSet) imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_minecraft))

        return isCustomIcon
    }

    /**
     * カスタムアイコンファイルを削除して、デフォルトアイコンにリセットする
     * **この操作は元に戻せません**
     */
    fun resetIcon() {
        FileUtils.deleteQuietly(iconFile)
    }

    /**
     * @return 現在のバージョンのカバーアイコンファイル
     */
    fun getIconFile() = iconFile

    /**
     * ModLoaderの名前に対応するアイコンリソースIDを取得する
     * @param name ModLoaderの名前
     * @return アイコンのリソースID。該当なしの場合はnull
     */
    private fun getLoaderIcon(name: String): Int? {
        return when(name.lowercase()) {
            "fabric" -> R.drawable.ic_fabric
            "forge" -> R.drawable.ic_anvil
            "quilt" -> R.drawable.ic_quilt
            "neoforge" -> R.drawable.ic_neoforge
            "optifine" -> R.drawable.ic_optifine
            "liteloader" -> R.drawable.ic_chicken_old
            else -> null
        }
    }
}
