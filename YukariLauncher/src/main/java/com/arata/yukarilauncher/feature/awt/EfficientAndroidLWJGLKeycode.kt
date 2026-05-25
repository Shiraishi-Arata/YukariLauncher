package com.arata.yukarilauncher.feature.awt

import android.view.KeyEvent
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.sendKeyPress
import java.util.Arrays
import java.util.HashMap

/**
 * AndroidキーコードとLWJGL GLFWキーコードの間の効率的なマッピングを提供するオブジェクト。
 * 106個のキーコードマッピングを事前に初期化し、高速な変換を実現します。
 * AndroidのKeyEventからLWJGLのキーコードへの変換と、その逆の変換をサポートします。
 */
object EfficientAndroidLWJGLKeycode {
    /** マッピング可能なキーコードの総数。 */
    private const val KEYCODE_COUNT = 106
    /** Androidキーコードを格納する配列。 */
    private val sAndroidKeycodes = IntArray(KEYCODE_COUNT)
    /** LWJGLキーコードを格納する配列。 */
    private val sLwjglKeycodes = ShortArray(KEYCODE_COUNT)
    /** キーコードの表示名を格納するマップ。 */
    private val sKeycodesName: MutableMap<Int, String> = HashMap()
    /** キー名の配列（遅延初期化）。 */
    private var androidKeyNameArray: Array<String?>? = null
    /** マッピング追加用の一時カウンター。 */
    private var mTmpCount = 0

    init {
        add(KeyEvent.KEYCODE_UNKNOWN, LwjglGlfwKeycode.GLFW_KEY_UNKNOWN, ContextExecutor.getString(R.string.keycode_unspecified))
        add(KeyEvent.KEYCODE_HOME, LwjglGlfwKeycode.GLFW_KEY_HOME, "Home")
        add(KeyEvent.KEYCODE_BACK, LwjglGlfwKeycode.GLFW_KEY_ESCAPE, "Back (Esc)")

        add(KeyEvent.KEYCODE_0, LwjglGlfwKeycode.GLFW_KEY_0, "0")
        add(KeyEvent.KEYCODE_1, LwjglGlfwKeycode.GLFW_KEY_1, "1")
        add(KeyEvent.KEYCODE_2, LwjglGlfwKeycode.GLFW_KEY_2, "2")
        add(KeyEvent.KEYCODE_3, LwjglGlfwKeycode.GLFW_KEY_3, "3")
        add(KeyEvent.KEYCODE_4, LwjglGlfwKeycode.GLFW_KEY_4, "4")
        add(KeyEvent.KEYCODE_5, LwjglGlfwKeycode.GLFW_KEY_5, "5")
        add(KeyEvent.KEYCODE_6, LwjglGlfwKeycode.GLFW_KEY_6, "6")
        add(KeyEvent.KEYCODE_7, LwjglGlfwKeycode.GLFW_KEY_7, "7")
        add(KeyEvent.KEYCODE_8, LwjglGlfwKeycode.GLFW_KEY_8, "8")
        add(KeyEvent.KEYCODE_9, LwjglGlfwKeycode.GLFW_KEY_9, "9")

        add(KeyEvent.KEYCODE_POUND, LwjglGlfwKeycode.GLFW_KEY_3, "# (3)")

        add(KeyEvent.KEYCODE_DPAD_UP, LwjglGlfwKeycode.GLFW_KEY_UP, "\u2191")
        add(KeyEvent.KEYCODE_DPAD_DOWN, LwjglGlfwKeycode.GLFW_KEY_DOWN, "\u2193")
        add(KeyEvent.KEYCODE_DPAD_LEFT, LwjglGlfwKeycode.GLFW_KEY_LEFT, "\u2190")
        add(KeyEvent.KEYCODE_DPAD_RIGHT, LwjglGlfwKeycode.GLFW_KEY_RIGHT, "\u2192")

        add(KeyEvent.KEYCODE_A, LwjglGlfwKeycode.GLFW_KEY_A, "A")
        add(KeyEvent.KEYCODE_B, LwjglGlfwKeycode.GLFW_KEY_B, "B")
        add(KeyEvent.KEYCODE_C, LwjglGlfwKeycode.GLFW_KEY_C, "C")
        add(KeyEvent.KEYCODE_D, LwjglGlfwKeycode.GLFW_KEY_D, "D")
        add(KeyEvent.KEYCODE_E, LwjglGlfwKeycode.GLFW_KEY_E, "E")
        add(KeyEvent.KEYCODE_F, LwjglGlfwKeycode.GLFW_KEY_F, "F")
        add(KeyEvent.KEYCODE_G, LwjglGlfwKeycode.GLFW_KEY_G, "G")
        add(KeyEvent.KEYCODE_H, LwjglGlfwKeycode.GLFW_KEY_H, "H")
        add(KeyEvent.KEYCODE_I, LwjglGlfwKeycode.GLFW_KEY_I, "I")
        add(KeyEvent.KEYCODE_J, LwjglGlfwKeycode.GLFW_KEY_J, "J")
        add(KeyEvent.KEYCODE_K, LwjglGlfwKeycode.GLFW_KEY_K, "K")
        add(KeyEvent.KEYCODE_L, LwjglGlfwKeycode.GLFW_KEY_L, "L")
        add(KeyEvent.KEYCODE_M, LwjglGlfwKeycode.GLFW_KEY_M, "M")
        add(KeyEvent.KEYCODE_N, LwjglGlfwKeycode.GLFW_KEY_N, "N")
        add(KeyEvent.KEYCODE_O, LwjglGlfwKeycode.GLFW_KEY_O, "O")
        add(KeyEvent.KEYCODE_P, LwjglGlfwKeycode.GLFW_KEY_P, "P")
        add(KeyEvent.KEYCODE_Q, LwjglGlfwKeycode.GLFW_KEY_Q, "Q")
        add(KeyEvent.KEYCODE_R, LwjglGlfwKeycode.GLFW_KEY_R, "R")
        add(KeyEvent.KEYCODE_S, LwjglGlfwKeycode.GLFW_KEY_S, "S")
        add(KeyEvent.KEYCODE_T, LwjglGlfwKeycode.GLFW_KEY_T, "T")
        add(KeyEvent.KEYCODE_U, LwjglGlfwKeycode.GLFW_KEY_U, "U")
        add(KeyEvent.KEYCODE_V, LwjglGlfwKeycode.GLFW_KEY_V, "V")
        add(KeyEvent.KEYCODE_W, LwjglGlfwKeycode.GLFW_KEY_W, "W")
        add(KeyEvent.KEYCODE_X, LwjglGlfwKeycode.GLFW_KEY_X, "X")
        add(KeyEvent.KEYCODE_Y, LwjglGlfwKeycode.GLFW_KEY_Y, "Y")
        add(KeyEvent.KEYCODE_Z, LwjglGlfwKeycode.GLFW_KEY_Z, "Z")

        add(KeyEvent.KEYCODE_COMMA, LwjglGlfwKeycode.GLFW_KEY_COMMA, ",")
        add(KeyEvent.KEYCODE_PERIOD, LwjglGlfwKeycode.GLFW_KEY_PERIOD, ".")

        add(KeyEvent.KEYCODE_ALT_LEFT, LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT, ContextExecutor.getString(R.string.keycode_left_alt))
        add(KeyEvent.KEYCODE_ALT_RIGHT, LwjglGlfwKeycode.GLFW_KEY_RIGHT_ALT, ContextExecutor.getString(R.string.keycode_right_alt))

        add(KeyEvent.KEYCODE_SHIFT_LEFT, LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, ContextExecutor.getString(R.string.keycode_left_shift))
        add(KeyEvent.KEYCODE_SHIFT_RIGHT, LwjglGlfwKeycode.GLFW_KEY_RIGHT_SHIFT, ContextExecutor.getString(R.string.keycode_right_shift))

        add(KeyEvent.KEYCODE_TAB, LwjglGlfwKeycode.GLFW_KEY_TAB, "Tab \u21C4")
        add(KeyEvent.KEYCODE_SPACE, LwjglGlfwKeycode.GLFW_KEY_SPACE, ContextExecutor.getString(R.string.keycode_space))
        add(KeyEvent.KEYCODE_ENTER, LwjglGlfwKeycode.GLFW_KEY_ENTER, "Enter \u21B2")
        add(KeyEvent.KEYCODE_DEL, LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, "\u2190 Backspace")
        add(KeyEvent.KEYCODE_GRAVE, LwjglGlfwKeycode.GLFW_KEY_GRAVE_ACCENT, "`")
        add(KeyEvent.KEYCODE_MINUS, LwjglGlfwKeycode.GLFW_KEY_MINUS, "-")
        add(KeyEvent.KEYCODE_EQUALS, LwjglGlfwKeycode.GLFW_KEY_EQUAL, "=")
        add(KeyEvent.KEYCODE_LEFT_BRACKET, LwjglGlfwKeycode.GLFW_KEY_LEFT_BRACKET, "[")
        add(KeyEvent.KEYCODE_RIGHT_BRACKET, LwjglGlfwKeycode.GLFW_KEY_RIGHT_BRACKET, "]")
        add(KeyEvent.KEYCODE_BACKSLASH, LwjglGlfwKeycode.GLFW_KEY_BACKSLASH, "\\")
        add(KeyEvent.KEYCODE_SEMICOLON, LwjglGlfwKeycode.GLFW_KEY_SEMICOLON, ";")
        add(KeyEvent.KEYCODE_APOSTROPHE, LwjglGlfwKeycode.GLFW_KEY_APOSTROPHE, "'")
        add(KeyEvent.KEYCODE_SLASH, LwjglGlfwKeycode.GLFW_KEY_SLASH, "/")
        add(KeyEvent.KEYCODE_AT, LwjglGlfwKeycode.GLFW_KEY_2, "@ (2)")

        add(KeyEvent.KEYCODE_PLUS, LwjglGlfwKeycode.GLFW_KEY_KP_ADD, ContextExecutor.getString(R.string.keycode_kp_plus))

        add(KeyEvent.KEYCODE_PAGE_UP, LwjglGlfwKeycode.GLFW_KEY_PAGE_UP, "PGUp +")
        add(KeyEvent.KEYCODE_PAGE_DOWN, LwjglGlfwKeycode.GLFW_KEY_PAGE_DOWN, "PGDn -")

        add(KeyEvent.KEYCODE_ESCAPE, LwjglGlfwKeycode.GLFW_KEY_ESCAPE, "Esc")

        add(KeyEvent.KEYCODE_CTRL_LEFT, LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL, ContextExecutor.getString(R.string.keycode_left_ctrl))
        add(KeyEvent.KEYCODE_CTRL_RIGHT, LwjglGlfwKeycode.GLFW_KEY_RIGHT_CONTROL, ContextExecutor.getString(R.string.keycode_right_ctrl))

        add(KeyEvent.KEYCODE_CAPS_LOCK, LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK, "Caps Lock")
        add(KeyEvent.KEYCODE_BREAK, LwjglGlfwKeycode.GLFW_KEY_PAUSE, "Pause/Break")
        add(KeyEvent.KEYCODE_MOVE_HOME, LwjglGlfwKeycode.GLFW_KEY_HOME, "Home")
        add(KeyEvent.KEYCODE_MOVE_END, LwjglGlfwKeycode.GLFW_KEY_END, "End")
        add(KeyEvent.KEYCODE_INSERT, LwjglGlfwKeycode.GLFW_KEY_INSERT, "Insert")

        add(KeyEvent.KEYCODE_F1, LwjglGlfwKeycode.GLFW_KEY_F1, "F1")
        add(KeyEvent.KEYCODE_F2, LwjglGlfwKeycode.GLFW_KEY_F2, "F2")
        add(KeyEvent.KEYCODE_F3, LwjglGlfwKeycode.GLFW_KEY_F3, "F3")
        add(KeyEvent.KEYCODE_F4, LwjglGlfwKeycode.GLFW_KEY_F4, "F4")
        add(KeyEvent.KEYCODE_F5, LwjglGlfwKeycode.GLFW_KEY_F5, "F5")
        add(KeyEvent.KEYCODE_F6, LwjglGlfwKeycode.GLFW_KEY_F6, "F6")
        add(KeyEvent.KEYCODE_F7, LwjglGlfwKeycode.GLFW_KEY_F7, "F7")
        add(KeyEvent.KEYCODE_F8, LwjglGlfwKeycode.GLFW_KEY_F8, "F8")
        add(KeyEvent.KEYCODE_F9, LwjglGlfwKeycode.GLFW_KEY_F9, "F9")
        add(KeyEvent.KEYCODE_F10, LwjglGlfwKeycode.GLFW_KEY_F10, "F10")
        add(KeyEvent.KEYCODE_F11, LwjglGlfwKeycode.GLFW_KEY_F11, "F11")
        add(KeyEvent.KEYCODE_F12, LwjglGlfwKeycode.GLFW_KEY_F12, "F12")

        add(KeyEvent.KEYCODE_NUM_LOCK, LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK, "Num Lock")
        add(KeyEvent.KEYCODE_NUMPAD_0, LwjglGlfwKeycode.GLFW_KEY_KP_0, ContextExecutor.getString(R.string.keycode_kp_0))
        add(KeyEvent.KEYCODE_NUMPAD_1, LwjglGlfwKeycode.GLFW_KEY_KP_1, ContextExecutor.getString(R.string.keycode_kp_1))
        add(KeyEvent.KEYCODE_NUMPAD_2, LwjglGlfwKeycode.GLFW_KEY_KP_2, ContextExecutor.getString(R.string.keycode_kp_2))
        add(KeyEvent.KEYCODE_NUMPAD_3, LwjglGlfwKeycode.GLFW_KEY_KP_3, ContextExecutor.getString(R.string.keycode_kp_3))
        add(KeyEvent.KEYCODE_NUMPAD_4, LwjglGlfwKeycode.GLFW_KEY_KP_4, ContextExecutor.getString(R.string.keycode_kp_4))
        add(KeyEvent.KEYCODE_NUMPAD_5, LwjglGlfwKeycode.GLFW_KEY_KP_5, ContextExecutor.getString(R.string.keycode_kp_5))
        add(KeyEvent.KEYCODE_NUMPAD_6, LwjglGlfwKeycode.GLFW_KEY_KP_6, ContextExecutor.getString(R.string.keycode_kp_6))
        add(KeyEvent.KEYCODE_NUMPAD_7, LwjglGlfwKeycode.GLFW_KEY_KP_7, ContextExecutor.getString(R.string.keycode_kp_7))
        add(KeyEvent.KEYCODE_NUMPAD_8, LwjglGlfwKeycode.GLFW_KEY_KP_8, ContextExecutor.getString(R.string.keycode_kp_8))
        add(KeyEvent.KEYCODE_NUMPAD_9, LwjglGlfwKeycode.GLFW_KEY_KP_9, ContextExecutor.getString(R.string.keycode_kp_9))
        add(KeyEvent.KEYCODE_NUMPAD_DIVIDE, LwjglGlfwKeycode.GLFW_KEY_KP_DIVIDE, ContextExecutor.getString(R.string.keycode_kp_divide))
        add(KeyEvent.KEYCODE_NUMPAD_MULTIPLY, LwjglGlfwKeycode.GLFW_KEY_KP_MULTIPLY, ContextExecutor.getString(R.string.keycode_kp_multiply))
        add(KeyEvent.KEYCODE_NUMPAD_SUBTRACT, LwjglGlfwKeycode.GLFW_KEY_KP_SUBTRACT, ContextExecutor.getString(R.string.keycode_kp_subtract))
        add(KeyEvent.KEYCODE_NUMPAD_ADD, LwjglGlfwKeycode.GLFW_KEY_KP_ADD, ContextExecutor.getString(R.string.keycode_kp_plus))
        add(KeyEvent.KEYCODE_NUMPAD_DOT, LwjglGlfwKeycode.GLFW_KEY_KP_DECIMAL, ContextExecutor.getString(R.string.keycode_kp_decimal))
        add(KeyEvent.KEYCODE_NUMPAD_COMMA, LwjglGlfwKeycode.GLFW_KEY_COMMA, ContextExecutor.getString(R.string.keycode_kp_comma))
        add(KeyEvent.KEYCODE_NUMPAD_ENTER, LwjglGlfwKeycode.GLFW_KEY_KP_ENTER, ContextExecutor.getString(R.string.keycode_kp_enter))
        add(KeyEvent.KEYCODE_NUMPAD_EQUALS, LwjglGlfwKeycode.GLFW_KEY_EQUAL, ContextExecutor.getString(R.string.keycode_kp_equal))
    }

    /**
     * 指定されたインデックスが有効かどうかを判定します。
     * @param index 判定するインデックス
     * @return インデックスが0以上の場合はtrue
     */
    fun containsIndex(index: Int): Boolean = index >= 0

    /**
     * すべてのキーの表示名を生成します。
     * @return キー名の配列
     */
    fun generateKeyName(): Array<String?> {
        if (androidKeyNameArray == null) {
            androidKeyNameArray = arrayOfNulls(sAndroidKeycodes.size)
            for (i in androidKeyNameArray!!.indices) {
                androidKeyNameArray!![i] = sKeycodesName[i]
            }
        }
        return androidKeyNameArray!!
    }

    /**
     * AndroidのKeyEventを処理し、対応するLWJGLキー入力を実行します。
     * @param keyEvent 処理するAndroidキーイベント
     * @param valueIndex マッピングテーブルのインデックス
     */
    fun execKey(keyEvent: KeyEvent, valueIndex: Int) {
        CallbackBridge.holdingAlt = keyEvent.isAltPressed
        CallbackBridge.holdingCapslock = keyEvent.isCapsLockOn
        CallbackBridge.holdingCtrl = keyEvent.isCtrlPressed
        CallbackBridge.holdingNumlock = keyEvent.isNumLockOn
        CallbackBridge.holdingShift = keyEvent.isShiftPressed

        println(keyEvent.keyCode.toString() + " " + keyEvent.displayLabel)
        val key = if (keyEvent.unicodeChar != 0) keyEvent.unicodeChar.toChar() else '\u0000'
        sendKeyPress(
            getValueByIndex(valueIndex).toInt(),
            key,
            0,
            CallbackBridge.getCurrentMods(),
            keyEvent.action == KeyEvent.ACTION_DOWN
        )
    }

    /**
     * 指定されたインデックスのキー入力を実行します。
     * @param index 実行するキーのマッピングインデックス
     */
    fun execKeyIndex(index: Int) {
        sendKeyPress(getValueByIndex(index).toInt())
    }

    /**
     * インデックスに対応するLWJGLキーコードを取得します。
     * @param index マッピングテーブルのインデックス
     * @return LWJGLキーコード
     */
    fun getValueByIndex(index: Int): Short = sLwjglKeycodes[index]

    /**
     * Androidキーコードに対応するマッピングインデックスを二分探索で検索します。
     * @param key Androidキーコード
     * @return マッピングテーブルのインデックス
     */
    fun getIndexByKey(key: Int): Int = Arrays.binarySearch(sAndroidKeycodes, key)

    /**
     * LWJGLキーコードに対応するマッピングインデックスを線形探索で検索します。
     * @param lwjglKey LWJGLキーコード
     * @return マッピングテーブルのインデックス
     */
    fun getIndexByValue(lwjglKey: Int): Int {
        for (i in sLwjglKeycodes.indices) {
            if (sLwjglKeycodes[i].toInt() == lwjglKey) return i
        }
        return 0
    }

    /**
     * AndroidキーコードとLWJGLキーコードのマッピングを追加します。
     * @param androidKeycode Androidのキーコード
     * @param LWJGLKeycode 対応するLWJGLキーコード
     * @param name キーの表示名
     */
    private fun add(androidKeycode: Int, LWJGLKeycode: Short, name: String) {
        sAndroidKeycodes[mTmpCount] = androidKeycode
        sLwjglKeycodes[mTmpCount] = LWJGLKeycode
        sKeycodesName[mTmpCount] = name
        mTmpCount++
    }
}
