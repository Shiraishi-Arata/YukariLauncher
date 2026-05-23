package com.arata.yukarilauncher.feature.download.platform.curseforge

import android.widget.Toast
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.feature.download.Filters
import com.arata.yukarilauncher.feature.download.enums.Classify
import com.arata.yukarilauncher.feature.download.install.InstallHelper
import com.arata.yukarilauncher.feature.download.install.UnpackWorldZipHelper
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.item.ModLoaderWrapper
import com.arata.yukarilauncher.feature.download.item.ScreenshotItem
import com.arata.yukarilauncher.feature.download.item.SearchResult
import com.arata.yukarilauncher.feature.download.item.VersionItem
import com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper
import com.arata.yukarilauncher.feature.download.platform.curseforge.CurseForgeCommonUtils.Companion.CURSEFORGE_MODPACK_CLASS_ID
import com.arata.yukarilauncher.feature.download.platform.curseforge.CurseForgeCommonUtils.Companion.CURSEFORGE_MOD_CLASS_ID
import com.arata.yukarilauncher.feature.download.utils.PlatformUtils
import java.io.File

class CurseForgeHelper : AbstractPlatformHelper(PlatformUtils.createCurseForgeApi()) {
    /**
     * 自身のコピーを生成する
     * @return 新しいCurseForgeHelperインスタンス
     */
/**
 * copyする
 */
    override fun copy(): AbstractPlatformHelper {
        return CurseForgeHelper()
    }

    // スラグを使用してURLを構築するように変更
    /**
     * CurseForgeのWebページURLを取得する
     * @param infoItem 対象のInfoItem
     * @return WebページのURL。対応する分類がない場合はnull
     */
/**
 * getWebUrlする
 */
    override fun getWebUrl(infoItem: InfoItem): String? {
        return "https://www.curseforge.com/minecraft/${
            when (infoItem.classify) {
                Classify.ALL -> return null
                Classify.MOD -> "mc-mods"
                Classify.MODPACK -> "modpacks"
                Classify.RESOURCE_PACK -> "texture-packs"
                Classify.WORLD -> "worlds"
                Classify.SHADER_PACK -> "shaders"
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
        return CurseForgeCommonUtils.getScreenshots(api, projectId)
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
        return CurseForgeModHelper.modLikeSearch(api, lastResult, filters, CURSEFORGE_MOD_CLASS_ID, Classify.MOD)
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
        return CurseForgeModHelper.modLikeSearch(api, lastResult, filters, CURSEFORGE_MODPACK_CLASS_ID, Classify.MODPACK)
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
        return CurseForgeCommonUtils.getResults(api, lastResult, filters, 12, Classify.RESOURCE_PACK)
    }

    /**
     * ワールドを検索する
     * @param filters 検索フィルター
     * @param lastResult 前回の検索結果
     * @return 検索結果。失敗時はnull
     */
    @Throws(Throwable::class)
/**
 * searchWorldする
 */
    override fun searchWorld(filters: Filters, lastResult: SearchResult): SearchResult? {
        return CurseForgeCommonUtils.getResults(api, lastResult, filters, 17, Classify.WORLD)
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
        return CurseForgeCommonUtils.getResults(api, lastResult, filters, 6552, Classify.SHADER_PACK)
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
        return CurseForgeModHelper.getModVersions(api, infoItem, force)
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
        return CurseForgeModHelper.getModPackVersions(api, infoItem, force)
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
        return CurseForgeCommonUtils.getVersions(api, infoItem, force)
    }

    /**
     * ワールドのバージョン一覧を取得する
     * @param infoItem 対象のInfoItem
     * @param force キャッシュを使用せず強制的に取得するかどうか
     * @return バージョンアイテムのリスト
     */
    @Throws(Throwable::class)
/**
 * getWorldVersionsする
 */
    override fun getWorldVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return CurseForgeCommonUtils.getVersions(api, infoItem, force)
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
        return CurseForgeCommonUtils.getVersions(api, infoItem, force)
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
        return CurseForgeModPackInstallHelper.startInstall(api, version, customName)
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
     * ワールドをインストールする（ダウンロード後にZIP展開）
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
        InstallHelper.downloadFile(version, targetPath, progressKey) { file ->
            targetPath.parentFile?.let {
                runCatching {
                    UnpackWorldZipHelper.unpackFile(file, it)
                }.getOrElse {
                    ContextExecutor.showToast(R.string.download_install_unpack_world_error, Toast.LENGTH_SHORT)
                }
            }
        }
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
