package com.arata.yukarilauncher.ui.subassembly.customcontrols.keyboard

import com.arata.yukarilauncher.feature.awt.AWTInputBridge
import com.arata.yukarilauncher.feature.awt.AWTInputEvent

/** AWT InputBridge経由で文字をMinecraftエンジンに送信する実装。 */
class AwtCharSender : CharacterSenderStrategy {
    override fun sendBackspace() {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_BACK_SPACE)
    }

    override fun sendEnter() {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_ENTER)
    }

    override fun sendChar(character: Char) {
        AWTInputBridge.sendChar(character)
    }
}