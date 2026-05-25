package com.arata.yukarilauncher.ui.subassembly.customcontrols.keyboard

import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import org.lwjgl.glfw.CallbackBridge

/** LWJGL経由で文字をMinecraftエンジンに送信する実装。 */
class LwjglCharSender : CharacterSenderStrategy {
    override fun sendBackspace() {
        CallbackBridge.sendKeycode(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE.toInt(), '\u0008', 0, 0, true)
        CallbackBridge.sendKeycode(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE.toInt(), '\u0008', 0, 0, false)
    }

    override fun sendEnter() {
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER.toInt())
    }

    override fun sendChar(character: Char) {
        CallbackBridge.sendChar(character, 0)
    }
}
