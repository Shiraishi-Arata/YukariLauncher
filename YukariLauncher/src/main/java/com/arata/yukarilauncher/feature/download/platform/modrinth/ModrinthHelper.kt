package com.arata.yukarilauncher.feature.download.platform.modrinth

import com.arata.yukarilauncher.feature.download.Filters
import com.arata.yukarilauncher.feature.download.enums.Classify
import com.arata.yukarilauncher.feature.download.install.InstallHelper
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.item.ModLoaderWrapper
import com.arata.yukarilauncher.feature.download.item.ScreenshotItem
import com.arata.yukarilauncher.feature.download.item.SearchResult
import com.arata.yukarilauncher.feature.download.item.VersionItem
import com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper
import com.arata.yukarilauncher.feature.download.platform.PlatformNotSupportedException
import net.kdt.pojavlaunch.modloaders.modpacks.api.ApiHandler
import java.io.File

class ModrinthHelper : AbstractPlatformHelper(ApiHandler("https://api.modrinth.com/v2")) {
    /**
     * 自身のコピーを生成する
     * @return 新しいModrinthHelperインスタンス
     */
/**
 * copyする
 */
    override fun copy(): AbstractPlatformHelper {
        return ModrinthHelper()
    }

    /**
     * ModrinthのWebページURLを取得する
     * @param infoItem 対象のInfoItem
     * @return WebページのURL。対応する分類がない場合はnull
     */
/**
 * getWebUrlする
 */
    override fun getWebUrl(infoItem: InfoItem): String? {
        return "https://modrinth.com/${
            when (infoItem.classify) {
                Classify.ALL -> return null
                Classify.MOD -> "mod"
                Classify.MODPACK -> "modpack"
                Classify.RESOURCE_PACK -> "resourcepack"
                Classify.WORLD -> return null
                Classify.SHADER_PACK -> "shader"
            }
        }/${infoItem.slug}"
    }

    /**
     * プロジェクトのスクリーンショット一覧を取得する
     * @param projectId プロジェクトID
     * @return スクリーンショットアイテムのリスト
     */
/**
 * getScreenshotsする
 */
    override fun getScreenshots(projectId: String): List<ScreenshotItem> {
        return ModrinthCommonUtils.getScreenshots(api, projectId)
    }

    /**
     * Modを検索する
     * @param filters 検索フィルター
     * @param lastResult 前回の検索結果
     * @return 検索結果。失敗時はnull
     */
    @Throws(Throwable::class)
/**
 * searchModする
 */
    override fun searchMod(filters: Filters, lastResult: SearchResult): SearchResult? {
        return ModrinthModHelper.modLikeSearch(api, lastResult, filters, "mod", Classify.MOD)
    }

    /**
     * Modパックを検索する
     * @param filters 検索フィルター
     * @param lastResult 前回の検索結果
     * @return 検索結果。失敗時はnull
     */
    @Throws(Throwable::class)
/**
 * searchModPackする
 */
    override fun searchModPack(filters: Filters, lastResult: SearchResult): SearchResult? {
        return ModrinthModHelper.modLikeSearch(api, lastResult, filters, "modpack", Classify.MODPACK)
    }

    /**
     * リソースパックを検索する
     * @param filters 検索フィルター
     * @param lastResult 前回の検索結果
     * @return 検索結果。失敗時はnull
     */
    @Throws(Throwable::class)
/**
 * searchResourcePackする
 */
    override fun searchResourcePack(filters: Filters, lastResult: SearchResult): SearchResult? {
        return ModrinthCommonUtils.getResults(api, lastResult, filters, "resourcepack", Classify.RESOURCE_PACK)
    }

    /**
     * ワールドを検索する（Modrinthでは未サポート）
     * @param filters 検索フィルター
     * @param lastResult 前回の検索結果
     * @return 常にPlatformNotSupportedExceptionをスロー
     */
    @Throws(Throwable::class)
/**
 * searchWorldする
 */
    override fun searchWorld(filters: Filters, lastResult: SearchResult): SearchResult? {
        throw PlatformNotSupportedException("Modrinth does not provide archive download support.") // ModrinthはMCワールドを提供していない
    }

    /**
     * シェーダーパックを検索する
     * @param filters 検索フィルター
     * @param lastResult 前回の検索結果
     * @return 検索結果。失敗時はnull
     */
    @Throws(Throwable::class)
/**
 * searchShaderPackする
 */
    override fun searchShaderPack(filters: Filters, lastResult: SearchResult): SearchResult? {
        return ModrinthCommonUtils.getResults(api, lastResult, filters, "shader", Classify.SHADER_PACK)
    }

    /**
     * Modのバージョン一覧を取得する
     * @param infoItem 対象のInfoItem
     * @param force キャッシュを使用せず強制的に取得するかどうか
     * @return バージョンアイテムのリスト
     */
    @Throws(Throwable::class)
/**
 * getModVersionsする
 */
    override fun getModVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return ModrinthModHelper.getModVersions(api, infoItem, force)
    }

    /**
     * Modパックのバージョン一覧を取得する
     * @param infoItem 対象のInfoItem
     * @param force キャッシュを使用せず強制的に取得するかどうか
     * @return バージョンアイテムのリスト
     */
    @Throws(Throwable::class)
/**
 * getModPackVersionsする
 */
    override fun getModPackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return ModrinthModHelper.getModPackVersions(api, infoItem, force)
    }

    /**
     * リソースパックのバージョン一覧を取得する
     * @param infoItem 対象のInfoItem
     * @param force キャッシュを使用せず強制的に取得するかどうか
     * @return バージョンアイテムのリスト
     */
    @Throws(Throwable::class)
/**
 * getResourcePackVersionsする
 */
    override fun getResourcePackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return ModrinthCommonUtils.getVersions(api, infoItem, force)
    }

    /**
     * ワールドのバージョン一覧を取得する（Modrinthでは未サポート）
     * @param infoItem 対象のInfoItem
     * @param force キャッシュを使用せず強制的に取得するかどうか
     * @return 常にPlatformNotSupportedExceptionをスロー
     */
    @Throws(Throwable::class)
/**
 * getWorldVersionsする
 */
    override fun getWorldVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        throw PlatformNotSupportedException("Modrinth does not provide archive download support.") // ModrinthはMCワールドを提供していない
    }

    /**
     * シェーダーパックのバージョン一覧を取得する
     * @param infoItem 対象のInfoItem
     * @param force キャッシュを使用せず強制的に取得するかどうか
     * @return バージョンアイテムのリスト
     */
    @Throws(Throwable::class)
/**
 * getShaderPackVersionsする
 */
    override fun getShaderPackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return ModrinthCommonUtils.getVersions(api, infoItem, force)
    }

    /**
     * Modをインストールする（ファイルダウンロード）
     * @param infoItem 対象のInfoItem
     * @param version インストールするバージョン
     * @param targetPath インストール先のパス
     * @param progressKey 進捗表示用のキー
     */
    @Throws(Throwable::class)
/**
 * installModする
 */
    override fun installMod(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        InstallHelper.downloadFile(version, targetPath, progressKey)
    }

    /**
     * Modパックをインストールする
     * @param version インストールするバージョン
     * @param customName カスタムバージョン名
     * @return ModLoaderのラッパー情報
     */
    @Throws(Throwable::class)
/**
 * installModPackする
 */
    override fun installModPack(version: VersionItem, customName: String): ModLoaderWrapper? {
        return ModrinthModPackInstallHelper.startInstall(version, customName)
    }

    /**
     * リソースパックをインストールする（ファイルダウンロード）
     * @param infoItem 対象のInfoItem
     * @param version インストールするバージョン
     * @param targetPath インストール先のパス
     * @param progressKey 進捗表示用のキー
     */
    @Throws(Throwable::class)
/**
 * installResourcePackする
 */
    override fun installResourcePack(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        InstallHelper.downloadFile(version, targetPath, progressKey)
    }

    /**
     * ワールドをインストールする（Modrinthでは未サポート）
     * @param infoItem 対象のInfoItem
     * @param version インストールするバージョン
     * @param targetPath インストール先のパス
     * @param progressKey 進捗表示用のキー
     */
    @Throws(Throwable::class)
/**
 * installWorldする
 */
    override fun installWorld(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        throw PlatformNotSupportedException("Modrinth does not provide archive download support.")
    }

    /**
     * シェーダーパックをインストールする（ファイルダウンロード）
     * @param infoItem 対象のInfoItem
     * @param version インストールするバージョン
     * @param targetPath インストール先のパス
     * @param progressKey 進捗表示用のキー
     */
    @Throws(Throwable::class)
/**
 * installShaderPackする
 */
    override fun installShaderPack(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        InstallHelper.downloadFile(version, targetPath, progressKey)
    }
}
