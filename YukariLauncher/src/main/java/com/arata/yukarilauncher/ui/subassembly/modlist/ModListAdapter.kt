package com.arata.yukarilauncher.ui.subassembly.modlist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.ItemModDownloadBinding
import com.arata.yukarilauncher.feature.download.enums.DependencyType
import com.arata.yukarilauncher.feature.download.enums.ModLoader
import com.arata.yukarilauncher.feature.version.utils.VersionIconUtils
import com.arata.yukarilauncher.feature.version.VersionsManager

/**
 * Modダウンロード一覧のアダプター。
 */
class ModListAdapter(
    private val fragment: ModListFragment,
    private val mData: MutableList<ModListItemBean>?
) : RecyclerView.Adapter<ModListAdapter.InnerHolder>() {
    private val mCurrentVersion = VersionsManager.getCurrentVersion()
    private val mIconUtils = lazy { mCurrentVersion?.run { VersionIconUtils(mCurrentVersion) } }

    /**
     * ビューホルダーを生成します。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerHolder {
        return InnerHolder(ItemModDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    /**
     * ビューホルダーにModデータをバインドします。
     */
    override fun onBindViewHolder(holder: InnerHolder, position: Int) {
        holder.setData(mData!![position])
    }

    /**
     * アイテム総数を返します。
     */
    override fun getItemCount(): Int {
        return mData?.size ?: 0
    }

    /**
     * データを更新する
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<ModListItemBean>?) {
        mData?.clear()
        mData?.addAll(newData!!)
        super.notifyDataSetChanged()
    }

    /** データリストを取得する */
    val data: List<ModListItemBean>?
        get() = mData

    /**
     * Modリストアイテムのビューホルダー
     */
    inner class InnerHolder(private val binding: ItemModDownloadBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        private val mContext = binding.root.context

        /**
         * Modアイテムデータをビューに設定する
         */
        fun setData(item: ModListItemBean) {
            itemView.setOnClickListener {
                fragment.switchToChild(
                    item.getAdapter(),
                    item.title
                )
            }
            binding.apply {
                modVersionId.text = item.title
                val modLoaderInfo = getModLoaderInfo(mContext, item.modloader)
                modloaderIcon.setImageDrawable(modLoaderInfo.first)
                modLoaderInfo.second?.let { name ->
                    modloaderName.visibility = View.VISIBLE
                    modloaderName.text = name
                } ?: run {
                    modloaderName.visibility = View.GONE
                }

                val visibility = if (item.isAdapt) run {
                    mIconUtils.value?.start(currentVersionIcon)
                    root.background.setTint(DependencyType.OPTIONAL.color)
                    View.VISIBLE
                } else run {
                    root.background.setTintList(null)
                    View.GONE
                }

                isAdapt.visibility = visibility
                currentVersionIcon.visibility = visibility

                if (item.isAdapt) {
                    val versionName = mCurrentVersion?.getVersionName()
                    isAdapt.apply {
                        text = versionName?.run {
                            mContext.getString(R.string.download_install_is_adapt, versionName)
                        }
                        isSelected = true
                    }
                }
            }
        }

        /**
         * Modローダーに対応するアイコンと名称を取得する
         * @return Modローダーのアイコンと名前。該当なしの場合は草ブロックとnull
         */
        private fun getModLoaderInfo(context: Context, modLoader: ModLoader?): Pair<Drawable?, String?> {
            val loaderInfo: Pair<Int, String?> = when (modLoader) {
                ModLoader.FORGE -> Pair(R.drawable.ic_anvil, ModLoader.FORGE.loaderName)
                ModLoader.NEOFORGE -> Pair(R.drawable.ic_neoforge, ModLoader.NEOFORGE.loaderName)
                ModLoader.FABRIC -> Pair(R.drawable.ic_fabric, ModLoader.FABRIC.loaderName)
                ModLoader.QUILT -> Pair(R.drawable.ic_quilt, ModLoader.QUILT.loaderName)
                else -> Pair(R.drawable.ic_minecraft, null)
            }

            return Pair(ContextCompat.getDrawable(context, loaderInfo.first), loaderInfo.second)
        }
    }
}