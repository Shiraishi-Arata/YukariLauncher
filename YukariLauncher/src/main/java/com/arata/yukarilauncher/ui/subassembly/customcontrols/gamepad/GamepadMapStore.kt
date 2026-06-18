package com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad

import android.util.Log
import com.google.gson.JsonParseException
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.file.FileUtils
import java.io.File
import java.io.IOException

/** ゲームパッドマッピングの永続化ストレージ。JSONファイルとして保存・読み込みを行います。 */
object GamepadMapStore {
    private val STORE_FILE = File(PathManager.DIR_DATA, "gamepad_map.json")
    private var sMapStore: GamepadMapStoreData? = null

    /** @return デフォルト設定のストアデータ */
    private fun createDefault(): GamepadMapStoreData {
        val mapStore = GamepadMapStoreData()
        mapStore.mInGameMap = GamepadMap.getDefaultGameMap()
        mapStore.mInMenuMap = GamepadMap.getDefaultMenuMap()
        return mapStore
    }

    /** 必要に応じてストアを読み込みます。 */
    private fun loadIfNecessary() {
        if (sMapStore == null) return
        load()
    }

    /** ストアをファイルから読み込みます。 */
    fun load() {
        var mapStore: GamepadMapStoreData? = null
        if (STORE_FILE.exists() && STORE_FILE.canRead()) {
            try {
                val storeFileContent = Tools.read(STORE_FILE)
                mapStore = Tools.GLOBAL_GSON.fromJson(storeFileContent, GamepadMapStoreData::class.java)
            } catch (e: Exception) {
                if (e is JsonParseException || e is IOException) {
                    Log.w("GamepadMapStore", "Map store failed to load!", e)
                }
            }
        }
        if (mapStore == null) mapStore = createDefault()
        sMapStore = mapStore
    }

    /**
     * ストアをファイルに保存します。
     * @throws IOException ファイル書き込みエラー
     */
    @Throws(IOException::class)
    fun save() {
        if (sMapStore == null) throw RuntimeException("Must load map store first!")
        FileUtils.ensureParentDirectory(STORE_FILE)
        val jsonData = Tools.GLOBAL_GSON.toJson(sMapStore)
        Tools.write(STORE_FILE.absolutePath, jsonData)
    }

    /** @return ゲーム内用のマップ */
    fun getGameMap(): GamepadMap {
        loadIfNecessary()
        return sMapStore!!.mInGameMap!!
    }

    /** @return メニュー画面用のマップ */
    fun getMenuMap(): GamepadMap {
        loadIfNecessary()
        return sMapStore!!.mInMenuMap!!
    }

    /** ストアの内部データクラス。 */
    private class GamepadMapStoreData {
        var mInMenuMap: GamepadMap? = null
        var mInGameMap: GamepadMap? = null
    }
}