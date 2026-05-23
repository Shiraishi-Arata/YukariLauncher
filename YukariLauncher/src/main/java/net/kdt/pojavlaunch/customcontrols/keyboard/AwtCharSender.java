package net.kdt.pojavlaunch.customcontrols.keyboard;

import net.kdt.pojavlaunch.AWTInputBridge;
import net.kdt.pojavlaunch.AWTInputEvent;

/** Send chars via the AWT Bridgee */
public class AwtCharSender implements CharacterSenderStrategy {
/**
 * 「send Backspace」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void sendBackspace() {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_BACK_SPACE);
    }
/**
 * 「send Enter」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void sendEnter() {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_ENTER);
    }
/**
 * 「send Char」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void sendChar(char character) {
        AWTInputBridge.sendChar(character);
    }

}
