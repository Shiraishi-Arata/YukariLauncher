package com.arata.yukarilauncher.feature.download.platform.modrinth

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.arata.yukarilauncher.feature.download.Filters
import com.arata.yukarilauncher.feature.download.InfoCache
import com.arata.yukarilauncher.feature.download.enums.Category
import com.arata.yukarilauncher.feature.download.enums.Classify
import com.arata.yukarilauncher.feature.download.enums.Platform
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.item.ScreenshotItem
import com.arata.yukarilauncher.feature.download.item.SearchResult
import com.arata.yukarilauncher.feature.download.item.VersionItem
import com.arata.yukarilauncher.feature.download.platform.PlatformNotSupportedException
import com.arata.yukarilauncher.feature.download.utils.CategoryUtils
import com.arata.yukarilauncher.feature.download.utils.PlatformUtils.Companion.safeRun
import com.arata.yukarilauncher.feature.download.utils.VersionTypeUtils
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.stringutils.StringUtilsKt
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.mod.modpack.api.ApiHandler
import java.util.StringJoiner
import java.util.TreeSet

class ModrinthCommonUtils {
    companion object {
        private const val MODRINTH_SEARCH_COUNT = 20

        /**
         * 検索フィルターからカテゴリのファセット文字列を生成する
         * @param filters 検索フィルター
         * @return ファセット文字列
         */
/**
 * getCategoriesする
 */
        private fun getCategories(filters: Filters): String {
            val categories = mutableListOf<String>().apply {
                filters.modloader?.let { add(it.modrinthName) }
                if (filters.category != Category.ALL) {
                    add(filters.category.modrinthName!!)
                }
            }
            return if (categories.isEmpty()) ""
            else categories.joinToString { "[\"categories:$it\"]" }
        }

        /**
         * APIリクエストのデフォルトパラメータを設定する
         * @param params パラメータマップ
         * @param filters 検索フィルター
         * @param previousCount 現在のオフセット数
         */
/**
 * putDefaultParamsする
 */
        private fun putDefaultParams(params: HashMap<String, Any>, filters: Filters, previousCount: Int) {
            params["query"] = filters.name
            params["limit"] = MODRINTH_SEARCH_COUNT
            params["index"] = filters.sort.modrinth
            params["offset"] = previousCount
        }

        /**
         * JSONレスポンスからすべてのカテゴリを抽出する
         * @param hit JSONオブジェクト
         * @return カテゴリのセット
         */
/**
 * getAllCategoriesする
 */
        internal fun getAllCategories(hit: JsonObject): Set<Category> {
            val list: MutableSet<Category> = TreeSet()
            for (categories in hit["categories"].asJsonArray) {
                val name = categories.asString
                CategoryUtils.getCategoryByModrinth(name)?.let { list.add(it) }
            }
            return list
        }

        /**
         * JSONからアイコンURLを取得する
         * @param hit JSONオブジェクト
         * @return アイコンURL。取得できない場合はnull
         */
/**
 * getIconUrlする
 */
        internal fun getIconUrl(hit: JsonObject): String? {
            return runCatching {
                hit.get("icon_url").asString
            }.getOrNull()
        }

        /**
         * プロジェクトのスクリーンショット一覧を取得する
         * @param api APIハンドラー
         * @param projectId プロジェクトID
         * @return スクリーンショットアイテムのリスト
         */
/**
 * getScreenshotsする
 */
        internal fun getScreenshots(api: ApiHandler, projectId: String): List<ScreenshotItem> {
            searchModFromID(api, projectId)?.let { hit ->
                val screenshotItems: MutableList<ScreenshotItem> = ArrayList()
                hit.getAsJsonArray("gallery").forEach { element ->
                    runCatching {
                        val screenshotObject = element.asJsonObject
                        val url = screenshotObject.get("url").asString

                        val titleElement = screenshotObject.get("title")
                        val titleString = if (titleElement.isJsonNull) null
                        else StringUtilsKt.getNonEmptyOrBlank(titleElement.asString)

                        val descriptionElement = screenshotObject.get("description")
                        val descriptionString = if (descriptionElement.isJsonNull) null
                        else StringUtilsKt.getNonEmptyOrBlank(descriptionElement.asString)

                        screenshotItems.add(ScreenshotItem(url, titleString, descriptionString))
                    }.getOrElse { e ->
                        val error = Tools.printToString(e)
                        Logging.e("ModrinthCommonUtils", "There was an exception while getting the screenshot information!\n$error")
                    }
                }
                return screenshotItems
            }
            return emptyList()
        }

        /**
         * 指定されたタイプのリソースを検索する
         * @param api APIハンドラー
         * @param lastResult 前回の検索結果
         * @param filters 検索フィルター
         * @param type プロジェクトタイプ
         * @param classify 分類タイプ
         * @return 検索結果
         */
/**
 * getResultsする
 */
        internal fun getResults(api: ApiHandler, lastResult: SearchResult, filters: Filters, type: String, classify: Classify): SearchResult? {
            if (filters.category != Category.ALL && filters.category.modrinthName == null) {
                throw PlatformNotSupportedException("The platform does not support the ${filters.category} category!")
            }

            val response = api.get("search", getParams(lastResult, filters, type), JsonObject::class.java) ?: return null
            val responseHits = response.getAsJsonArray("hits") ?: return null

            val infoItems: MutableList<InfoItem> = ArrayList()
            for (responseHit in responseHits) {
                val hit = responseHit.asJsonObject
                getInfoItem(hit, classify)?.let { item ->
                    infoItems.add(item)
                }
            }

            return returnResults(lastResult, infoItems, response, responseHits)
        }

        /**
         * 検索用のパラメータマップを生成する
         * @param lastResult 前回の検索結果
         * @param filters 検索フィルター
         * @param type プロジェクトタイプ
         * @return パラメータマップ
         */
/**
 * getParamsする
 */
        internal fun getParams(lastResult: SearchResult, filters: Filters, type: String): HashMap<String, Any> {
            val params = HashMap<String, Any>()
            val facetString = StringJoiner(",", "[", "]")
            facetString.add("[\"project_type:$type\"]")

            filters.mcVersion?.let { facetString.add("[\"versions:$it\"]") }
            getCategories(filters).let { if (it.isNotBlank()) facetString.add(it) }

            params["facets"] = facetString.toString()
            putDefaultParams(params, filters, lastResult.previousCount)

            return params
        }

        /**
         * JSONオブジェクトからInfoItemを生成する
         * @param hit JSONデータオブジェクト
         * @param classify 分類タイプ
         * @return InfoItem。データパックの場合はnull
         */
/**
 * getInfoItemする
 */
        private fun getInfoItem(hit: JsonObject, classify: Classify): InfoItem? {
            val categories = hit.get("categories").asJsonArray
            for (category in categories) {
                if (category.asString == "datapack") return null // データパックのインストール需要はないため、一律除外
            }
            return InfoItem(
                classify,
                Platform.MODRINTH,
                hit.get("project_id").asString,
                hit.get("slug").asString,
                arrayOf(hit.get("author").asString),
                hit.get("title").asString,
                hit.get("description").asString,
                hit.get("downloads").asLong,
                YLTools.getDate(hit.get("date_created").asString),
                getIconUrl(hit),
                getAllCategories(hit).toList(),
            )
        }

        /**
         * プロジェクトIDからInfoItemを取得する
         * @param api APIハンドラー
         * @param classify 分類タイプ
         * @param projectId プロジェクトID
         * @return InfoItem。見つからない場合はnull
         */
/**
 * getInfoする
 */
        fun getInfo(api: ApiHandler, classify: Classify, projectId: String): InfoItem? {
            searchModFromID(api, projectId)?.let { hit ->
                return InfoItem(
                    classify,
                    Platform.MODRINTH,
                    projectId,
                    hit.get("slug").asString,
                    null,
                    hit.get("title").asString,
                    hit.get("description").asString,
                    hit.get("downloads").asLong,
                    YLTools.getDate(hit.get("published").asString),
                    getIconUrl(hit),
                    getAllCategories(hit).toList()
                )
            }
            return null
        }

        /**
         * 共通のバージョン一覧取得処理（キャッシュ対応、ジェネリック版）
         * @param api APIハンドラー
         * @param infoItem 対象のInfoItem
         * @param force キャッシュを無視するかどうか
         * @param cache 使用するキャッシュ
         * @param createItem バージョンアイテムを生成するラムダ
         * @return バージョンアイテムのリスト
         */
        @Throws(Throwable::class)
        internal fun <T> getCommonVersions(
            api: ApiHandler,
            infoItem: InfoItem,
            force: Boolean,
            cache: InfoCache.CacheBase<MutableList<T>>,
            createItem: (JsonObject, JsonObject, MutableList<String>) -> T
        ): List<T>? {
            if (!force && cache.containsKey(infoItem.projectId))
                return cache.get(infoItem.projectId)

            val response = api.get("project/${infoItem.projectId}/version", JsonArray::class.java) ?: return null

            val items: MutableList<T> = ArrayList()
            // 初回の依存関係情報取得に失敗した場合、そのIDを記録して以降は試行しない
            val invalidDependencies: MutableList<String> = ArrayList()
            for (element in response) {
                try {
                    val versionObject = element.asJsonObject
                    val filesJsonObject: JsonObject = versionObject.getAsJsonArray("files").get(0).asJsonObject

                    items.add(createItem(versionObject, filesJsonObject, invalidDependencies))
                } catch (e: Exception) {
                    Logging.e("ModrinthHelper", Tools.printToString(e))
                    continue
                }
            }

            cache.put(infoItem.projectId, items)
            return items
        }

        /**
         * バージョン一覧を取得する（キャッシュ対応）
         * @param api APIハンドラー
         * @param infoItem 対象のInfoItem
         * @param force キャッシュを無視して強制的に取得するかどうか
         * @return バージョンアイテムのリスト
         */
        @Throws(Throwable::class)
/**
 * getVersionsする
 */
        internal fun getVersions(api: ApiHandler, infoItem: InfoItem, force: Boolean): List<VersionItem>? {
            return getCommonVersions(
                api, infoItem, force, InfoCache.VersionCache
            ) { versionObject, filesJsonObject, _ ->
                VersionItem(
                    infoItem.projectId,
                    versionObject.get("name").asString,
                    versionObject.get("downloads").asLong,
                    YLTools.getDate(versionObject.get("date_published").asString),
                    getMcVersions(versionObject.getAsJsonArray("game_versions")),
                    VersionTypeUtils.getVersionType(versionObject.get("version_type").asString),
                    filesJsonObject.get("filename").asString,
                    getSha1Hash(filesJsonObject),
                    filesJsonObject.get("url").asString
                )
            }
        }

        /**
         * JSON配列からMinecraftバージョン文字列のリストを取得する
         * @param gameVersionJson ゲームバージョンのJSON配列
         * @return Minecraftバージョンのリスト
         */
/**
 * getMcVersionsする
 */
        internal fun getMcVersions(gameVersionJson: JsonArray): List<String> {
            val mcVersions: MutableList<String> = java.util.ArrayList()
            for (gameVersion in gameVersionJson) {
                mcVersions.add(gameVersion.asString)
            }
            return mcVersions
        }

        /**
         * JSONファイルオブジェクトからSHA-1ハッシュを取得する
         * @param filesJsonObject ファイル情報のJSONオブジェクト
         * @return SHA-1ハッシュ値。存在しない場合はnull
         */
/**
 * getSha1Hashする
 */
        internal fun getSha1Hash(filesJsonObject: JsonObject): String? {
            val hashesMap = filesJsonObject.getAsJsonObject("hashes")
            return if ((hashesMap != null && hashesMap.has("sha1"))) hashesMap["sha1"].asString else null
        }

        /**
         * IDによるMod情報の検索を実行する
         * @param api APIハンドラー
         * @param id 検索するModのID
         * @return JSONレスポンス。見つからない場合はnull
         */
/**
 * searchModFromIDする
 */
        internal fun searchModFromID(api: ApiHandler, id: String): JsonObject? {
            return api.safeRun { get("project/$id", JsonObject::class.java) }?.also {
                Logging.i("Modrinth_searchModFromID", it.toString())
            }
        }

        /**
         * 検索結果をラップして返す
         * @param lastResult 前回の検索結果
         * @param infoItems 追加するInfoItemのリスト
         * @param response レスポンスオブジェクト
         * @param responseHits レスポンスのヒット配列
         * @return 更新されたSearchResult
         */
/**
 * returnResultsする
 */
        internal fun returnResults(
            lastResult: SearchResult,
            infoItems: List<InfoItem>,
            response: JsonObject,
            responseHits: JsonArray
        ): SearchResult = lastResult.apply {
            this.infoItems.addAll(infoItems)
            this.previousCount += responseHits.size()
            this.totalResultCount = response.get("total_hits").asInt
            this.isLastPage = responseHits.size() < MODRINTH_SEARCH_COUNT
        }
    }
}