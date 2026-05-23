package net.kdt.pojavlaunch.customcontrols.keyboard;

import static org.lwjgl.glfw.CallbackBridge.sendKeyPress;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;

import org.lwjgl.glfw.CallbackBridge;

/** Sends keys via the CallBackBridge */
public class LwjglCharSender implements CharacterSenderStrategy {
/**
 * 「send Backspace」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void sendBackspace() {
        CallbackBridge.sendKeycode(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, '\u0008', 0, 0, true);
        CallbackBridge.sendKeycode(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, '\u0008', 0, 0, false);
    }
/**
 * 「send Enter」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void sendEnter() {
        sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER);
    }
/**
 * 「send Char」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void sendChar(char character) {
        CallbackBridge.sendChar(character, 0);
    }
}
