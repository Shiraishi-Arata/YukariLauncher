package net.kdt.pojavlaunch.customcontrols.gamepad;

import net.kdt.pojavlaunch.GrabListener;

import org.lwjgl.glfw.CallbackBridge;

public class DefaultDataProvider implements GamepadDataProvider {
    public static final DefaultDataProvider INSTANCE = new DefaultDataProvider();
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */

    // Cannot instantiate this class publicly
    private DefaultDataProvider() {}
/**
 * 「GameMap」の値を取得します。
 */
    @Override
    public GamepadMap getGameMap() {
        return GamepadMapStore.getGameMap();
    }
/**
 * 「MenuMap」の値を取得します。
 */

    @Override
    public GamepadMap getMenuMap() {
        return GamepadMapStore.getMenuMap();
    }
/**
 * このオブジェクトが「Grabbing」状態であるかを判定します。
 */
    @Override
    public boolean isGrabbing() {
        return CallbackBridge.isGrabbing();
    }
/**
 * 「attach Grab Listener」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void attachGrabListener(GrabListener grabListener) {
        CallbackBridge.addGrabListener(grabListener);
    }
}
