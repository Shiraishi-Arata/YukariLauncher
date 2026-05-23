package net.kdt.pojavlaunch.customcontrols.gamepad;

import android.content.Context;

import com.arata.yukarilauncher.R;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;

public class GamepadMap {


    public static final short MOUSE_SCROLL_DOWN = -1;
    public static final short MOUSE_SCROLL_UP = -2;
    // Made mouse keycodes their own specials because managing special keycodes above 0
    // proved to be complicated
    public static final short MOUSE_LEFT = -3;
    public static final short MOUSE_MIDDLE = -4;
    public static final short MOUSE_RIGHT = -5;
    // Workaround, because GLFW_KEY_UNKNOWN and GLFW_MOUSE_BUTTON_LEFT are both 0
    public static final short UNSPECIFIED = -6;

    /*
    This class is just here to store the mapping
    can be modified to create re-mappable controls I guess

    Be warned, you should define ALL keys if you want to avoid a non defined exception
   */

    public GamepadButton BUTTON_A, BUTTON_B, BUTTON_X, BUTTON_Y, BUTTON_START, BUTTON_SELECT,
                         TRIGGER_RIGHT, TRIGGER_LEFT, SHOULDER_RIGHT, SHOULDER_LEFT, THUMBSTICK_RIGHT,
                         THUMBSTICK_LEFT, DPAD_UP, DPAD_DOWN, DPAD_RIGHT, DPAD_LEFT;

    public GamepadEmulatedButton DIRECTION_FORWARD, DIRECTION_BACKWARD, DIRECTION_RIGHT, DIRECTION_LEFT;
/**
 * 「reset Pressed State」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

    /*
     * すべてのボタンを未押し状態に設定し、必要に応じて入力を送信します
     */
    public void resetPressedState(){
        BUTTON_A.resetButtonState();
        BUTTON_B.resetButtonState();
        BUTTON_X.resetButtonState();
        BUTTON_Y.resetButtonState();

        BUTTON_START.resetButtonState();
        BUTTON_SELECT.resetButtonState();

        TRIGGER_LEFT.resetButtonState();
        TRIGGER_RIGHT.resetButtonState();

        SHOULDER_LEFT.resetButtonState();
        SHOULDER_RIGHT.resetButtonState();

        THUMBSTICK_LEFT.resetButtonState();
        THUMBSTICK_RIGHT.resetButtonState();

        DPAD_UP.resetButtonState();
        DPAD_RIGHT.resetButtonState();
        DPAD_DOWN.resetButtonState();
        DPAD_LEFT.resetButtonState();

    }
/**
 * 「create And Initialize Buttons」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private static GamepadMap createAndInitializeButtons() {
        GamepadMap gamepadMap = new GamepadMap();
        gamepadMap.BUTTON_A = new GamepadButton();
        gamepadMap.BUTTON_B = new GamepadButton();
        gamepadMap.BUTTON_X = new GamepadButton();
        gamepadMap.BUTTON_Y = new GamepadButton();

        gamepadMap.BUTTON_START = new GamepadButton();
        gamepadMap.BUTTON_SELECT = new GamepadButton();

        gamepadMap.TRIGGER_RIGHT = new GamepadButton();
        gamepadMap.TRIGGER_LEFT = new GamepadButton();

        gamepadMap.SHOULDER_RIGHT = new GamepadButton();
        gamepadMap.SHOULDER_LEFT = new GamepadButton();

        gamepadMap.DIRECTION_FORWARD = new GamepadEmulatedButton();
        gamepadMap.DIRECTION_BACKWARD = new GamepadEmulatedButton();
        gamepadMap.DIRECTION_RIGHT = new GamepadEmulatedButton();
        gamepadMap.DIRECTION_LEFT = new GamepadEmulatedButton();

        gamepadMap.THUMBSTICK_RIGHT = new GamepadButton();
        gamepadMap.THUMBSTICK_LEFT = new GamepadButton();

        gamepadMap.DPAD_UP = new GamepadButton();
        gamepadMap.DPAD_RIGHT = new GamepadButton();
        gamepadMap.DPAD_DOWN = new GamepadButton();
        gamepadMap.DPAD_LEFT = new GamepadButton();
        return gamepadMap;
    }
/**
 * 「DefaultGameMap」の値を取得します。
 */

    /*
     * マウスがゲームにグラブされているときに使用される事前設定済みマッピングを返します。
     */
    public static GamepadMap getDefaultGameMap(){
        GamepadMap gameMap = GamepadMap.createEmptyMap();

        gameMap.BUTTON_A.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_SPACE;
        gameMap.BUTTON_B.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_Q;
        gameMap.BUTTON_X.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_E;
        gameMap.BUTTON_Y.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_F;

        gameMap.DIRECTION_FORWARD.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_W;
        gameMap.DIRECTION_BACKWARD.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_S;
        gameMap.DIRECTION_RIGHT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_D;
        gameMap.DIRECTION_LEFT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_A;

        gameMap.DPAD_UP.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT;
        gameMap.DPAD_DOWN.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_O;    // Mod用？
        gameMap.DPAD_RIGHT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_K;   // Mod用？
        gameMap.DPAD_LEFT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_J;    // Mod用？

        gameMap.SHOULDER_LEFT.keycodes[0] = GamepadMap.MOUSE_SCROLL_UP;
        gameMap.SHOULDER_RIGHT.keycodes[0] = GamepadMap.MOUSE_SCROLL_DOWN;

        gameMap.TRIGGER_LEFT.keycodes[0] = GamepadMap.MOUSE_RIGHT;
        gameMap.TRIGGER_RIGHT.keycodes[0] = GamepadMap.MOUSE_LEFT;

        gameMap.THUMBSTICK_LEFT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL;
        gameMap.THUMBSTICK_RIGHT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT;
        gameMap.THUMBSTICK_RIGHT.isToggleable = true;

        gameMap.BUTTON_START.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE;
        gameMap.BUTTON_SELECT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_TAB;

        return gameMap;
    }
/**
 * 「DefaultMenuMap」の値を取得します。
 */

    /*
     * マウスがゲームにグラブされていないときに使用される事前設定済みマッピングを返します。
     */
    public static GamepadMap getDefaultMenuMap(){
        GamepadMap menuMap = GamepadMap.createEmptyMap();

        menuMap.BUTTON_A.keycodes[0] = GamepadMap.MOUSE_LEFT;
        menuMap.BUTTON_B.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE;
        menuMap.BUTTON_X.keycodes[0] = GamepadMap.MOUSE_RIGHT;
        {
            short[] keycodes = menuMap.BUTTON_Y.keycodes;
            keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT;
            keycodes[1] = GamepadMap.MOUSE_RIGHT;
        }

        {
            short[] keycodes = menuMap.DIRECTION_FORWARD.keycodes;
            keycodes[0] = keycodes[1] = keycodes[2] = keycodes[3] = GamepadMap.MOUSE_SCROLL_UP;
        }
        {
            short[] keycodes = menuMap.DIRECTION_BACKWARD.keycodes;
            keycodes[0] = keycodes[1] = keycodes[2] = keycodes[3] = GamepadMap.MOUSE_SCROLL_DOWN;
        }

        menuMap.DPAD_DOWN.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_O; // Mod用？
        menuMap.DPAD_RIGHT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_K; // Mod用？
        menuMap.DPAD_LEFT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_J; // Mod用？

        menuMap.SHOULDER_LEFT.keycodes[0] = GamepadMap.MOUSE_SCROLL_UP;
        menuMap.SHOULDER_RIGHT.keycodes[0] = GamepadMap.MOUSE_SCROLL_DOWN;

        menuMap.BUTTON_SELECT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE;

        return menuMap;
    }
/**
 * 「Buttons」の値を取得します。
 */

    /*
     * コントローラーキーマップのすべてのGamepadEmulatedButtonを返します。
     */
    public GamepadEmulatedButton[] getButtons(){
        return new GamepadEmulatedButton[]{ BUTTON_A, BUTTON_B, BUTTON_X, BUTTON_Y,
                                    BUTTON_SELECT, BUTTON_START,
                                    TRIGGER_LEFT, TRIGGER_RIGHT,
                                    SHOULDER_LEFT, SHOULDER_RIGHT,
                                    THUMBSTICK_LEFT, THUMBSTICK_RIGHT,
                                    DPAD_UP, DPAD_RIGHT, DPAD_DOWN, DPAD_LEFT,
                                    DIRECTION_FORWARD, DIRECTION_BACKWARD,
                                    DIRECTION_LEFT, DIRECTION_RIGHT};
    }

    /*
     * 空のキーコードのみを持つ事前初期化済みのGamepadMapを返します
     */
    @SuppressWarnings("unused") public static GamepadMap createEmptyMap(){
        GamepadMap emptyMap = createAndInitializeButtons();
        for(GamepadEmulatedButton button : emptyMap.getButtons())
            button.keycodes = new short[] {UNSPECIFIED, UNSPECIFIED, UNSPECIFIED, UNSPECIFIED};
        return emptyMap;
    }
/**
 * 「SpecialKeycodeNames」の値を取得します。
 */
    public static String[] getSpecialKeycodeNames(Context context) {
        return new String[]{
                context.getString(R.string.keycode_unspecified),
                context.getString(R.string.keycode_mouse_right),
                context.getString(R.string.keycode_mouse_middle),
                context.getString(R.string.keycode_mouse_left),
                context.getString(R.string.keycode_scroll_up),
                context.getString(R.string.keycode_scroll_down)};
    }
}
