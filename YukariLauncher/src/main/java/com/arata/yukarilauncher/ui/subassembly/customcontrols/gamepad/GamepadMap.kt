package com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad

import android.content.Context
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode

/**
 * ゲームパッドのボタンマッピングを保持するクラス。
 * 各物理ボタンやスティックにMinecraftのキーコードを割り当てます。
 */
class GamepadMap {
    companion object {
        /** マウススクロールダウンの特殊キーコード。 */
        const val MOUSE_SCROLL_DOWN: Short = -1
        /** マウススクロールアップの特殊キーコード。 */
        const val MOUSE_SCROLL_UP: Short = -2
        /** マウス左ボタンの特殊キーコード。 */
        const val MOUSE_LEFT: Short = -3
        /** マウス中ボタンの特殊キーコード。 */
        const val MOUSE_MIDDLE: Short = -4
        /** マウス右ボタンの特殊キーコード。 */
        const val MOUSE_RIGHT: Short = -5
        /** 未指定（割り当てなし）の特殊キーコード。 */
        const val UNSPECIFIED: Short = -6

        /** @return デフォルトのゲーム内用マップ */
        fun getDefaultGameMap(): GamepadMap {
            val gameMap = createEmptyMap()

            gameMap.BUTTON_A!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_SPACE.toShort()
            gameMap.BUTTON_B!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_Q.toShort()
            gameMap.BUTTON_X!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_E.toShort()
            gameMap.BUTTON_Y!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_F.toShort()

            gameMap.DIRECTION_FORWARD!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_W.toShort()
            gameMap.DIRECTION_BACKWARD!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_S.toShort()
            gameMap.DIRECTION_RIGHT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_D.toShort()
            gameMap.DIRECTION_LEFT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_A.toShort()

            gameMap.DPAD_UP!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toShort()
            gameMap.DPAD_DOWN!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_O.toShort()
            gameMap.DPAD_RIGHT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_K.toShort()
            gameMap.DPAD_LEFT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_J.toShort()

            gameMap.SHOULDER_LEFT!!.keycodes[0] = MOUSE_SCROLL_UP
            gameMap.SHOULDER_RIGHT!!.keycodes[0] = MOUSE_SCROLL_DOWN

            gameMap.TRIGGER_LEFT!!.keycodes[0] = MOUSE_RIGHT
            gameMap.TRIGGER_RIGHT!!.keycodes[0] = MOUSE_LEFT

            gameMap.THUMBSTICK_LEFT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL.toShort()
            gameMap.THUMBSTICK_RIGHT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toShort()
            gameMap.THUMBSTICK_RIGHT!!.isToggleable = true

            gameMap.BUTTON_START!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toShort()
            gameMap.BUTTON_SELECT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_TAB.toShort()

            return gameMap
        }

        /** @return デフォルトのメニュー画面用マップ */
        fun getDefaultMenuMap(): GamepadMap {
            val menuMap = createEmptyMap()

            menuMap.BUTTON_A!!.keycodes[0] = MOUSE_LEFT
            menuMap.BUTTON_B!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toShort()
            menuMap.BUTTON_X!!.keycodes[0] = MOUSE_RIGHT
            menuMap.BUTTON_Y!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toShort()
            menuMap.BUTTON_Y!!.keycodes[1] = MOUSE_RIGHT

            menuMap.DIRECTION_FORWARD!!.keycodes[0] = MOUSE_SCROLL_UP
            menuMap.DIRECTION_FORWARD!!.keycodes[1] = MOUSE_SCROLL_UP
            menuMap.DIRECTION_FORWARD!!.keycodes[2] = MOUSE_SCROLL_UP
            menuMap.DIRECTION_FORWARD!!.keycodes[3] = MOUSE_SCROLL_UP

            menuMap.DIRECTION_BACKWARD!!.keycodes[0] = MOUSE_SCROLL_DOWN
            menuMap.DIRECTION_BACKWARD!!.keycodes[1] = MOUSE_SCROLL_DOWN
            menuMap.DIRECTION_BACKWARD!!.keycodes[2] = MOUSE_SCROLL_DOWN
            menuMap.DIRECTION_BACKWARD!!.keycodes[3] = MOUSE_SCROLL_DOWN

            menuMap.DPAD_DOWN!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_O.toShort()
            menuMap.DPAD_RIGHT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_K.toShort()
            menuMap.DPAD_LEFT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_J.toShort()

            menuMap.SHOULDER_LEFT!!.keycodes[0] = MOUSE_SCROLL_UP
            menuMap.SHOULDER_RIGHT!!.keycodes[0] = MOUSE_SCROLL_DOWN

            menuMap.BUTTON_SELECT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toShort()

            return menuMap
        }

        /** @return 空のマップ（全てのボタンがUNSPECIFIED） */
        fun createEmptyMap(): GamepadMap {
            val emptyMap = createAndInitializeButtons()
            for (button in emptyMap.buttons) {
                button!!.keycodes = shortArrayOf(UNSPECIFIED, UNSPECIFIED, UNSPECIFIED, UNSPECIFIED)
            }
            return emptyMap
        }

        /** @return 全てのボタンが初期化された新しいGamepadMap */
        private fun createAndInitializeButtons(): GamepadMap {
            val gamepadMap = GamepadMap()
            gamepadMap.BUTTON_A = GamepadButton()
            gamepadMap.BUTTON_B = GamepadButton()
            gamepadMap.BUTTON_X = GamepadButton()
            gamepadMap.BUTTON_Y = GamepadButton()
            gamepadMap.BUTTON_START = GamepadButton()
            gamepadMap.BUTTON_SELECT = GamepadButton()
            gamepadMap.TRIGGER_RIGHT = GamepadButton()
            gamepadMap.TRIGGER_LEFT = GamepadButton()
            gamepadMap.SHOULDER_RIGHT = GamepadButton()
            gamepadMap.SHOULDER_LEFT = GamepadButton()
            gamepadMap.DIRECTION_FORWARD = GamepadEmulatedButton()
            gamepadMap.DIRECTION_BACKWARD = GamepadEmulatedButton()
            gamepadMap.DIRECTION_RIGHT = GamepadEmulatedButton()
            gamepadMap.DIRECTION_LEFT = GamepadEmulatedButton()
            gamepadMap.THUMBSTICK_RIGHT = GamepadButton()
            gamepadMap.THUMBSTICK_LEFT = GamepadButton()
            gamepadMap.DPAD_UP = GamepadButton()
            gamepadMap.DPAD_RIGHT = GamepadButton()
            gamepadMap.DPAD_DOWN = GamepadButton()
            gamepadMap.DPAD_LEFT = GamepadButton()
            return gamepadMap
        }

        /**
         * 特殊キーコードのローカライズ名配列を返します。
         * @param context Androidコンテキスト
         * @return 特殊キーコード名の配列
         */
        fun getSpecialKeycodeNames(context: Context): Array<String> {
            return arrayOf(
                context.getString(R.string.keycode_unspecified),
                context.getString(R.string.keycode_mouse_right),
                context.getString(R.string.keycode_mouse_middle),
                context.getString(R.string.keycode_mouse_left),
                context.getString(R.string.keycode_scroll_up),
                context.getString(R.string.keycode_scroll_down)
            )
        }
    }

    var BUTTON_A: GamepadButton? = null
    var BUTTON_B: GamepadButton? = null
    var BUTTON_X: GamepadButton? = null
    var BUTTON_Y: GamepadButton? = null
    var BUTTON_START: GamepadButton? = null
    var BUTTON_SELECT: GamepadButton? = null
    var TRIGGER_RIGHT: GamepadButton? = null
    var TRIGGER_LEFT: GamepadButton? = null
    var SHOULDER_RIGHT: GamepadButton? = null
    var SHOULDER_LEFT: GamepadButton? = null
    var THUMBSTICK_RIGHT: GamepadButton? = null
    var THUMBSTICK_LEFT: GamepadButton? = null
    var DPAD_UP: GamepadButton? = null
    var DPAD_DOWN: GamepadButton? = null
    var DPAD_RIGHT: GamepadButton? = null
    var DPAD_LEFT: GamepadButton? = null
    var DIRECTION_FORWARD: GamepadEmulatedButton? = null
    var DIRECTION_BACKWARD: GamepadEmulatedButton? = null
    var DIRECTION_RIGHT: GamepadEmulatedButton? = null
    var DIRECTION_LEFT: GamepadEmulatedButton? = null

    /** 全てのボタンの押下状態をリセットします。 */
    fun resetPressedState() {
        BUTTON_A!!.resetButtonState()
        BUTTON_B!!.resetButtonState()
        BUTTON_X!!.resetButtonState()
        BUTTON_Y!!.resetButtonState()
        BUTTON_START!!.resetButtonState()
        BUTTON_SELECT!!.resetButtonState()
        TRIGGER_LEFT!!.resetButtonState()
        TRIGGER_RIGHT!!.resetButtonState()
        SHOULDER_LEFT!!.resetButtonState()
        SHOULDER_RIGHT!!.resetButtonState()
        THUMBSTICK_LEFT!!.resetButtonState()
        THUMBSTICK_RIGHT!!.resetButtonState()
        DPAD_UP!!.resetButtonState()
        DPAD_RIGHT!!.resetButtonState()
        DPAD_DOWN!!.resetButtonState()
        DPAD_LEFT!!.resetButtonState()
    }

    /** @return 全てのボタンを含む配列 */
    val buttons: Array<GamepadEmulatedButton?>
        get() = arrayOf(
            BUTTON_A, BUTTON_B, BUTTON_X, BUTTON_Y,
            BUTTON_SELECT, BUTTON_START,
            TRIGGER_LEFT, TRIGGER_RIGHT,
            SHOULDER_LEFT, SHOULDER_RIGHT,
            THUMBSTICK_LEFT, THUMBSTICK_RIGHT,
            DPAD_UP, DPAD_RIGHT, DPAD_DOWN, DPAD_LEFT,
            DIRECTION_FORWARD, DIRECTION_BACKWARD,
            DIRECTION_LEFT, DIRECTION_RIGHT
        )
}