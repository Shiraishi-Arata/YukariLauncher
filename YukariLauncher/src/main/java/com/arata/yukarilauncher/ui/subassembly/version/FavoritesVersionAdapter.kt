package com.arata.yukarilauncher.ui.subassembly.version

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.databinding.ItemFileListViewBinding
import com.arata.yukarilauncher.feature.version.favorites.FavoritesVersionUtils

/**
 * お気に入りバージョン選択アダプター
 */
class FavoritesVersionAdapter(private val versionName: String) : RecyclerView.Adapter<FavoritesVersionAdapter.ViewHolder>() {
    private val allCategories = FavoritesVersionUtils.getFavoritesStructure().keys.toList()
    private val favoritesMap = FavoritesVersionUtils.getFavoritesStructure()
    private val selectedCategorySet: MutableSet<String> = HashSet()

    init {
        favoritesMap.forEach { (categoryName, versions) ->
            if (versions.contains(versionName)) {
                selectedCategorySet.add(categoryName)
            }
        }
    }

    /**
     * 選択されたお気に入りカテゴリ名のセットを取得する
     */
    fun getSelectedCategorySet() = selectedCategorySet

    /**
     * ビューホルダーを生成します。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemFileListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    /**
     * ビューホルダーにカテゴリデータをバインドします。
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(allCategories[position])
    }

    /**
     * カテゴリ総数を返します。
     */
    override fun getItemCount(): Int = allCategories.size

    /**
     * お気に入りカテゴリアイテムのビューホルダー
     */
    inner class ViewHolder(private val binding: ItemFileListViewBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * カテゴリデータをビューにバインドする
         */
        fun bind(categoryName: String) {
            binding.apply {
                image.visibility = View.GONE
                name.text = categoryName

                check.setOnClickListener(null)

                if (selectedCategorySet.contains(categoryName)) {
                    check.isChecked = true
                }

                root.setOnClickListener { check.isChecked = !check.isChecked }
                check.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedCategorySet.add(categoryName)
                    else selectedCategorySet.remove(categoryName)
                }
            }
        }
    }
}