package net.kdt.pojavlaunch.customcontrols.gamepad;

import android.util.Log;

import com.google.gson.JsonParseException;
import com.arata.yukarilauncher.utils.path.PathManager;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class GamepadMapStore {
    private static final File STORE_FILE = new File(PathManager.DIR_DATA, "gamepad_map.json");
    private static GamepadMapStore sMapStore;
    private GamepadMap mInMenuMap;
    private GamepadMap mInGameMap;
/**
 * 「create Default」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private static GamepadMapStore createDefault() {
        GamepadMapStore mapStore = new GamepadMapStore();
        mapStore.mInGameMap = GamepadMap.getDefaultGameMap();
        mapStore.mInMenuMap = GamepadMap.getDefaultMenuMap();
        return mapStore;
    }
/**
 * 「load If Necessary」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private static void loadIfNecessary() {
        if(sMapStore == null) return;
        load();
    }
/**
 * 「load」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    public static void load() {
        GamepadMapStore mapStore = null;
        if(STORE_FILE.exists() && STORE_FILE.canRead()) {
            try {
                String storeFileContent = Tools.read(STORE_FILE);
                mapStore = Tools.GLOBAL_GSON.fromJson(storeFileContent, GamepadMapStore.class);
            } catch (JsonParseException | IOException e) {
                Log.w("GamepadMapStore", "Map store failed to load!", e);
            }
        }
        if(mapStore == null) mapStore = createDefault();
        sMapStore = mapStore;
    }
/**
 * 「save」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    public static void save() throws IOException {
        if(sMapStore == null) throw new RuntimeException("Must load map store first!");
        FileUtils.ensureParentDirectory(STORE_FILE);
        String jsonData = Tools.GLOBAL_GSON.toJson(sMapStore);
        Tools.write(STORE_FILE.getAbsolutePath(), jsonData);
    }
/**
 * 「GameMap」の値を取得します。
 */
    public static GamepadMap getGameMap() {
        loadIfNecessary();
        return sMapStore.mInGameMap;
    }
/**
 * 「MenuMap」の値を取得します。
 */
    public static GamepadMap getMenuMap() {
        loadIfNecessary();
        return sMapStore.mInMenuMap;
    }
}
