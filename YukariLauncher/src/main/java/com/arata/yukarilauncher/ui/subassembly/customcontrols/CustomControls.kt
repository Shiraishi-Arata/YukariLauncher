package com.arata.yukarilauncher.ui.subassembly.customcontrols

import androidx.annotation.Keep
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlInfoData
import com.arata.yukarilauncher.Tools
import java.io.IOException
import java.util.ArrayList
import java.util.List

/**
 * カスタムコントロールのレイアウト全体を表すデータクラス。
 * ボタン、ドロワー、ジョイスティックの各リストを持ち、JSONとして保存・読み込みが行われます。
 */
@Keep
class CustomControls {
    /** レイアウトファイルのバージョン。 */
    var version = 7
    /** スケール適用時の基準値。 */
    var scaledAt: Float = 0f
    /** コントロールボタンのデータリスト。 */
    var mControlDataList: MutableList<ControlData>? = null
    /** ドロワー（格納型コントロール）のデータリスト。 */
    var mDrawerDataList: MutableList<ControlDrawerData>? = null
    /** ジョイスティックのデータリスト。 */
    var mJoystickDataList: MutableList<ControlJoystickData>? = null
    /** コントロール情報のメタデータ。 */
    var mControlInfoDataList: ControlInfoData? = null

    constructor() : this(ArrayList(), ArrayList(), ArrayList(), ControlInfoData())

    /**
     * @param mControlDataList コントロールボタンデータのリスト
     * @param mDrawerDataList ドロワーデータのリスト
     * @param mJoystickDataList ジョイスティックデータのリスト
     * @param mControlInfoDataList コントロール情報データ
     */
    constructor(mControlDataList: MutableList<ControlData>, mDrawerDataList: MutableList<ControlDrawerData>, mJoystickDataList: MutableList<ControlJoystickData>, mControlInfoDataList: ControlInfoData) {
        this.mControlDataList = mControlDataList
        this.mDrawerDataList = mDrawerDataList
        this.mJoystickDataList = mJoystickDataList
        this.mControlInfoDataList = mControlInfoDataList
        this.scaledAt = 100f
    }

    /**
     * レイアウトデータをJSONファイルとして保存します。
     * @param path 保存先のファイルパス
     * @throws IOException ファイル書き込みエラー
     */
    @Throws(IOException::class)
    fun save(path: String) {
        version = 8
        Tools.write(path, Tools.GLOBAL_GSON.toJson(this))
    }
}