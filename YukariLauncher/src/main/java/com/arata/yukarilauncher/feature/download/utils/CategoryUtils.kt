package com.arata.yukarilauncher.feature.download.utils

import com.arata.yukarilauncher.feature.download.enums.Category
import com.arata.yukarilauncher.feature.download.enums.Classify

class CategoryUtils {
    companion object {
        private val sModCategories = mutableListOf<Category>()
        private val sModPackCategories = mutableListOf<Category>()
        private val sResourcePackCategories = mutableListOf<Category>()
        private val sWorldCategories = mutableListOf<Category>()
        private val sShaderPackCategories = mutableListOf<Category>()

        private val sCategoryMap: List<MutableList<Category>> = listOf(
            sModCategories, sModPackCategories, sResourcePackCategories, sWorldCategories, sShaderPackCategories
        )

/**
 * reloadCategoriesする
 */
        private fun reloadCategories() {
            sCategoryMap.forEach { it.clear() }

            Category.entries.forEach { category ->
                when (category.classify) {
                    Classify.ALL -> sCategoryMap.forEach { it.add(category) }
                    Classify.MOD -> sModCategories.add(category)
                    Classify.MODPACK -> sModPackCategories.add(category)
                    Classify.RESOURCE_PACK -> sResourcePackCategories.add(category)
                    Classify.WORLD -> sWorldCategories.add(category)
                    Classify.SHADER_PACK -> sShaderPackCategories.add(category)
                }
            }
        }

/**
 * getModCategoryする
 */
        fun getModCategory(): List<Category> = sModCategories.ifEmpty {
            reloadCategories()
            sModCategories
        }

/**
 * getModPackCategoryする
 */
        fun getModPackCategory(): List<Category> = sModPackCategories.ifEmpty {
            reloadCategories()
            sModPackCategories
        }

/**
 * getResourcePackCategoryする
 */
        fun getResourcePackCategory(): List<Category> = sResourcePackCategories.ifEmpty {
            reloadCategories()
            sResourcePackCategories
        }

/**
 * getWorldCategoryする
 */
        fun getWorldCategory(): List<Category> = sWorldCategories.ifEmpty {
            reloadCategories()
            sWorldCategories
        }

/**
 * getShaderPackCategoryする
 */
        fun getShaderPackCategory(): List<Category> = sShaderPackCategories.ifEmpty {
            reloadCategories()
            sShaderPackCategories
        }

/**
 * checkForCategoryする
 */
        private fun checkForCategory(func: (category: Category) -> Boolean): Category? {
            Category.entries.forEach { category ->
                if (func.invoke(category)) return category
            }
            return null
        }

/**
 * getCategoryByModrinthする
 */
        fun getCategoryByModrinth(name: String): Category? {
            return checkForCategory { category -> category.modrinthName == name }
        }

/**
 * getCategoryByCurseForgeする
 */
        fun getCategoryByCurseForge(id: String): Category? {
            return checkForCategory { category -> category.curseforgeID == id }
        }
    }
}