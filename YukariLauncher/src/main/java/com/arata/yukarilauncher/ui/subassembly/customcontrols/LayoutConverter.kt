package com.arata.yukarilauncher.ui.subassembly.customcontrols

import android.content.Context
import com.google.gson.JsonSyntaxException
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import com.arata.yukarilauncher.Tools
import org.apache.commons.io.IOUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.lwjgl.glfw.CallbackBridge
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList

/** 旧バージョンのコントロールレイアウトJSONを最新バージョンに変換するユーティリティオブジェクト。 */
object LayoutConverter {
    /**
     * JSONファイルを読み込み、必要に応じて変換を行います。
     * @param ctx Androidコンテキスト
     * @param jsonPath JSONファイルのパス
     * @return 変換されたCustomControls、失敗時はnull
     * @throws IOException ファイル読み込みエラー
     * @throws JsonSyntaxException JSONパースエラー
     */
    @Throws(IOException::class, JsonSyntaxException::class)
    fun loadAndConvertIfNecessary(ctx: Context, jsonPath: String): CustomControls? {
        val jsonLayoutData = Tools.read(jsonPath)
        return try {
            val layoutJobj = JSONObject(jsonLayoutData)
            loadAndConvertIfNecessary(ctx, layoutJobj, jsonLayoutData, jsonPath, true)
        } catch (e: Exception) {
            Tools.showError(ctx, ctx.getString(R.string.controls_load_failed), e)
            null
        }
    }

    /**
     * アセットファイルからレイアウトを読み込みます。
     * @param context Androidコンテキスト
     * @param jsonName アセット内のJSONファイル名
     * @return 変換されたCustomControls、失敗時はnull
     */
    fun loadFromAssets(context: Context, jsonName: String): CustomControls? {
        return try {
            context.assets.open(jsonName).use { `is` ->
                val string = IOUtils.toString(`is`, StandardCharsets.UTF_8)
                val layoutJobj = JSONObject(string)
                loadAndConvertIfNecessary(context, layoutJobj, string, null, true)
            }
        } catch (e: Exception) {
            Tools.showError(context, context.getString(R.string.controls_load_failed), e)
            null
        }
    }

    /**
     * JSONObjectからレイアウトを読み込みます。
     * @param ctx Androidコンテキスト
     * @param layoutJobj レイアウトのJSONオブジェクト
     * @param jsonString 元のJSON文字列
     * @param jsonPath JSONファイルのパス（null可）
     * @param showError エラー表示を行うかどうか
     * @return 変換されたCustomControls、失敗時はnull
     */
    fun loadFromJsonObject(ctx: Context, layoutJobj: JSONObject, jsonString: String, jsonPath: String?, showError: Boolean): CustomControls? {
        return try {
            loadAndConvertIfNecessary(ctx, layoutJobj, jsonString, jsonPath, showError)
        } catch (e: Exception) {
            if (showError) Tools.showError(ctx, ctx.getString(R.string.controls_load_failed), e)
            null
        }
    }

    /**
     * 内部的な変換処理。バージョンに応じて適切な変換メソッドを呼び出します。
     * @param ctx Androidコンテキスト
     * @param layoutJobj レイアウトのJSONオブジェクト
     * @param jsonLayoutData JSON文字列
     * @param jsonPath ファイルパス
     * @param showError エラー表示フラグ
     * @return 変換されたCustomControls
     * @throws Exception 変換エラー
     */
    @Throws(Exception::class)
    private fun loadAndConvertIfNecessary(ctx: Context, layoutJobj: JSONObject, jsonLayoutData: String, jsonPath: String?, showError: Boolean): CustomControls? {
        return if (!layoutJobj.has("version")) {
            val layout = convertV1Layout(layoutJobj)
            if (jsonPath != null) layout.save(jsonPath)
            layout
        } else {
            val version = layoutJobj.getInt("version")
            when (version) {
                2 -> {
                    val layout = convertV2Layout(layoutJobj)
                    if (jsonPath != null) layout.save(jsonPath)
                    layout
                }
                3, 4, 5 -> convertV3_4Layout(layoutJobj)
                6, 7 -> convertV6_7Layout(layoutJobj)
                8 -> Tools.GLOBAL_GSON.fromJson(jsonLayoutData, CustomControls::class.java)
                else -> {
                    if (showError) {
                        val ioException = IOException()
                        Tools.showError(ctx, ctx.getString(R.string.controls_unsupported_layout_version), ioException)
                    }
                    null
                }
            }
        }
    }

    /**
     * バージョン6/7のレイアウトをバージョン8に変換します。
     * @param oldLayoutJson 旧レイアウトのJSONオブジェクト
     * @return 変換されたCustomControls
     */
    fun convertV6_7Layout(oldLayoutJson: JSONObject): CustomControls {
        val layout = Tools.GLOBAL_GSON.fromJson(oldLayoutJson.toString(), CustomControls::class.java)
        for (data in layout.mJoystickDataList!!) {
            if (data.getHeight() > data.getWidth()) {
                val ratio = data.getHeight() / data.getWidth()
                data.dynamicX = data.dynamicX!!.replace("\${height}", "($ratio * \${height})")
                data.dynamicY = data.dynamicY!!.replace("\${height}", "($ratio * \${height})") + " + (" + (ratio - 1) + " * \${height})"
                data.setHeight(data.getWidth())
            }
        }
        layout.version = 8
        return layout
    }

    /**
     * バージョン3/4のレイアウトをバージョン6に変換します。
     * @param oldLayoutJson 旧レイアウトのJSONオブジェクト
     * @return 変換されたCustomControls
     */
    private fun convertV3_4Layout(oldLayoutJson: JSONObject): CustomControls {
        val layout = Tools.GLOBAL_GSON.fromJson(oldLayoutJson.toString(), CustomControls::class.java)
        convertStrokeWidth(layout)
        layout.version = 6
        return layout
    }

    /**
     * バージョン2のレイアウトをバージョン3に変換します。
     * @param oldLayoutJson 旧レイアウトのJSONオブジェクト
     * @return 変換されたCustomControls
     * @throws JSONException JSONパースエラー
     */
    @Throws(JSONException::class)
    private fun convertV2Layout(oldLayoutJson: JSONObject): CustomControls {
        val layout = Tools.GLOBAL_GSON.fromJson(oldLayoutJson.toString(), CustomControls::class.java)
        val layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList")
        layout.mControlDataList = ArrayList(layoutMainArray.length())
        for (i in 0 until layoutMainArray.length()) {
            val button = layoutMainArray.getJSONObject(i)
            val n_button = Tools.GLOBAL_GSON.fromJson(button.toString(), ControlData::class.java)
            if (!Tools.isValidString(n_button.dynamicX) && button.has("x")) {
                val buttonC = button.getDouble("x")
                val ratio = buttonC / CallbackBridge.physicalWidth
                n_button.dynamicX = "$ratio * \${screen_width}"
            }
            if (!Tools.isValidString(n_button.dynamicY) && button.has("y")) {
                val buttonC = button.getDouble("y")
                val ratio = buttonC / CallbackBridge.physicalHeight
                n_button.dynamicY = "$ratio * \${screen_height}"
            }
            layout.mControlDataList!!.add(n_button)
        }
        val layoutDrawerArray = oldLayoutJson.getJSONArray("mDrawerDataList")
        layout.mDrawerDataList = ArrayList()
        for (i in 0 until layoutDrawerArray.length()) {
            val button = layoutDrawerArray.getJSONObject(i)
            val buttonProperties = button.getJSONObject("properties")
            val n_button = Tools.GLOBAL_GSON.fromJson(button.toString(), ControlDrawerData::class.java)
            if (!Tools.isValidString(n_button.properties.dynamicX) && buttonProperties.has("x")) {
                val buttonC = buttonProperties.getDouble("x")
                val ratio = buttonC / CallbackBridge.physicalWidth
                n_button.properties.dynamicX = "$ratio * \${screen_width}"
            }
            if (!Tools.isValidString(n_button.properties.dynamicY) && buttonProperties.has("y")) {
                val buttonC = buttonProperties.getDouble("y")
                val ratio = buttonC / CallbackBridge.physicalHeight
                n_button.properties.dynamicY = "$ratio * \${screen_height}"
            }
            layout.mDrawerDataList!!.add(n_button)
        }
        convertStrokeWidth(layout)
        layout.version = 3
        return layout
    }

    /**
     * バージョン1のレガシーレイアウトをバージョン3に変換します。
     * @param oldLayoutJson 旧レイアウトのJSONオブジェクト
     * @return 変換されたCustomControls
     * @throws JSONException JSONパースエラー
     */
    @Throws(JSONException::class)
    private fun convertV1Layout(oldLayoutJson: JSONObject): CustomControls {
        val empty = CustomControls()
        val layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList")
        for (i in 0 until layoutMainArray.length()) {
            val button = layoutMainArray.getJSONObject(i)
            val n_button = ControlData()
            val keycodes = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(), LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(), LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(), LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt())
            n_button.dynamicX = button.getString("dynamicX")
            n_button.dynamicY = button.getString("dynamicY")
            if (!Tools.isValidString(n_button.dynamicX) && button.has("x")) {
                val buttonC = button.getDouble("x")
                val ratio = buttonC / CallbackBridge.physicalWidth
                n_button.dynamicX = "$ratio * \${screen_width}"
            }
            if (!Tools.isValidString(n_button.dynamicY) && button.has("y")) {
                val buttonC = button.getDouble("y")
                val ratio = buttonC / CallbackBridge.physicalHeight
                n_button.dynamicY = "$ratio * \${screen_height}"
            }
            n_button.name = button.getString("name")
            n_button.opacity = ((button.getInt("transparency") - 100) * -1) / 100f
            n_button.passThruEnabled = button.getBoolean("passThruEnabled")
            n_button.isToggle = button.getBoolean("isToggle")
            n_button.setHeight(button.getInt("height").toFloat())
            n_button.setWidth(button.getInt("width").toFloat())
            n_button.bgColor = 0x4d000000.toInt()
            n_button.strokeWidth = 0f
            if (button.getBoolean("isRound")) {
                n_button.cornerRadius = 35f
            }
            var next_idx = 0
            if (button.getBoolean("holdShift")) {
                keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toInt()
                next_idx++
            }
            if (button.getBoolean("holdCtrl")) {
                keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL.toInt()
                next_idx++
            }
            if (button.getBoolean("holdAlt")) {
                keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT.toInt()
                next_idx++
            }
            keycodes[next_idx] = button.getInt("keycode")
            n_button.keycodes = keycodes
            empty.mControlDataList!!.add(n_button)
        }
        empty.scaledAt = oldLayoutJson.getDouble("scaledAt").toFloat()
        empty.version = 3
        return empty
    }

    /**
     * ストローク幅の値をパーセントからdp単位に変換します。
     * @param layout 変換対象のCustomControls
     */
    private fun convertStrokeWidth(layout: CustomControls) {
        for (data in layout.mControlDataList!!) {
            data.strokeWidth = Tools.pxToDp(computeStrokeWidth(data.strokeWidth, data.getWidth(), data.getHeight()).toFloat())
        }
        for (data in layout.mDrawerDataList!!) {
            data.properties.strokeWidth = Tools.pxToDp(computeStrokeWidth(data.properties.strokeWidth, data.properties.getWidth(), data.properties.getHeight()).toFloat())
            for (subButtonData in data.buttonProperties) {
                subButtonData.strokeWidth = Tools.pxToDp(computeStrokeWidth(subButtonData.strokeWidth, data.properties.getWidth(), data.properties.getWidth()).toFloat())
            }
        }
    }

    /**
     * ストローク幅のピクセル値を計算します。
     * @param widthInPercent パーセントで指定された幅
     * @param width ボタンの幅
     * @param height ボタンの高さ
     * @return 計算されたピクセル値
     */
    fun computeStrokeWidth(widthInPercent: Float, width: Float, height: Float): Int {
        val maxSize = Math.max(width, height)
        return ((maxSize / 2) * (widthInPercent / 100)).toInt()
    }
}