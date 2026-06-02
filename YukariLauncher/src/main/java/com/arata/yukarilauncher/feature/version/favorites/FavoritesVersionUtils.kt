package com.arata.yukarilauncher.feature.version.favorites

import com.arata.yukarilauncher.feature.version.VersionsManager
import java.util.concurrent.ConcurrentHashMap

/**
 * お気に入りバージョン管理のユーティリティクラス
 * お気に入りフォルダの追加・削除・リネームなどの操作を提供する
 */
class FavoritesVersionUtils private constructor() {
    companion object {
        /**
         * お気に入りマップを変更するための汎用関数
         * @param action 変更操作
         */
        private inline fun modifyFavorites(action: (MutableMap<String, MutableSet<String>>) -> Unit) {
            VersionsManager.currentGameInfo.apply {
                action(favoritesMap)
                saveCurrentInfo()
            }
        }

        /**
         * バージョン名を原子的に変更する
         * @param oldName 古い名前
         * @param newName 新しい名前
         */
        fun renameVersion(oldName: String, newName: String) = modifyFavorites { map ->
            map.values.forEach { versions ->
                if (oldName in versions) {
                    versions.remove(oldName)
                    versions.add(newName)
                }
            }
        }

        /**
         * 新しいお気に入りフォルダを追加する
         * @param name フォルダ名
         */
        fun addFolder(name: String) = modifyFavorites { map ->
            map.putIfAbsent(name, ConcurrentHashMap.newKeySet())
        }

        /**
         * お気に入りフォルダを削除する
         * @param name フォルダ名
         */
        fun removeFolder(name: String) = modifyFavorites { map ->
            map.remove(name)
        }

        /**
         * バージョンのお気に入りフォルダ所属を更新する
         * @param version 対象バージョン
         * @param targetFolders このバージョンを含めるお気に入りフォルダのセット
         */
        fun updateVersionFolders(version: String, targetFolders: Set<String>) = modifyFavorites { map ->
            // ターゲットフォルダに追加
            targetFolders.forEach { folder ->
                map.getOrPut(folder) { ConcurrentHashMap.newKeySet() }.add(version)
            }

            // 非ターゲットフォルダから削除
            map.keys.filterNot { it in targetFolders }.forEach { folder ->
                map[folder]?.remove(version)
            }
        }

        /**
         * 有効なお気に入り構造を取得する
         * @return フォルダ名とバージョンセットのマップ
         */
        fun getFavoritesStructure(): Map<String, Set<String>> =
            VersionsManager.currentGameInfo.favoritesMap.let { map ->
                map.entries.associate { (k, v) -> k to v.toSet() }
            }

        /**
         * 指定されたお気に入りフォルダの有効なバージョンを取得する
         * @param folder フォルダ名
         * @return 有効なバージョン名のセット
         */
        fun getValidVersions(folder: String): Set<String> =
            VersionsManager.currentGameInfo.favoritesMap[folder]
                ?.filter { VersionsManager.checkVersionExistsByName(it) }
                .orEmpty()
                .toSet()
    }
}