package com.arata.yukarilauncher.feature.download

import com.arata.yukarilauncher.feature.download.enums.Category
import com.arata.yukarilauncher.feature.download.enums.ModLoader
import com.arata.yukarilauncher.feature.download.enums.Sort

/**
 * プラットフォームで検索を行う際に、フィルター情報を提供するためのクラス
 */
class Filters {
    var name: String = ""
    var mcVersion: String? = null
    var modloader: ModLoader? = null
    var sort: Sort = Sort.RELEVANT
    var categories: MutableList<Category> = mutableListOf()

    val category: Category
        get() = if (categories.isEmpty()) Category.ALL else categories.first()

    /**
     * フィルターの内容を文字列として返す
     * @return フィルター情報の文字列表現
     */
    override fun toString(): String {
        return "Filters(name='$name', mcVersion=$mcVersion, modloader=$modloader, sort=$sort, categories=$categories)"
    }
}