package com.arata.yukarilauncher.ui.subassembly.customcontrols

import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode.GLFW_KEY_UNKNOWN
import android.content.Context
import android.util.ArrayMap
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlInterface
import com.arata.yukarilauncher.utils.JSONUtils
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import org.lwjgl.glfw.CallbackBridge
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.Map

/**
 * コントロールボタンの設定データを保持するクラス。
 * 名前、キーコード、位置、サイズ、色、表示条件など、Minecraftのコントロールボタンの全プロパティを定義します。
 */
@Keep
open class ControlData {
    companion object {
        /** キーボード表示切り替えボタンの特殊キーコード。 */
        const val SPECIALBTN_KEYBOARD = -1
        /** コントロール表示切り替えボタンの特殊キーコード。 */
        const val SPECIALBTN_TOGGLECTRL = -2
        /** マウス左クリックの特殊キーコード。 */
        const val SPECIALBTN_MOUSEPRI = -3
        /** マウス右クリックの特殊キーコード。 */
        const val SPECIALBTN_MOUSESEC = -4
        /** 仮想マウス表示切り替えの特殊キーコード。 */
        const val SPECIALBTN_VIRTUALMOUSE = -5
        /** マウス中クリックの特殊キーコード。 */
        const val SPECIALBTN_MOUSEMID = -6
        /** スクロールアップの特殊キーコード。 */
        const val SPECIALBTN_SCROLLUP = -7
        /** スクロールダウンの特殊キーコード。 */
        const val SPECIALBTN_SCROLLDOWN = -8
        /** メニューボタンの特殊キーコード。 */
        const val SPECIALBTN_MENU = -9

        private var SPECIAL_BUTTONS: Array<ControlData>? = null
        private var SPECIAL_BUTTON_NAME_ARRAY: MutableList<String>? = null
        private var builder = WeakReference<ExpressionBuilder>(null)
        private var conversionMap = WeakReference<ArrayMap<String, String>>(null)

        init {
            buildExpressionBuilder()
            buildConversionMap()
        }

        /**
         * 特殊ボタンのデータ配列を取得します。
         * @param context Androidコンテキスト
         * @return 特殊ボタンのControlData配列
         */
        fun getSpecialButtons(context: Context): Array<ControlData> {
            if (SPECIAL_BUTTONS == null) {
                SPECIAL_BUTTONS = arrayOf(
                    ControlData(context.getString(R.string.keycode_special_keyboard), intArrayOf(SPECIALBTN_KEYBOARD), "\${margin} * 3 + \${width} * 2", "\${margin}", false),
                    ControlData("GUI", intArrayOf(SPECIALBTN_TOGGLECTRL), "\${margin}", "\${bottom} - \${margin}"),
                    ControlData(context.getString(R.string.keycode_special_pri), intArrayOf(SPECIALBTN_MOUSEPRI), "\${margin}", "\${screen_height} - \${margin} * 3 - \${height} * 3"),
                    ControlData(context.getString(R.string.keycode_special_sec), intArrayOf(SPECIALBTN_MOUSESEC), "\${margin} * 3 + \${width} * 2", "\${screen_height} - \${margin} * 3 - \${height} * 3"),
                    ControlData(context.getString(R.string.keycode_special_mouse), intArrayOf(SPECIALBTN_VIRTUALMOUSE), "\${right}", "\${margin}", false),
                    ControlData(context.getString(R.string.keycode_special_mid), intArrayOf(SPECIALBTN_MOUSEMID), "\${margin}", "\${margin}"),
                    ControlData(context.getString(R.string.keycode_special_scrollup), intArrayOf(SPECIALBTN_SCROLLUP), "\${margin}", "\${margin}"),
                    ControlData(context.getString(R.string.keycode_special_scrolldown), intArrayOf(SPECIALBTN_SCROLLDOWN), "\${margin}", "\${margin}"),
                    ControlData(context.getString(R.string.keycode_special_menu), intArrayOf(SPECIALBTN_MENU), "\${margin}", "\${margin}")
                )
            }
            return SPECIAL_BUTTONS!!
        }

        /**
         * 特殊ボタン名のリストを構築します。
         * @param context Androidコンテキスト
         * @return 特殊ボタン名のリスト
         */
        fun buildSpecialButtonArray(context: Context): MutableList<String> {
            if (SPECIAL_BUTTON_NAME_ARRAY == null) {
                val nameList = ArrayList<String>()
                for (btn in getSpecialButtons(context)) {
                    nameList.add(StringUtils.insertSpace(context.getString(R.string.keycode_special), btn.name))
                }
                SPECIAL_BUTTON_NAME_ARRAY = nameList
                Collections.reverse(SPECIAL_BUTTON_NAME_ARRAY)
            }
            return SPECIAL_BUTTON_NAME_ARRAY!!
        }

        /**
         * 数式を計算して結果を返します。
         * @param math 計算する数式文字列
         * @return 計算結果
         */
        private fun calculate(math: String): Float {
            setExpression(math)
            return builder.get()!!.build().evaluate().toFloat()
        }

        /**
         * キーコード配列を4要素に拡張します。
         * @param keycodes 元のキーコード配列
         * @return 4要素に拡張された配列
         */
        private fun inflateKeycodeArray(keycodes: IntArray): IntArray {
            val inflatedArray = intArrayOf(GLFW_KEY_UNKNOWN.toInt(), GLFW_KEY_UNKNOWN.toInt(), GLFW_KEY_UNKNOWN.toInt(), GLFW_KEY_UNKNOWN.toInt())
            System.arraycopy(keycodes, 0, inflatedArray, 0, keycodes.size)
            return inflatedArray
        }

        /** 数式ビルダーを初期化し、dp/px関数を登録します。 */
        private fun buildExpressionBuilder() {
            val expressionBuilder = ExpressionBuilder("1 + 1")
                .function(object : Function("dp", 1) {
                    override fun apply(vararg args: Double): Double {
                        return Tools.pxToDp(args[0].toFloat()).toDouble()
                    }
                })
                .function(object : Function("px", 1) {
                    override fun apply(vararg args: Double): Double {
                        return Tools.dpToPx(args[0].toFloat()).toDouble()
                    }
                })
            builder = WeakReference(expressionBuilder)
        }

        /**
         * 数式ビルダーの式を設定します。
         * @param stringExpression 設定する数式
         */
        private fun setExpression(stringExpression: String) {
            if (builder.get() == null) buildExpressionBuilder()
            builder.get()!!.expression(stringExpression)
        }

        /** 動的位置計算用の変数マップを構築します。 */
        private fun buildConversionMap() {
            val keyValueMap = ArrayMap<String, String>(10)
            keyValueMap["top"] = "0"
            keyValueMap["left"] = "0"
            keyValueMap["right"] = "DUMMY_RIGHT"
            keyValueMap["bottom"] = "DUMMY_BOTTOM"
            keyValueMap["width"] = "DUMMY_WIDTH"
            keyValueMap["height"] = "DUMMY_HEIGHT"
            keyValueMap["screen_width"] = "DUMMY_DATA"
            keyValueMap["screen_height"] = "DUMMY_DATA"
            keyValueMap["margin"] = ControlInterface.getMarginDistance().toInt().toString()
            keyValueMap["preferred_scale"] = "DUMMY_DATA"
            conversionMap = WeakReference(keyValueMap)
        }
    }

    /** ゲーム内で非表示にできるかどうか。 */
    @Transient
    var isHideable: Boolean = false
    /** X座標の動的計算式。 */
    var dynamicX: String? = null
    /** Y座標の動的計算式。 */
    var dynamicY: String? = null
    /** トグル動作（押すごとにON/OFF切り替え）をするかどうか。 */
    var isToggle: Boolean = false
    /** マウスパススルーが有効かどうか。 */
    var passThruEnabled: Boolean = false
    /** ボタンに表示する名前。 */
    var name: String? = null
    /** 割り当てられたキーコード配列。 */
    var keycodes: IntArray = intArrayOf()
    /** ボタンの不透明度。 */
    var opacity: Float = 1f
    /** 背景色。 */
    var bgColor: Int = 0
    /** ストローク（枠線）の色。 */
    var strokeColor: Int = 0
    /** ストローク（枠線）の幅。 */
    var strokeWidth: Float = 0f
    /** 角丸の半径。 */
    var cornerRadius: Float = 0f
    /** スワイプ操作が有効かどうか。 */
    var isSwipeable: Boolean = false
    /** 長押しリピートが有効かどうか。 */
    var repeatedlyEnabled: Boolean = false
    /** リピート時の1秒間あたりの入力回数。 */
    var repeatCps: Int = 10
    /** リピート開始までの長押し遅延時間（ミリ秒）。 */
    var repeatLongPressDelayMs: Int = 300
    /** ゲーム内で表示するかどうか。 */
    var displayInGame: Boolean = true
    /** メニュー画面で表示するかどうか。 */
    var displayInMenu: Boolean = true

    @SerializedName("width")
    private var widthDp: Float = 50f
    @SerializedName("height")
    private var heightDp: Float = 50f

    constructor() : this(ContextExecutor.getString(R.string.controls_add_control_button))

    constructor(name: String) : this(name, intArrayOf())

    constructor(name: String, keycodes: IntArray) : this(name, keycodes, Tools.currentDisplayMetrics.widthPixels / 2f, Tools.currentDisplayMetrics.heightPixels / 2f)

    constructor(name: String, keycodes: IntArray, x: Float, y: Float) : this(name, keycodes, x, y, 50f, 50f)

    constructor(ctx: Context, resId: Int, keycodes: IntArray, x: Float, y: Float, isSquare: Boolean) : this(ctx.resources.getString(resId), keycodes, x, y, isSquare)

    constructor(name: String, keycodes: IntArray, x: Float, y: Float, isSquare: Boolean) : this(name, keycodes, x, y, if (isSquare) 50f else 80f, if (isSquare) 50f else 30f)

    constructor(name: String, keycodes: IntArray, x: Float, y: Float, width: Float, height: Float) : this(name, keycodes, x.toString(), y.toString(), width, height, false)

    constructor(name: String, keycodes: IntArray, dynamicX: String?, dynamicY: String?) : this(name, keycodes, dynamicX, dynamicY, 50f, 50f, false)

    constructor(ctx: Context, resId: Int, keycodes: IntArray, dynamicX: String?, dynamicY: String?, isSquare: Boolean) : this(ctx.resources.getString(resId), keycodes, dynamicX, dynamicY, isSquare)

    constructor(name: String, keycodes: IntArray, dynamicX: String?, dynamicY: String?, isSquare: Boolean) : this(name, keycodes, dynamicX, dynamicY, if (isSquare) 50f else 80f, if (isSquare) 50f else 30f, false)

    constructor(name: String, keycodes: IntArray, dynamicX: String?, dynamicY: String?, width: Float, height: Float, isToggle: Boolean) : this(name, keycodes, dynamicX, dynamicY, width, height, isToggle, 1f, 0x4D000000.toInt(), -0x1, 0f, 0f, true, true, false, false)

    constructor(name: String, keycodes: IntArray, dynamicX: String?, dynamicY: String?, width: Float, height: Float, isToggle: Boolean, opacity: Float, bgColor: Int, strokeColor: Int, strokeWidth: Float, cornerRadius: Float, displayInGame: Boolean, displayInMenu: Boolean, isSwipable: Boolean, mousePassthrough: Boolean) {
        this.name = name
        this.keycodes = inflateKeycodeArray(keycodes)
        this.dynamicX = dynamicX
        this.dynamicY = dynamicY
        this.widthDp = width
        this.heightDp = height
        this.isToggle = isToggle
        this.opacity = opacity
        this.bgColor = bgColor
        this.strokeColor = strokeColor
        this.strokeWidth = strokeWidth
        this.cornerRadius = cornerRadius
        this.displayInGame = displayInGame
        this.displayInMenu = displayInMenu
        this.isSwipeable = isSwipable
        this.passThruEnabled = mousePassthrough
    }

    /** @param controlData コピー元のControlData */
    constructor(controlData: ControlData) : this(
        controlData.name!!,
        controlData.keycodes,
        controlData.dynamicX,
        controlData.dynamicY,
        controlData.widthDp,
        controlData.heightDp,
        controlData.isToggle,
        controlData.opacity,
        controlData.bgColor,
        controlData.strokeColor,
        controlData.strokeWidth,
        controlData.cornerRadius,
        controlData.displayInGame,
        controlData.displayInMenu,
        controlData.isSwipeable,
        controlData.passThruEnabled
    ) {
        this.repeatedlyEnabled = controlData.repeatedlyEnabled
        this.repeatCps = controlData.repeatCps
        this.repeatLongPressDelayMs = controlData.repeatLongPressDelayMs
    }

    /**
     * 動的位置指定の数式を評価し、実際の座標値を計算します。
     * @param dynamicPos 動的位置の数式文字列
     * @return 計算された座標値
     */
    fun insertDynamicPos(dynamicPos: String?): Float {
        val insertedPos = JSONUtils.insertSingleJSONValue(dynamicPos!!, fillConversionMap())
        return calculate(insertedPos)
    }

    /**
     * 指定されたキーコードがこのボタンに割り当てられているかチェックします。
     * @param keycodeToCheck チェックするキーコード
     * @return 割り当てられている場合はtrue
     */
    fun containsKeycode(keycodeToCheck: Int): Boolean {
        for (keycode in keycodes) {
            if (keycodeToCheck == keycode) return true
        }
        return false
    }

    /** @return dpからpxに変換した幅 */
    fun getWidth(): Float = Tools.dpToPx(widthDp)

    /** @param widthInPx 設定する幅（ピクセル） */
    fun setWidth(widthInPx: Float) {
        widthDp = Tools.pxToDp(widthInPx)
    }

    /** @return dpからpxに変換した高さ */
    fun getHeight(): Float = Tools.dpToPx(heightDp)

    /** @param heightInPx 設定する高さ（ピクセル） */
    fun setHeight(heightInPx: Float) {
        heightDp = Tools.pxToDp(heightInPx)
    }

    /**
     * 動的位置計算用の変数マップを実際の値で埋めて返します。
     * @return 画面サイズやボタンサイズで埋められた変数マップ
     */
    private fun fillConversionMap(): kotlin.collections.Map<String, String> {
        var valueMap = conversionMap.get()
        if (valueMap == null) {
            buildConversionMap()
            valueMap = conversionMap.get()
        }
        valueMap!!["right"] = (CallbackBridge.physicalWidth - getWidth()).toString()
        valueMap["bottom"] = (CallbackBridge.physicalHeight - getHeight()).toString()
        valueMap["width"] = getWidth().toString()
        valueMap["height"] = getHeight().toString()
        valueMap["screen_width"] = CallbackBridge.physicalWidth.toString()
        valueMap["screen_height"] = CallbackBridge.physicalHeight.toString()
        valueMap["preferred_scale"] = AllSettings.buttonScale.getValue().toString()
        return valueMap as kotlin.collections.Map<String, String>
    }
}
