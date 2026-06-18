package com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad

import com.arata.yukarilauncher.ui.view.GrabListener
import org.lwjgl.glfw.CallbackBridge

/** デフォルトのGamepadDataProvider実装。GamepadMapStoreとCallbackBridgeを直接利用します。 */
object DefaultDataProvider : GamepadDataProvider {
    override val gameMap: GamepadMap
        get() = GamepadMapStore.getGameMap()

    override val menuMap: GamepadMap
        get() = GamepadMapStore.getMenuMap()

    override fun isGrabbing(): Boolean = CallbackBridge.isGrabbing()

    override fun attachGrabListener(grabListener: GrabListener) {
        CallbackBridge.addGrabListener(grabListener)
    }
}