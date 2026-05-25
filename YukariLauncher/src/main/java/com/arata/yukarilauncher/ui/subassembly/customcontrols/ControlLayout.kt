package com.arata.yukarilauncher.ui.subassembly.customcontrols

import android.content.Context.INPUT_METHOD_SERVICE
import com.arata.yukarilauncher.Tools.currentDisplayMetrics
import org.lwjgl.glfw.CallbackBridge.isGrabbing
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import com.google.gson.JsonSyntaxException
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.EditControlInfoDialog
import com.arata.yukarilauncher.ui.dialog.SelectControlsDialog
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlInfoData
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.stringutils.StringUtilsKt
import com.arata.yukarilauncher.MinecraftGLSurface
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlButton
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlDrawer
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlInterface
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlJoystick
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlSubButton
import com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview.ActionRow
import com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview.ControlHandleView
import com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview.EditControlPopup
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.List

/**
 * カスタムコントロールのレイアウトを管理するFrameLayout。
 * ボタン、ドロワー、ジョイスティックの追加/削除、レイアウトの保存/読み込み、編集機能を提供します。
 */
class ControlLayout : FrameLayout {
    protected var mLayout: CustomControls? = null
    private var mInfoData: ControlInfoData? = null
    private var mGameSurface: MinecraftGLSurface? = null
    private var mButtons: MutableList<ControlInterface>? = null
    private var mModifiable = false
    private var mIsModified = false
    private var mControlVisible = false
    private var mControlPopup: EditControlPopup? = null
    private var mHandleView: ControlHandleView? = null
    private var mMenuListener: ControlButtonMenuListener? = null
    var mActionRow: ActionRow? = null
    var mLayoutFileName: String? = null

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    /**
     * JSONファイルからレイアウトを読み込みます。
     * @param jsonPath JSONファイルのパス、nullの場合はデフォルトレイアウト
     */
    fun loadLayout(jsonPath: String?) {
        val jsonFile = if (jsonPath != null) File(jsonPath) else File(AllSettings.defaultCtrl.getValue())

        val layout: CustomControls?
        if (jsonFile.exists()) {
            layout = LayoutConverter.loadAndConvertIfNecessary(context, jsonFile.absolutePath)
        } else {
            layout = LayoutConverter.loadFromAssets(context, "yukari.json")
        }
        if (layout != null) {
            loadLayout(layout)
            mLayoutFileName = if (jsonFile.exists()) {
                StringUtilsKt.removeSuffix(jsonFile.name, ".json")
            } else {
                "default"
            }
        }
    }

    /**
     * CustomControlsオブジェクトからレイアウトを読み込みます。
     * @param controlLayout 読み込むレイアウトデータ
     */
    fun loadLayout(controlLayout: CustomControls?) {
        var sanitizedModified = false
        if (controlLayout != null) {
            sanitizedModified = LayoutSanitizer.sanitizeLayout(controlLayout)
        }
        mInfoData = controlLayout?.mControlInfoDataList
        if (mInfoData == null) {
            mInfoData = ControlInfoData()
        }

        if (mActionRow == null) {
            mActionRow = ActionRow(context)
            addView(mActionRow)
        }

        removeAllButtons()
        if (mLayout != null) {
            mLayout!!.mControlDataList = null
            mLayout = null
        }

        System.gc()
        mapTable.clear()

        if (controlLayout == null) return

        mLayout = controlLayout

        for (joystick in mLayout!!.mJoystickDataList!!) {
            addJoystickView(joystick)
        }

        for (button in controlLayout.mControlDataList!!) {
            addControlView(button)
        }

        for (drawerData in controlLayout.mDrawerDataList!!) {
            val drawer = addDrawerView(drawerData)
            if (mModifiable) drawer.areButtonsVisible = true
        }

        mLayout!!.scaledAt = AllSettings.buttonScale.getValue().toFloat()

        setModified(sanitizedModified)
        mButtons = null
        getButtonChildren()
    }

    /**
     * コントロールボタンを追加します。
     * @param controlButton 追加するボタンデータ
     */
    fun addControlButton(controlButton: ControlData) {
        mLayout!!.mControlDataList!!.add(controlButton)
        addControlView(controlButton)
    }

    /**
     * コントロールボタンのビューを作成して追加します。
     * @param controlButton 追加するボタンデータ
     */
    private fun addControlView(controlButton: ControlData) {
        val view = ControlButton(this, controlButton)
        if (!mModifiable) {
            view.alpha = view.properties.opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
        setModified(true)
    }

    /**
     * ドロワーを追加します。
     * @param drawerData 追加するドロワーデータ
     */
    fun addDrawer(drawerData: ControlDrawerData) {
        mLayout!!.mDrawerDataList!!.add(drawerData)
        addDrawerView()
    }

    /**
     * 最新のドロワーデータからビューを作成して追加します。
     * @return 作成されたControlDrawer
     */
    private fun addDrawerView(): ControlDrawer {
        return addDrawerView(null)
    }

    /**
     * 指定されたドロワーデータからビューを作成して追加します。
     * @param drawerData 追加するドロワーデータ（nullの場合はリストの最後のデータを使用）
     * @return 作成されたControlDrawer
     */
    private fun addDrawerView(drawerData: ControlDrawerData?): ControlDrawer {
        val view = ControlDrawer(this, drawerData ?: mLayout!!.mDrawerDataList!![mLayout!!.mDrawerDataList!!.size - 1])
        if (!mModifiable) {
            view.alpha = view.properties.opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
        for (subButton in view.drawerData.buttonProperties) {
            addSubView(view, subButton)
        }
        setModified(true)
        return view
    }

    /**
     * ドロワーにサブボタンを追加します。
     * @param drawer 追加先のドロワー
     * @param controlButton 追加するサブボタンデータ
     */
    fun addSubButton(drawer: ControlDrawer, controlButton: ControlData) {
        drawer.drawerData.buttonProperties.add(controlButton)
        addSubView(drawer, drawer.drawerData.buttonProperties[drawer.drawerData.buttonProperties.size - 1])
    }

    /**
     * サブボタンのビューを作成してドロワーに追加します。
     * @param drawer 追加先のドロワー
     * @param controlButton 追加するサブボタンデータ
     */
    private fun addSubView(drawer: ControlDrawer, controlButton: ControlData) {
        val view = ControlSubButton(this, controlButton, drawer)
        if (!mModifiable) {
            view.alpha = view.properties.opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        } else {
            view.setVisible(true)
        }
        addView(view)
        drawer.addButton(view)
        setModified(true)
    }

    /**
     * ジョイスティックを追加します。
     * @param data 追加するジョイスティックデータ
     */
    fun addJoystickButton(data: ControlJoystickData) {
        mLayout!!.mJoystickDataList!!.add(data)
        addJoystickView(data)
    }

    /**
     * ジョイスティックのビューを作成して追加します。
     * @param data 追加するジョイスティックデータ
     */
    private fun addJoystickView(data: ControlJoystickData) {
        val view = ControlJoystick(this, data)
        if (!mModifiable) {
            view.alpha = view.properties.opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
    }

    /** 全てのボタンをレイアウトから削除します。 */
    private fun removeAllButtons() {
        for (button in getButtonChildren()) {
            removeView(button.controlView)
        }
        System.gc()
    }

    /**
     * レイアウトを指定されたパスに保存します。
     * @param path 保存先のファイルパス
     * @throws Exception 保存エラー
     */
    @Throws(Exception::class)
    fun saveLayout(path: String) {
        mLayout!!.save(path)
        setModified(false)
    }

    /** コントロールの表示/非表示を切り替えます。 */
    fun toggleControlVisible() {
        mControlVisible = !mControlVisible
        setControlVisible(mControlVisible)
    }

    /** @return 現在のレイアウトのスケール値 */
    val layoutScale: Float
        get() = mLayout!!.scaledAt

    /** @return 現在のCustomControlsオブジェクト */
    val layout: CustomControls?
        get() = mLayout

    /**
     * コントロールの表示状態を設定します。
     * @param isVisible 表示する場合はtrue
     */
    fun setControlVisible(isVisible: Boolean) {
        if (mModifiable) return
        mControlVisible = isVisible
        for (button in getButtonChildren()) {
            button.setVisible(((button.properties.displayInGame && isGrabbing()) || (button.properties.displayInMenu && !isGrabbing())) && isVisible)
        }
    }

    /**
     * 編集可能モードを設定します。
     * @param isModifiable 編集可能にする場合はtrue
     */
    fun setModifiable(isModifiable: Boolean) {
        if (!isModifiable && mModifiable) {
            removeEditWindow()
        }
        mModifiable = isModifiable
        if (isModifiable) {
            for (button in getButtonChildren()) {
                button.setVisible(true)
            }
        }
    }

    /** @return 編集可能モードかどうか */
    val isModifiable: Boolean
        get() = mModifiable

    /** @param isModified 変更状態を設定 */
    fun setModified(isModified: Boolean) {
        mIsModified = isModified
    }

    /**
     * 全ての子ボタンを取得します。
     * @return ControlInterfaceのリスト
     */
    fun getButtonChildren(): MutableList<ControlInterface> {
        if (mModifiable || mButtons == null) {
            mButtons = ArrayList()
            for (i in 0 until childCount) {
                val v = getChildAt(i)
                if (v is ControlInterface) {
                    mButtons!!.add(v)
                }
            }
        }
        return mButtons!!
    }

    /** 全てのボタンの位置を再計算して更新します。 */
    fun refreshControlButtonPositions() {
        for (button in getButtonChildren()) {
            button.setDynamicX(button.properties.dynamicX!!)
            button.setDynamicY(button.properties.dynamicY!!)
        }
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        if (child is ControlInterface && mControlPopup != null) {
            mControlPopup!!.disappearColor()
            mControlPopup!!.disappear()
        }
    }

    /**
     * コントロールボタンの編集UIを開きます。
     * @param button 編集対象のボタン
     */
    fun editControlButton(button: ControlInterface) {
        if (mControlPopup == null) {
            mControlPopup = EditControlPopup(context, this)
            post { editControlButton(button) }
            return
        }
        mControlPopup!!.internalChanges = true
        mControlPopup!!.setCurrentlyEditedButton(button)
        mControlPopup!!.appear(button.controlView.x + button.controlView.width / 2f < currentDisplayMetrics.widthPixels / 2f)
        button.loadEditValues(mControlPopup!!)
        mControlPopup!!.internalChanges = false
        mControlPopup!!.disappearColor()
        if (mHandleView == null) {
            mHandleView = ControlHandleView(context)
            addView(mHandleView)
        }
        mHandleView!!.setControlButton(button)
    }

    /** 編集ポップアップの表示位置を調整します。 */
    fun adaptPanelPosition() {
        mControlPopup?.adaptPanelPosition()
    }

    /** ビューとControlInterfaceのマッピングテーブル。 */
    val mapTable = HashMap<View, ControlInterface>()

    /**
     * タッチイベントを処理してスワイプ操作を実現します。
     * @param v タッチされたビュー
     * @param ev タッチイベント
     */
    fun onTouch(v: View, ev: MotionEvent) {
        var lastControlButton = mapTable[v]
        ev.offsetLocation(v.x, v.y)

        if (ev.actionMasked == MotionEvent.ACTION_UP
            || ev.actionMasked == MotionEvent.ACTION_CANCEL
            || ev.actionMasked == MotionEvent.ACTION_POINTER_UP) {
            lastControlButton?.sendKeyPresses(false)
            mapTable.remove(v)
            return
        }

        if (ev.actionMasked != MotionEvent.ACTION_MOVE) return

        if (lastControlButton != null) {
            if (ev.x > lastControlButton.controlView.x
                && ev.x < lastControlButton.controlView.x + lastControlButton.controlView.width
                && ev.y > lastControlButton.controlView.y
                && ev.y < lastControlButton.controlView.y + lastControlButton.controlView.height) {
                return
            }
        }

        if (lastControlButton != null) lastControlButton.sendKeyPresses(false)
        mapTable.remove(v)

        for (button in getButtonChildren()) {
            if (!button.properties.isSwipeable) continue
            if (ev.x > button.controlView.x
                && ev.x < button.controlView.x + button.controlView.width
                && ev.y > button.controlView.y
                && ev.y < button.controlView.y + button.controlView.height) {
                if (button !== lastControlButton) {
                    button.sendKeyPresses(true)
                    mapTable[v] = button
                    return
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mModifiable && event.actionMasked != MotionEvent.ACTION_UP || mControlPopup == null) return true

        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        if (!imm.hideSoftInputFromWindow(windowToken, 0)) {
            if (mControlPopup!!.disappearLayer()) {
                mActionRow!!.setFollowedButton(null)
                mHandleView!!.hide()
            }
        }
        return true
    }

    /** 編集ウィンドウを閉じます。 */
    fun removeEditWindow() {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
        mControlPopup?.disappearColor()
        mControlPopup?.disappear()
        mActionRow?.setFollowedButton(null)
        mHandleView?.hide()
    }

    /**
     * レイアウトをファイルに保存します。
     * @param path 保存先のパス
     */
    fun save(path: String) {
        try {
            mLayout!!.save(path)
        } catch (e: IOException) {
            Logging.e("ControlLayout", "Failed to save the layout at:$path")
        }
    }

    /** @return メニューボタンがレイアウトに含まれているかどうか */
    fun hasMenuButton(): Boolean {
        for (controlInterface in getButtonChildren()) {
            for (keycode in controlInterface.properties.keycodes) {
                if (keycode == ControlData.SPECIALBTN_MENU) return true
            }
        }
        return false
    }

    /** @param menuListener メニューリスナーを設定 */
    fun setMenuListener(menuListener: ControlButtonMenuListener?) {
        this.mMenuListener = menuListener
    }

    /** メニューリスナーにメニュー押下を通知します。 */
    fun notifyAppMenu() {
        mMenuListener?.onClickedMenu()
    }

    /** @return MinecraftのGLサーフェスビュー */
    fun getGameSurface(): MinecraftGLSurface? {
        if (mGameSurface == null) {
            mGameSurface = findViewById(R.id.main_game_render_view)
        }
        return mGameSurface
    }

    /**
     * 変更がある場合は保存確認ダイアログを表示してからエディタを終了します。
     * @param editorExitable エディタ終了リスナー
     */
    fun askToExit(editorExitable: EditorExitable) {
        if (mIsModified) {
            openSaveAndExitDialog(editorExitable)
        } else {
            openExitDialog(editorExitable)
        }
    }

    /**
     * レイアウトを指定された名前でコントロールマップディレクトリに保存します。
     * @param name 保存するファイル名（拡張子なし）
     * @return 保存されたファイルのパス
     * @throws Exception 保存エラー
     */
    @Throws(Exception::class)
    fun saveToDirectory(name: String): String {
        val jsonPath = PathManager.DIR_CTRLMAP_PATH + "/" + name + ".json"
        saveLayout(jsonPath)
        return jsonPath
    }

    /**
     * 保存ダイアログを表示します。
     * @param title ダイアログのタイトル
     * @param confirmTask 確認後のタスク
     */
    private fun saveDialog(title: String?, confirmTask: Task<*>?) {
        val infoDialog = EditControlInfoDialog(context, true, mLayoutFileName, mInfoData!!)
        if (!title.isNullOrEmpty()) infoDialog.setTitle(title)
        infoDialog.setOnConfirmClickListener { fileName, controlInfoData ->
            try {
                val jsonPath = saveToDirectory(fileName)
                Toast.makeText(context, context.getString(R.string.generic_save) + ": " + jsonPath, Toast.LENGTH_SHORT).show()
                confirmTask?.execute()
            } catch (th: Throwable) {
                Tools.showError(context, th, true)
            }
            infoDialog.dismiss()
        }
        infoDialog.show()
    }

    /** 保存ダイアログを開きます。 */
    fun openSaveDialog() {
        saveDialog(context.getString(R.string.generic_save), null)
    }

    /**
     * 保存して終了するダイアログを開きます。
     * @param editorExitable エディタ終了リスナー
     */
    fun openSaveAndExitDialog(editorExitable: EditorExitable) {
        saveDialog(context.getString(R.string.global_save_and_exit),
            Task.runTask(TaskExecutors.getAndroidUI()) {
                editorExitable.exitEditor()
                null
            })
    }

    /** レイアウト読み込みダイアログを開きます。 */
    fun openLoadDialog() {
        val dialog = SelectControlsDialog(context, object : SelectControlsDialog.SelectedListener {
            override fun onSelected(file: File) {
                try {
                    loadLayout(file.absolutePath)
                } catch (e: IOException) {
                    Tools.showError(context, e)
                }
            }
        })
        dialog.show()
    }

    /** デフォルトレイアウト設定ダイアログを開きます。 */
    fun openSetDefaultDialog() {
        val dialog = SelectControlsDialog(context, object : SelectControlsDialog.SelectedListener {
            override fun onSelected(file: File) {
                val absolutePath = file.absolutePath
                try {
                    AllSettings.defaultCtrl.put(absolutePath).save()
                    loadLayout(absolutePath)
                } catch (e: Exception) {
                    if (e is IOException || e is JsonSyntaxException) {
                        Tools.showError(context, e)
                    }
                }
            }
        })
        dialog.setTitleText(R.string.customctrl_selectdefault)
        dialog.show()
    }

    /**
     * 編集終了確認ダイアログを開きます。
     * @param exitListener 終了リスナー
     */
    fun openExitDialog(exitListener: EditorExitable) {
        TipDialog.Builder(context)
            .setTitle(R.string.customctrl_editor_exit_title)
            .setMessage(R.string.customctrl_editor_exit_msg)
            .setConfirmClickListener { exitListener.exitEditor() }
            .showDialog()
    }

    /** @return コントロールの表示状態 */
    fun areControlVisible(): Boolean = mControlVisible
}
