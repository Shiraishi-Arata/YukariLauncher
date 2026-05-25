package com.arata.yukarilauncher.ui.subassembly.customcontrols

import android.content.Context
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.Tools
import java.util.ArrayList

/**
 * コントロールドロワーの設定データを保持するクラス。
 * ドロワーは折りたたみ可能なボタンコンテナで、複数のサブボタンを格納できます。
 */
@androidx.annotation.Keep
class ControlDrawerData {
    /** ドロワーの開閉方向を表す列挙型。 */
    @androidx.annotation.Keep
    enum class Orientation {
        /** 下方向に開く */
        DOWN,
        /** 左方向に開く */
        LEFT,
        /** 上方向に開く */
        UP,
        /** 右方向に開く */
        RIGHT,
        /** 自由位置に配置 */
        FREE
    }

    /** ドロワー内のサブボタンデータリスト。 */
    val buttonProperties: ArrayList<ControlData>
    /** ドロワー自体のプロパティ。 */
    val properties: ControlData
    /** ドロワーの開閉方向。 */
    @androidx.annotation.Keep
    var orientation: Orientation = Orientation.LEFT

    constructor() : this(ArrayList())

    constructor(buttonProperties: ArrayList<ControlData>) : this(buttonProperties, ControlData(ContextExecutor.getString(R.string.controls_add_control_drawer), intArrayOf(), Tools.currentDisplayMetrics.widthPixels / 2f, Tools.currentDisplayMetrics.heightPixels / 2f))

    constructor(buttonProperties: ArrayList<ControlData>, properties: ControlData) : this(buttonProperties, properties, Orientation.LEFT)

    constructor(buttonProperties: ArrayList<ControlData>, properties: ControlData, orientation: Orientation) {
        this.buttonProperties = buttonProperties
        this.properties = properties
        this.orientation = orientation
    }

    /** @param drawerData コピー元のControlDrawerData */
    constructor(drawerData: ControlDrawerData) {
        buttonProperties = ArrayList(drawerData.buttonProperties.size)
        for (controlData in drawerData.buttonProperties) {
            buttonProperties.add(ControlData(controlData))
        }
        properties = ControlData(drawerData.properties)
        orientation = drawerData.orientation
    }

    companion object {
        /**
         * ローカライズされた方向名の配列を返します。
         * @param context Androidコンテキスト
         * @return 方向名の文字列配列
         */
        fun getOrientations(context: Context): Array<String> {
            return arrayOf(
                context.getString(R.string.controls_orientation_down),
                context.getString(R.string.controls_orientation_left),
                context.getString(R.string.controls_orientation_up),
                context.getString(R.string.controls_orientation_right),
                context.getString(R.string.controls_orientation_free)
            )
        }

        /**
         * Orientation列挙型を整数値に変換します。
         * @param orientation 変換する方向
         * @return 整数値 (0=DOWN, 1=LEFT, 2=UP, 3=RIGHT, 4=FREE)
         */
        fun orientationToInt(orientation: Orientation): Int {
            return when (orientation) {
                Orientation.DOWN -> 0
                Orientation.LEFT -> 1
                Orientation.UP -> 2
                Orientation.RIGHT -> 3
                Orientation.FREE -> 4
            }
        }

        /**
         * 整数値をOrientation列挙型に変換します。
         * @param by 変換する整数値
         * @return 対応するOrientation、該当なしの場合はnull
         */
        fun intToOrientation(by: Int): Orientation? {
            return when (by) {
                0 -> Orientation.DOWN
                1 -> Orientation.LEFT
                2 -> Orientation.UP
                3 -> Orientation.RIGHT
                4 -> Orientation.FREE
                else -> null
            }
        }
    }
}
