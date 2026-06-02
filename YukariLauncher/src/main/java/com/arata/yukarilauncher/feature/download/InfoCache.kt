package com.arata.yukarilauncher.feature.download

import com.arata.yukarilauncher.feature.download.item.DependenciesInfoItem
import com.arata.yukarilauncher.feature.download.item.ModLikeVersionItem
import com.arata.yukarilauncher.feature.download.item.ModVersionItem
import com.arata.yukarilauncher.feature.download.item.VersionItem

/**
 * 検索結果をメモリにキャッシュし、次回読み込み時に前回の検索結果を直接取得できるようにする
 */
class InfoCache {
    abstract class CacheBase<V> {
        private val cache: MutableMap<String, V> = HashMap()

        /**
         * ModIdをキーにして、検索結果をメモリに保存する
         * @param modId ModのID
         * @param value 保存する値
         */
        fun put(modId: String, value: V) {
            cache[modId] = value
        }

        /**
         * ModIdをキーにして、メモリに保存された値を取得する。存在しない場合はnullを返す
         * @param modId ModのID
         * @return 保存された値、またはnull
         */
        fun get(modId: String): V? {
            return cache[modId]
        }

        /**
         * 指定されたModIdがメモリに存在するかどうかを確認する
         * @param modId ModのID
         * @return 存在する場合はtrue
         */
        fun containsKey(modId: String): Boolean {
            return cache.containsKey(modId)
        }
    }

    object DependencyInfoCache : CacheBase<DependenciesInfoItem>()
    object VersionCache : CacheBase<MutableList<VersionItem>>()
    object ModVersionCache : CacheBase<MutableList<ModVersionItem>>()
    object ModPackVersionCache : CacheBase<MutableList<ModLikeVersionItem>>()
}