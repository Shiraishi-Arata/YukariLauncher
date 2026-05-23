package com.arata.yukarilauncher.feature.download.platform.curseforge

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
import com.arata.yukarilauncher.utils.MCVersionRegex.Companion.RELEASE_REGEX
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.stringutils.StringUtilsKt
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.modloaders.modpacks.api.ApiHandler
import net.kdt.pojavlaunch.utils.GsonJsonUtils
import java.io.IOException
import java.util.TreeSet
import java.util.function.Consumer

class CurseForgeCommonUtils {
    companion object {
        private const val ALGO_SHA_1 = 1
        private const val CURSEFORGE_MINECRAFT_GAME_ID = 432
        private const val CURSEFORGE_SEARCH_COUNT = 20
        private const val CURSEFORGE_PAGINATION_SIZE = 500
        internal const val CURSEFORGE_MODPACK_CLASS_ID = 4471
        internal const val CURSEFORGE_MOD_CLASS_ID = 6

        /**
         * APIリクエストのデフォルトパラメータを設定する
         * @param params パラメータマップ
         * @param filters 検索フィルター
         * @param index ページネーションのインデックス
         */
/**
 * putDefaultParamsする
 */
        internal fun putDefaultParams(params: HashMap<String, Any>, filters: Filters, index: Int) {
            params["gameId"] = CURSEFORGE_MINECRAFT_GAME_ID
            params["searchFilter"] = filters.name
            params["sortField"] = filters.sort.curseforge
            params["sortOrder"] = "desc"
            params["pageSize"] = CURSEFORGE_SEARCH_COUNT
            filters.category.curseforgeID?.let { if (filters.category != Category.ALL) params["categoryId"] = it }
            filters.mcVersion?.let { if (it.isNotEmpty()) params["gameVersion"] = it }
            params["index"] = index
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
                val id = categories.asJsonObject["id"].asString
                CategoryUtils.getCategoryByCurseForge(id)?.let { list.add(it) }
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
                hit.getAsJsonObject("logo").get("thumbnailUrl").asString
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
            searchModFromID(api, projectId)?.let { jsonObject ->
                val hit = jsonObject.getAsJsonObject("data")
                val screenshotItems: MutableList<ScreenshotItem> = ArrayList()
                hit.getAsJsonArray("screenshots").forEach { element ->
                    val screenshotObject = element.asJsonObject
                    screenshotItems.add(
                        ScreenshotItem(
                            screenshotObject.get("url").asString,
                            StringUtilsKt.getNonEmptyOrBlank(screenshotObject.get("title").asString),
                            StringUtilsKt.getNonEmptyOrBlank(screenshotObject.get("description").asString),
                        )
                    )
                }
                return screenshotItems
            }
            return emptyList()
        }

        /**
         * 指定されたクラスIDのリソースを検索する
         * @param api APIハンドラー
         * @param lastResult 前回の検索結果
         * @param filters 検索フィルター
         * @param classId CurseForgeのクラスID
         * @param classify 分類タイプ
         * @return 検索結果
         */
/**
 * getResultsする
 */
        internal fun getResults(api: ApiHandler, lastResult: SearchResult, filters: Filters, classId: Int, classify: Classify): SearchResult? {
            if (filters.category != Category.ALL && filters.category.curseforgeID == null) {
                throw PlatformNotSupportedException("The platform does not support the ${filters.category} category!")
            }

            val params = HashMap<String, Any>()
            putDefaultParams(params, filters, lastResult.previousCount)
            params["classId"] = classId

            val response = api.get("mods/search", params, JsonObject::class.java) ?: return null
            val dataArray = response.getAsJsonArray("data") ?: return null

            val infoItems: MutableList<InfoItem> = ArrayList()
            for (data in dataArray) {
                val dataElement = data.asJsonObject
                getInfoItem(dataElement, classify)?.let { item ->
                    infoItems.add(item)
                }
            }

            return returnResults(lastResult, infoItems, dataArray, response)
        }

        /**
         * JSONオブジェクトからInfoItemを生成する
         * @param dataObject JSONデータオブジェクト
         * @param classify 分類タイプ
         * @return InfoItem。配布が許可されていない場合はnull
         */
/**
 * getInfoItemする
 */
        internal fun getInfoItem(dataObject: JsonObject, classify: Classify): InfoItem? {
            val allowModDistribution = dataObject.get("allowModDistribution")
            // Gsonはnullを自動的にfalseにキャストするため、問題が発生する
            // そのため、allowModDistributionフラグがnullでない場合のみチェックする
            if (!allowModDistribution.isJsonNull && !allowModDistribution.asBoolean) {
                Logging.i("CurseForgeCommonUtils", "Skipping project ${dataObject["name"].asString} because curseforge sucks")
                return null
            }

            return InfoItem(
                classify,
                Platform.CURSEFORGE,
                dataObject.get("id").asString,
                dataObject.get("slug").asString,
                getAuthors(dataObject.get("authors").asJsonArray).toTypedArray(),
                dataObject.get("name").asString,
                dataObject.get("summary").asString,
                dataObject.get("downloadCount").asLong,
                YLTools.getDate(dataObject.get("dateCreated").asString),
                getIconUrl(dataObject),
                getAllCategories(dataObject).toList(),
            )
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
            if (!force && InfoCache.VersionCache.containsKey(infoItem.projectId)) return InfoCache.VersionCache.get(infoItem.projectId)

            val allData = getPaginatedData(api, infoItem.projectId)

            val versionsItem: MutableList<VersionItem> = ArrayList()
            for (data in allData) {
                try {
                    // バージョン情報の取得
                    val mcVersions: MutableSet<String> = TreeSet()
                    for (gameVersionElement in data.getAsJsonArray("gameVersions")) {
                        val gameVersion = gameVersionElement.asString
                        mcVersions.add(gameVersion)
                    }
                    // MCバージョンではない要素をフィルタリング
                    val releaseRegex = RELEASE_REGEX
                    val nonMCVersion: MutableSet<String> = TreeSet()
                    mcVersions.forEach(Consumer { string: String ->
                        if (!releaseRegex.matcher(string).find()) nonMCVersion.add(string)
                    })
                    if (nonMCVersion.isNotEmpty()) mcVersions.removeAll(nonMCVersion)

                    versionsItem.add(
                        VersionItem(
                            infoItem.projectId,
                            data.get("displayName").asString,
                            data.get("downloadCount").asLong,
                            YLTools.getDate(data.get("fileDate").asString),
                            mcVersions.toList(),
                            VersionTypeUtils.getVersionType(data.get("releaseType").asString),
                            data.get("fileName").asString,
                            getSha1FromData(data),
                            data.get("downloadUrl").asString
                        )
                    )
                } catch (e: Exception) {
                    Logging.e("CurseForgeHelper", Tools.printToString(e))
                    continue
                }
            }

            InfoCache.VersionCache.put(infoItem.projectId, versionsItem)
            return versionsItem
        }

        /**
         * JSON配列から著者リストを取得する
         * @param array 著者情報のJSON配列
         * @return 著者名のリスト
         */
/**
 * getAuthorsする
 */
        internal fun getAuthors(array: JsonArray): List<String> {
            val authors: MutableList<String> = ArrayList()
            for (authorElement in array) {
                val authorObject = authorElement.asJsonObject
                authors.add(authorObject.get("name").asString)
            }
            return authors
        }

        /**
         * JSONからSHA-1ハッシュ値を抽出する
         * @param jsonObject ファイル情報のJSONオブジェクト
         * @return SHA-1ハッシュ値。存在しない場合はnull
         */
/**
 * getSha1FromDataする
 */
        internal fun getSha1FromData(jsonObject: JsonObject): String? {
            val hashes = GsonJsonUtils.getJsonArraySafe(jsonObject, "hashes") ?: return null
            for (jsonElement in hashes) {
                // sha1 = 1; md5 = 2;
                val jsonObject1 = GsonJsonUtils.getJsonObjectSafe(jsonElement)
                if (GsonJsonUtils.getIntSafe(jsonObject1, "algo", -1) == ALGO_SHA_1) {
                    return GsonJsonUtils.getStringSafe(jsonObject1, "value")
                }
            }
            return null
        }

        /**
         * ファイルのダウンロードURLを取得する（公式API→エッジリンクのフォールバック）
         * @param api APIハンドラー
         * @param projectID プロジェクトID
         * @param fileID ファイルID
         * @return ダウンロードURL。取得できない場合はnull
         */
/**
 * getDownloadUrlする
 */
        internal fun getDownloadUrl(api: ApiHandler, projectID: Long, fileID: Long): String? {
            // 最初に公式APIエンドポイントを試す
            val response = api.safeRun { get("mods/$projectID/files/$fileID/download-url", JsonObject::class.java) }
            if (response != null && !response["data"].isJsonNull) return response["data"].asString

            // フォールバックとしてエッジリンクを構築
            val fallbackResponse = api.safeRun { get("mods/$projectID/files/$fileID", JsonObject::class.java) }
            if (fallbackResponse != null && !fallbackResponse["data"].isJsonNull) {
                val modData = fallbackResponse["data"].asJsonObject
                val id = modData["id"].asInt
                return "https://edge.forgecdn.net/files/${id / 1000}/${id % 1000}/${modData["fileName"].asString}"
            }

            return null
        }

        /**
         * ファイルのSHA-1ハッシュを取得する
         * @param api APIハンドラー
         * @param projectID プロジェクトID
         * @param fileID ファイルID
         * @return SHA-1ハッシュ値。取得できない場合はnull
         */
/**
 * getDownloadSha1する
 */
        internal fun getDownloadSha1(api: ApiHandler, projectID: Long, fileID: Long): String? {
            // APIエンドポイントを試す。失敗時はnullを返す
            val response = api.safeRun { get("mods/$projectID/files/$fileID", JsonObject::class.java) }
            val data = GsonJsonUtils.getJsonObjectSafe(response, "data") ?: return null
            return getSha1FromData(data)
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
            return api.safeRun { get("mods/$id", JsonObject::class.java) }?.also {
                Logging.i("CurseForge_searchModFromID", it.toString())
            }
        }

        /**
         * ページネーションに対応した全データを取得する
         * @param api APIハンドラー
         * @param projectId プロジェクトID
         * @return ファイル情報のリスト
         */
        @Throws(IOException::class)
/**
 * getPaginatedDataする
 */
        internal fun getPaginatedData(api: ApiHandler, projectId: String): List<JsonObject> {
            val dataList: MutableList<JsonObject> = ArrayList()
            var index = 0
            while (index != -1) {
                val params = HashMap<String, Any>()
                params["index"] = index
                params["pageSize"] = CURSEFORGE_PAGINATION_SIZE

                val response = api.get("mods/$projectId/files", params, JsonObject::class.java)
                val data = GsonJsonUtils.getJsonArraySafe(response, "data") ?: throw IOException("Invalid data!")

                for (i in 0 until data.size()) {
                    val fileInfo = data[i].asJsonObject
                    if (fileInfo["isServerPack"].asBoolean) continue
                    dataList.add(fileInfo)
                }
                if (data.size() < CURSEFORGE_PAGINATION_SIZE) {
                    index = -1
                    continue
                }
                index += CURSEFORGE_PAGINATION_SIZE
            }

            return dataList
        }

        /**
         * 検索結果をラップして返す
         * @param lastResult 前回の検索結果
         * @param infoItems 追加するInfoItemのリスト
         * @param dataArray レスポンスのデータ配列
         * @param response レスポンスオブジェクト
         * @return 更新されたSearchResult
         */
/**
 * returnResultsする
 */
        internal fun returnResults(
            lastResult: SearchResult,
            infoItems: List<InfoItem>,
            dataArray: JsonArray,
            response: JsonObject
        ): SearchResult = lastResult.apply {
            this.infoItems.addAll(infoItems)
            this.previousCount += dataArray.size()
            this.totalResultCount = response.getAsJsonObject("pagination").get("totalCount").asInt
            this.isLastPage = dataArray.size() < CURSEFORGE_SEARCH_COUNT
        }
    }
}
