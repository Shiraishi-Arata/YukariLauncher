package net.kdt.pojavlaunch.customcontrols.gamepad;

import net.kdt.pojavlaunch.GrabListener;

public interface GamepadDataProvider {
/**
 * 「MenuMap」の値を取得します。
 */
    GamepadMap getMenuMap();
/**
 * 「GameMap」の値を取得します。
 */
    GamepadMap getGameMap();
/**
 * このオブジェクトが「Grabbing」状態であるかを判定します。
 */
    boolean isGrabbing();
/**
 * 「attach Grab Listener」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    void attachGrabListener(GrabListener grabListener);
}
