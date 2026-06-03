package com.arata.yukarilauncher.feature.download.platform.modrinth

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.arata.yukarilauncher.feature.download.Filters
import com.arata.yukarilauncher.feature.download.InfoCache
import com.arata.yukarilauncher.feature.download.enums.Category
import com.arata.yukarilauncher.feature.download.enums.Classify
import com.arata.yukarilauncher.feature.download.enums.ModLoader
import com.arata.yukarilauncher.feature.download.enums.Platform
import com.arata.yukarilauncher.feature.download.item.DependenciesInfoItem
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.item.ModInfoItem
import com.arata.yukarilauncher.feature.download.item.ModLikeVersionItem
import com.arata.yukarilauncher.feature.download.item.ModVersionItem
import com.arata.yukarilauncher.feature.download.item.SearchResult
import com.arata.yukarilauncher.feature.download.item.VersionItem
import com.arata.yukarilauncher.feature.download.platform.PlatformNotSupportedException
import com.arata.yukarilauncher.feature.download.utils.DependencyUtils
import com.arata.yukarilauncher.feature.download.utils.ModLoaderUtils
import com.arata.yukarilauncher.feature.download.utils.PlatformUtils
import com.arata.yukarilauncher.feature.download.utils.VersionTypeUtils
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.feature.mod.modpack.api.ApiHandler

class ModrinthModHelper {
    companion object {
        /**
         * Mod/Modパックの検索を実行する
         * @param api APIハンドラー
         * @param lastResult 前回の検索結果
         * @param filters 検索フィルター
         * @param type プロジェクトタイプ（"mod"または"modpack"）
         * @param classify 分類タイプ
         * @return 検索結果
         */
        @Throws(Throwable::class)
/**
 * modLikeSearchする
 */
        internal fun modLikeSearch(api: ApiHandler, lastResult: SearchResult, filters: Filters, type: String, classify: Classify): SearchResult? {
            val selectedMR = filters.categories.firstOrNull { it.modrinthName != null }
            if (filters.categories.isNotEmpty() && selectedMR == null) {
                throw PlatformNotSupportedException("The platform does not support the selected categories!")
            }

            PlatformUtils.searchModLikeWithChinese(filters, type == "mod")?.let {
                filters.name = it
            }

            val response = api.get("search",
                ModrinthCommonUtils.getParams(lastResult, filters, type), JsonObject::class.java) ?: return null
            val responseHits = response.getAsJsonArray("hits") ?: return null

            val infoItems: MutableList<InfoItem> = ArrayList()
            responseHit@for (responseHit in responseHits) {
                val hit = responseHit.asJsonObject

                val categories = hit.get("categories").asJsonArray
                val modloaders: MutableList<ModLoader> = ArrayList()
                for (category in categories) {
                    val string = category.asString
                    if (string == "datapack") continue@responseHit // ここでよくデータパックが検索にヒットするため、不思議...
                    ModLoaderUtils.getModLoaderByModrinth(string)?.let { modloaders.add(it) }
                }

                val updatedDate = try { YLTools.getDate(hit.get("updated").asString) } catch (_: Exception) { null }

                infoItems.add(
                    ModInfoItem(
                        classify,
                        Platform.MODRINTH,
                        hit.get("project_id").asString,
                        hit.get("slug").asString,
                        arrayOf(hit.get("author").asString),
                        hit.get("title").asString,
                        hit.get("description").asString,
                        hit.get("downloads").asLong,
                        YLTools.getDate(hit.get("date_created").asString),
                        ModrinthCommonUtils.getIconUrl(hit),
                        ModrinthCommonUtils.getAllCategories(hit).toList(),
                        modloaders,
                        updatedDate
                    )
                )
            }

            return ModrinthCommonUtils.returnResults(lastResult, infoItems, response, responseHits)
        }

        /**
         * Modのバージョン一覧を取得する（依存関係情報付き）
         * @param api APIハンドラー
         * @param infoItem 対象のInfoItem
         * @param force キャッシュを無視するかどうか
         * @return ModVersionItemのリスト
         */
        @Throws(Throwable::class)
/**
 * getModVersionsする
 */
        internal fun getModVersions(api: ApiHandler, infoItem: InfoItem, force: Boolean): List<VersionItem>? {
            return ModrinthCommonUtils.getCommonVersions(
                api, infoItem, force, InfoCache.ModVersionCache
            ) { versionObject, filesJsonObject, invalidDependencies ->
                val dependencies = versionObject.get("dependencies").asJsonArray
                val dependencyInfoItems: MutableList<DependenciesInfoItem> = ArrayList()
                if (dependencies.size() != 0) {
                    for (dependency in dependencies) {
                        val dObject = dependency.asJsonObject
                        val dProjectId = dObject.get("project_id").asString
                        val dependencyType = dObject.get("dependency_type").asString

                        if (invalidDependencies.contains(dProjectId)) continue
                        if (!InfoCache.DependencyInfoCache.containsKey(dProjectId)) {
                            val hit = ModrinthCommonUtils.searchModFromID(api, dProjectId)
                            if (hit != null) {
                                InfoCache.DependencyInfoCache.put(
                                    dProjectId, DependenciesInfoItem(
                                        infoItem.classify,
                                        Platform.MODRINTH,
                                        dProjectId,
                                        hit.get("slug").asString,
                                        null,
                                        hit.get("title").asString,
                                        hit.get("description").asString,
                                        hit.get("downloads").asLong,
                                        YLTools.getDate(hit.get("published").asString),
                                        ModrinthCommonUtils.getIconUrl(hit),
                                        ModrinthCommonUtils.getAllCategories(hit).toList(),
                                        getModLoaders(hit.getAsJsonArray("loaders")),
                                        DependencyUtils.getDependencyTypeFromModrinth(dependencyType)
                                    )
                                )
                            } else invalidDependencies.add(dProjectId)
                        }
                        InfoCache.DependencyInfoCache.get(dProjectId)?.let {
                            dependencyInfoItems.add(it)
                        }
                    }
                }
                ModVersionItem(
                        infoItem.projectId,
                        versionObject.get("name").asString,
                        versionObject.get("downloads").asLong,
                        YLTools.getDate(versionObject.get("date_published").asString),
                        ModrinthCommonUtils.getMcVersions(versionObject.getAsJsonArray("game_versions")),
                        VersionTypeUtils.getVersionType(versionObject.get("version_type").asString),
                        filesJsonObject.get("filename").asString,
                        ModrinthCommonUtils.getSha1Hash(filesJsonObject),
                        filesJsonObject.get("url").asString,
                        getModLoaders(versionObject.getAsJsonArray("loaders")),
                        dependencyInfoItems
                    )
            }
        }

        /**
         * Modパックのバージョン一覧を取得する
         * @param api APIハンドラー
         * @param infoItem 対象のInfoItem
         * @param force キャッシュを無視するかどうか
         * @return ModLikeVersionItemのリスト
         */
        @Throws(Throwable::class)
/**
 * getModPackVersionsする
 */
        internal fun getModPackVersions(api: ApiHandler, infoItem: InfoItem, force: Boolean): List<ModLikeVersionItem>? {
            return ModrinthCommonUtils.getCommonVersions(
                api, infoItem, force, InfoCache.ModPackVersionCache
            ) { versionObject, filesJsonObject, _ ->
                ModLikeVersionItem(
                    infoItem.projectId,
                    versionObject.get("name").asString,
                    versionObject.get("downloads").asLong,
                    YLTools.getDate(versionObject.get("date_published").asString),
                    ModrinthCommonUtils.getMcVersions(versionObject.getAsJsonArray("game_versions")),
                    VersionTypeUtils.getVersionType(versionObject.get("version_type").asString),
                    filesJsonObject.get("filename").asString,
                    ModrinthCommonUtils.getSha1Hash(filesJsonObject),
                    filesJsonObject.get("url").asString,
                    getModLoaders(versionObject.getAsJsonArray("loaders"))
                )
            }
        }

        /**
         * JSON配列からModLoaderのリストを取得する
         * @param jsonArray ローダー名のJSON配列
         * @return ModLoaderのリスト
         */
/**
 * getModLoadersする
 */
        private fun getModLoaders(jsonArray: JsonArray): List<ModLoader> {
            val modLoaders: MutableList<ModLoader> = ArrayList()
            jsonArray.forEach {
                ModLoaderUtils.getModLoader(it.asString)?.let {
                    ml -> modLoaders.add(ml)
                }
            }
            return modLoaders
        }
    }
}