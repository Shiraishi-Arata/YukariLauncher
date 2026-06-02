package com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview

import android.animation.ObjectAnimator
import com.arata.yukarilauncher.Tools
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import com.google.android.material.materialswitch.MaterialSwitch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.kdt.DefocusableScrollView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.listener.SimpleTextWatcher
import com.arata.yukarilauncher.ui.dialog.KeyboardDialog
import com.arata.yukarilauncher.feature.awt.EfficientAndroidLWJGLKeycode
import com.arata.yukarilauncher.ui.view.colorselector.ColorSelector
import com.arata.yukarilauncher.ui.view.colorselector.ColorSelectionListener
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlDrawerData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlJoystickData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlDrawer
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlInterface

/**
 * コントロール編集用のポップアップパネル。
 * 名前、サイズ、キー割り当て、色、透明度など、コントロールの全プロパティを編集できます。
 */
class EditControlPopup(context: Context, parent: ViewGroup) {
    private val context: Context = context
    val mKeycodeSpinners = arrayOfNulls<Spinner>(4)
    private var keyboardDialog: KeyboardDialog? = null
    private val mScrollView: DefocusableScrollView
    private val mColorSelector: ColorSelector
    private val mEditPopupAnimator: ObjectAnimator
    private val mColorEditorAnimator: ObjectAnimator
    private val mMargin: Int
    var internalChanges = false

    private val mLayoutChangedListener = View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (internalChanges) return@OnLayoutChangeListener
        internalChanges = true
        val width = safeParseFloat(mWidthEditText!!.text.toString()).toInt()
        if (width >= 0 && Math.abs(right - width) > 1) {
            mWidthEditText!!.setText((right - left).toString())
        }
        val height = safeParseFloat(mHeightEditText!!.text.toString()).toInt()
        if (height >= 0 && Math.abs(bottom - height) > 1) {
            mHeightEditText!!.setText((bottom - top).toString())
        }
        internalChanges = false
    }

    var mNameEditText: EditText? = null
    var mWidthEditText: EditText? = null
    var mHeightEditText: EditText? = null
    var mToggleSwitch: MaterialSwitch? = null
    var mPassthroughSwitch: MaterialSwitch? = null
    var mSwipeableSwitch: MaterialSwitch? = null
    var mForwardLockSwitch: MaterialSwitch? = null
    var mAbsoluteTrackingSwitch: MaterialSwitch? = null
    var mRepeatedlySwitch: MaterialSwitch? = null
    var mOrientationSpinner: Spinner? = null
    var mKeycodeTextviews = arrayOfNulls<TextView>(4)
    var mStrokeWidthSeekbar: SeekBar? = null
    var mCornerRadiusSeekbar: SeekBar? = null
    var mAlphaSeekbar: SeekBar? = null
    var mRepeatedlyCpsSeekbar: SeekBar? = null
    var mRepeatedlyDelaySeekbar: SeekBar? = null
    var mStrokePercentTextView: TextView? = null
    var mCornerRadiusPercentTextView: TextView? = null
    var mAlphaPercentTextView: TextView? = null
    var mRepeatedlyCpsValueTextView: TextView? = null
    var mRepeatedlyDelayValueTextView: TextView? = null
    var mRepeatedlyCpsTextView: TextView? = null
    var mRepeatedlyDelayTextView: TextView? = null
    var mSelectBackgroundColor: TextView? = null
    var mSelectStrokeColor: TextView? = null
    var mAdapter: ArrayAdapter<String>? = null
    var mSpecialArray: MutableList<String>? = null
    var mDisplayInGameCheckbox: CheckBox? = null
    var mDisplayInMenuCheckbox: CheckBox? = null
    private var mRootView: ConstraintLayout? = null
    private var mDisplaying = false
    private var mDisplayingColor = false
    private var mCurrentlyEditedButton: ControlInterface? = null
    private var mOrientationTextView: TextView? = null
    private var mMappingTextView: TextView? = null
    private var mNameTextView: TextView? = null
    private var mCornerRadiusTextView: TextView? = null
    private var mVisibilityTextView: TextView? = null
    private var mSizeTextview: TextView? = null
    private var mSizeXTextView: TextView? = null

    init {
        mScrollView = LayoutInflater.from(context).inflate(R.layout.dialog_control_button_setting, parent, false) as DefocusableScrollView
        parent.addView(mScrollView)

        mMargin = context.resources.getDimensionPixelOffset(R.dimen._20sdp)

        mColorSelector = ColorSelector(context, parent, null)
        mColorSelector.getRootView().elevation = 11f
        mColorSelector.getRootView().translationZ = 11f
        mColorSelector.getRootView().x = -context.resources.getDimensionPixelOffset(R.dimen._280sdp).toFloat()

        mEditPopupAnimator = ObjectAnimator.ofFloat(mScrollView, "x", 0f).setDuration(600)
        mColorEditorAnimator = ObjectAnimator.ofFloat(mColorSelector.getRootView(), "x", 0f).setDuration(600)
        val decelerate: Interpolator = AccelerateDecelerateInterpolator()
        mEditPopupAnimator.interpolator = decelerate
        mColorEditorAnimator.interpolator = decelerate

        mScrollView.elevation = 10f
        mScrollView.translationZ = 10f
        mScrollView.x = -context.resources.getDimensionPixelOffset(R.dimen._280sdp).toFloat()

        bindLayout()
        loadAdapter()
        setupRealTimeListeners()
    }

    companion object {
        /**
         * テキストビューにパーセント表示を設定します。
         * @param textView 設定するテキストビュー
         * @param progress 進捗値
         */
        fun setPercentageText(textView: TextView, progress: Int) {
            textView.text = textView.context.getString(R.string.percent_format, progress)
        }
    }

    /**
     * 編集パネルを表示します。
     * @param fromRight 右側から表示するかどうか
     */
    fun appear(fromRight: Boolean) {
        disappearColor()
        if (fromRight) {
            if (!mDisplaying || !isAtRight()) {
                mEditPopupAnimator.setFloatValues(Tools.currentDisplayMetrics.widthPixels.toFloat(), (Tools.currentDisplayMetrics.widthPixels - mScrollView.width - mMargin).toFloat())
                mEditPopupAnimator.start()
            }
        } else {
            if (!mDisplaying || isAtRight()) {
                mEditPopupAnimator.setFloatValues(-mScrollView.width.toFloat(), mMargin.toFloat())
                mEditPopupAnimator.start()
            }
        }
        mDisplaying = true
    }

    /** 編集パネルを非表示にします。 */
    fun disappear() {
        if (!mDisplaying) return
        mDisplaying = false
        if (isAtRight())
            mEditPopupAnimator.setFloatValues((Tools.currentDisplayMetrics.widthPixels - mScrollView.width - mMargin).toFloat(), Tools.currentDisplayMetrics.widthPixels.toFloat())
        else
            mEditPopupAnimator.setFloatValues(mMargin.toFloat(), -mScrollView.width.toFloat())
        mEditPopupAnimator.start()
    }

    /**
     * カラーピッカーを表示します。
     * @param fromRight 右側から表示するかどうか
     * @param color 初期色
     */
    fun appearColor(fromRight: Boolean, color: Int) {
        if (fromRight) {
            if (!mDisplayingColor || !isAtRight()) {
                mColorEditorAnimator.setFloatValues(Tools.currentDisplayMetrics.widthPixels.toFloat(), (Tools.currentDisplayMetrics.widthPixels - mScrollView.width - mMargin).toFloat())
                mColorEditorAnimator.start()
            }
        } else {
            if (!mDisplayingColor || isAtRight()) {
                mColorEditorAnimator.setFloatValues(-mScrollView.width.toFloat(), mMargin.toFloat())
                mColorEditorAnimator.start()
            }
        }

        val params = mColorSelector.getRootView().layoutParams
        params.height = mScrollView.height
        mColorSelector.getRootView().layoutParams = params

        mDisplayingColor = true
        mColorSelector.show(if (color == -1) Color.WHITE else color)
    }

    /** カラーピッカーを非表示にします。 */
    fun disappearColor() {
        if (!mDisplayingColor) return
        mDisplayingColor = false
        if (isAtRight())
            mColorEditorAnimator.setFloatValues((Tools.currentDisplayMetrics.widthPixels - mScrollView.width - mMargin).toFloat(), Tools.currentDisplayMetrics.widthPixels.toFloat())
        else
            mColorEditorAnimator.setFloatValues(mMargin.toFloat(), -mScrollView.width.toFloat())
        mColorEditorAnimator.start()
    }

    /**
     * レイヤーを非表示にします。カラーピッカーが表示中の場合はそちらを先に隠します。
     * @return メインパネルが非表示になった場合はtrue
     */
    fun disappearLayer(): Boolean {
        return if (mDisplayingColor) {
            disappearColor()
            false
        } else {
            disappear()
            true
        }
    }

    /** 編集パネルの表示位置を現在のボタン位置に合わせて調整します。 */
    fun adaptPanelPosition() {
        if (mDisplaying) {
            val isAtRight = mCurrentlyEditedButton!!.controlView.x + mCurrentlyEditedButton!!.controlView.width / 2f < Tools.currentDisplayMetrics.widthPixels / 2f
            appear(isAtRight)
        }
    }

    /** ポップアップを破棄し、リソースを解放します。 */
    fun destroy() {
        (mScrollView.parent as ViewGroup).removeView(mColorSelector.getRootView())
        (mScrollView.parent as ViewGroup).removeView(mScrollView)
    }

    /** アダプターを読み込みます。 */
    private fun loadAdapter() {
        mAdapter = ArrayAdapter(context, R.layout.item_centered_textview)
        mSpecialArray = ControlData.buildSpecialButtonArray(context) as? MutableList<String> ?: mutableListOf()

        mAdapter!!.addAll(mSpecialArray!!)
        mAdapter!!.addAll(*EfficientAndroidLWJGLKeycode.generateKeyName().filterNotNull().toTypedArray())
        mAdapter!!.setDropDownViewResource(android.R.layout.simple_list_item_single_choice)

        for (spinner in mKeycodeSpinners) {
            spinner!!.adapter = mAdapter
        }

        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item)
        adapter.addAll(*ControlDrawerData.getOrientations(context))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice)
        mOrientationSpinner!!.adapter = adapter
    }

    /** デフォルトの表示設定を適用します。 */
    private fun setDefaultVisibilitySetting() {
        for (i in 0 until mRootView!!.childCount) {
            mRootView!!.getChildAt(i).visibility = View.VISIBLE
        }
        for (s in mKeycodeSpinners) {
            s!!.visibility = View.INVISIBLE
        }
    }

    /** @return パネルが画面右側に表示されているかどうか */
    private fun isAtRight(): Boolean {
        return mScrollView.x > Tools.currentDisplayMetrics.widthPixels / 2f
    }

    /**
     * ControlDataの値を編集パネルに読み込みます。
     * @param data 読み込むコントロールデータ
     */
    fun loadValues(data: ControlData) {
        setDefaultVisibilitySetting()
        mOrientationTextView!!.visibility = View.GONE
        mOrientationSpinner!!.visibility = View.GONE
        mForwardLockSwitch!!.visibility = View.GONE
        mAbsoluteTrackingSwitch!!.visibility = View.GONE

        mNameEditText!!.setText(data.name)
        mWidthEditText!!.setText(data.getWidth().toInt().toString())
        mHeightEditText!!.setText(data.getHeight().toInt().toString())

        mAlphaSeekbar!!.progress = (data.opacity * 100).toInt()
        mStrokeWidthSeekbar!!.progress = (data.strokeWidth * 10).toInt()
        mCornerRadiusSeekbar!!.progress = data.cornerRadius.toInt()

        setPercentageText(mAlphaPercentTextView!!, (data.opacity * 100).toInt())
        setPercentageText(mStrokePercentTextView!!, (data.strokeWidth * 10).toInt())
        setPercentageText(mCornerRadiusPercentTextView!!, data.cornerRadius.toInt())

        mToggleSwitch!!.isChecked = data.isToggle
        mPassthroughSwitch!!.isChecked = data.passThruEnabled
        mSwipeableSwitch!!.isChecked = data.isSwipeable
        mRepeatedlySwitch!!.isChecked = data.repeatedlyEnabled
        mRepeatedlyCpsSeekbar!!.progress = Math.max(0, data.repeatCps - 1)
        mRepeatedlyDelaySeekbar!!.progress = Math.max(0, data.repeatLongPressDelayMs / 100)
        mRepeatedlyCpsValueTextView!!.text = data.repeatCps.toString()
        mRepeatedlyDelayValueTextView!!.text = context.getString(R.string.customctrl_repeatedly_delay_value, data.repeatLongPressDelayMs)
        updateRepeatedlyVisibility(data.repeatedlyEnabled)

        mDisplayInGameCheckbox!!.isChecked = data.displayInGame
        mDisplayInMenuCheckbox!!.isChecked = data.displayInMenu

        for (i in data.keycodes.indices) {
            if (data.keycodes[i] < 0) {
                mKeycodeSpinners[i]!!.setSelection(data.keycodes[i] + mSpecialArray!!.size)
            } else {
                mKeycodeSpinners[i]!!.setSelection(EfficientAndroidLWJGLKeycode.getIndexByValue(data.keycodes[i]) + mSpecialArray!!.size)
            }
        }
    }

    /**
     * ControlDrawerDataの値を編集パネルに読み込みます。
     * @param data 読み込むドロワーデータ
     */
    fun loadValues(data: ControlDrawerData) {
        loadValues(data.properties)
        mOrientationSpinner!!.setSelection(ControlDrawerData.orientationToInt(data.orientation))
        mMappingTextView!!.visibility = View.GONE
        for (i in mKeycodeSpinners.indices) {
            mKeycodeSpinners[i]!!.visibility = View.GONE
            mKeycodeTextviews[i]!!.visibility = View.GONE
        }
        mOrientationTextView!!.visibility = View.VISIBLE
        mOrientationSpinner!!.visibility = View.VISIBLE
        mSwipeableSwitch!!.visibility = View.GONE
        mPassthroughSwitch!!.visibility = View.GONE
        mToggleSwitch!!.visibility = View.GONE
    }

    /**
     * ジョイスティックデータの値を編集パネルに読み込みます。
     * @param data 読み込むジョイスティックデータ
     */
    fun loadJoystickValues(data: ControlJoystickData) {
        loadValues(data as ControlData)
        mMappingTextView!!.visibility = View.GONE
        for (i in mKeycodeSpinners.indices) {
            mKeycodeSpinners[i]!!.visibility = View.GONE
            mKeycodeTextviews[i]!!.visibility = View.GONE
        }
        mNameTextView!!.visibility = View.GONE
        mNameEditText!!.visibility = View.GONE
        mCornerRadiusTextView!!.visibility = View.GONE
        mCornerRadiusSeekbar!!.visibility = View.GONE
        mCornerRadiusPercentTextView!!.visibility = View.GONE
        mSwipeableSwitch!!.visibility = View.GONE
        mPassthroughSwitch!!.visibility = View.GONE
        mToggleSwitch!!.visibility = View.GONE
        mForwardLockSwitch!!.visibility = View.VISIBLE
        mForwardLockSwitch!!.isChecked = data.forwardLock
        mAbsoluteTrackingSwitch!!.visibility = View.VISIBLE
        mAbsoluteTrackingSwitch!!.isChecked = data.absolute
    }

    /**
     * サブボタンの値を編集パネルに読み込みます。
     * @param data 読み込むサブボタンデータ
     * @param drawerOrientation 親ドロワーの向き
     */
    fun loadSubButtonValues(data: ControlData, drawerOrientation: ControlDrawerData.Orientation) {
        loadValues(data)
        if (drawerOrientation != ControlDrawerData.Orientation.FREE) {
            mSizeTextview!!.visibility = View.GONE
            mSizeXTextView!!.visibility = View.GONE
            mWidthEditText!!.visibility = View.GONE
            mHeightEditText!!.visibility = View.GONE
        }
        mVisibilityTextView!!.visibility = View.GONE
        mDisplayInMenuCheckbox!!.visibility = View.GONE
        mDisplayInGameCheckbox!!.visibility = View.GONE
    }

    /** レイアウトのビューをバインドします。 */
    private fun bindLayout() {
        mRootView = mScrollView.findViewById(R.id.edit_layout)
        mNameEditText = mScrollView.findViewById(R.id.editName_editText)
        mWidthEditText = mScrollView.findViewById(R.id.editSize_editTextX)
        mHeightEditText = mScrollView.findViewById(R.id.editSize_editTextY)
        mToggleSwitch = mScrollView.findViewById(R.id.checkboxToggle)
        mPassthroughSwitch = mScrollView.findViewById(R.id.checkboxPassThrough)
        mSwipeableSwitch = mScrollView.findViewById(R.id.checkboxSwipeable)
        mForwardLockSwitch = mScrollView.findViewById(R.id.checkboxForwardLock)
        mAbsoluteTrackingSwitch = mScrollView.findViewById(R.id.checkboxAbsoluteFingerTracking)
        mRepeatedlySwitch = mScrollView.findViewById(R.id.checkboxRepeatedly)
        mKeycodeSpinners[0] = mScrollView.findViewById(R.id.editMapping_spinner_1)
        mKeycodeSpinners[1] = mScrollView.findViewById(R.id.editMapping_spinner_2)
        mKeycodeSpinners[2] = mScrollView.findViewById(R.id.editMapping_spinner_3)
        mKeycodeSpinners[3] = mScrollView.findViewById(R.id.editMapping_spinner_4)
        mKeycodeTextviews[0] = mScrollView.findViewById(R.id.mapping_1_textview)
        mKeycodeTextviews[1] = mScrollView.findViewById(R.id.mapping_2_textview)
        mKeycodeTextviews[2] = mScrollView.findViewById(R.id.mapping_3_textview)
        mKeycodeTextviews[3] = mScrollView.findViewById(R.id.mapping_4_textview)
        mOrientationSpinner = mScrollView.findViewById(R.id.editOrientation_spinner)
        mStrokeWidthSeekbar = mScrollView.findViewById(R.id.editStrokeWidth_seekbar)
        mCornerRadiusSeekbar = mScrollView.findViewById(R.id.editCornerRadius_seekbar)
        mAlphaSeekbar = mScrollView.findViewById(R.id.editButtonOpacity_seekbar)
        mRepeatedlyCpsSeekbar = mScrollView.findViewById(R.id.editRepeatedlyCps_seekbar)
        mRepeatedlyDelaySeekbar = mScrollView.findViewById(R.id.editRepeatedlyDelay_seekbar)
        mSelectBackgroundColor = mScrollView.findViewById(R.id.editBackgroundColor_textView)
        mSelectStrokeColor = mScrollView.findViewById(R.id.editStrokeColor_textView)
        mStrokePercentTextView = mScrollView.findViewById(R.id.editStrokeWidth_textView_percent)
        mAlphaPercentTextView = mScrollView.findViewById(R.id.editButtonOpacity_textView_percent)
        mCornerRadiusPercentTextView = mScrollView.findViewById(R.id.editCornerRadius_textView_percent)
        mRepeatedlyCpsValueTextView = mScrollView.findViewById(R.id.editRepeatedlyCpsValue_textView)
        mRepeatedlyDelayValueTextView = mScrollView.findViewById(R.id.editRepeatedlyDelayValue_textView)
        mRepeatedlyCpsTextView = mScrollView.findViewById(R.id.editRepeatedlyCps_textView)
        mRepeatedlyDelayTextView = mScrollView.findViewById(R.id.editRepeatedlyDelay_textView)
        mDisplayInGameCheckbox = mScrollView.findViewById(R.id.visibility_game_checkbox)
        mDisplayInMenuCheckbox = mScrollView.findViewById(R.id.visibility_menu_checkbox)

        mMappingTextView = mScrollView.findViewById(R.id.editMapping_textView)
        mOrientationTextView = mScrollView.findViewById(R.id.editOrientation_textView)
        mNameTextView = mScrollView.findViewById(R.id.editName_textView)
        mCornerRadiusTextView = mScrollView.findViewById(R.id.editCornerRadius_textView)
        mVisibilityTextView = mScrollView.findViewById(R.id.visibility_textview)
        mSizeTextview = mScrollView.findViewById(R.id.editSize_textView)
        mSizeXTextView = mScrollView.findViewById(R.id.editSize_x_textView)

        keyboardDialog = KeyboardDialog(context)
    }

    /** リアルタイム編集のリスナーを設定します。 */
    fun setupRealTimeListeners() {
        mNameEditText!!.addTextChangedListener(SimpleTextWatcher { s ->
            if (internalChanges) return@SimpleTextWatcher
            mCurrentlyEditedButton!!.properties.name = s.toString()
            mCurrentlyEditedButton!!.setProperties(mCurrentlyEditedButton!!.properties, false)
        })

        mWidthEditText!!.addTextChangedListener(SimpleTextWatcher { s ->
            if (internalChanges) return@SimpleTextWatcher
            val width = safeParseFloat(s.toString())
            if (width >= 0) {
                mCurrentlyEditedButton!!.properties.setWidth(width)
                if (mCurrentlyEditedButton!!.properties is ControlJoystickData) {
                    mCurrentlyEditedButton!!.properties.setHeight(width)
                }
                mCurrentlyEditedButton!!.updateProperties()
            }
        })

        mHeightEditText!!.addTextChangedListener(SimpleTextWatcher { s ->
            if (internalChanges) return@SimpleTextWatcher
            val height = safeParseFloat(s.toString())
            if (height >= 0) {
                mCurrentlyEditedButton!!.properties.setHeight(height)
                if (mCurrentlyEditedButton!!.properties is ControlJoystickData) {
                    mCurrentlyEditedButton!!.properties.setWidth(height)
                }
                mCurrentlyEditedButton!!.updateProperties()
            }
        })

        mSwipeableSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.properties.isSwipeable = isChecked
        }
        mToggleSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.properties.isToggle = isChecked
        }
        mPassthroughSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.properties.passThruEnabled = isChecked
        }
        mRepeatedlySwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.properties.repeatedlyEnabled = isChecked
            updateRepeatedlyVisibility(isChecked)
        }
        mForwardLockSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            if (mCurrentlyEditedButton!!.properties is ControlJoystickData) {
                (mCurrentlyEditedButton!!.properties as ControlJoystickData).forwardLock = isChecked
            }
        }
        mAbsoluteTrackingSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            if (mCurrentlyEditedButton!!.properties is ControlJoystickData) {
                (mCurrentlyEditedButton!!.properties as ControlJoystickData).absolute = isChecked
            }
        }

        mAlphaSeekbar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                mCurrentlyEditedButton!!.properties.opacity = mAlphaSeekbar!!.progress / 100f
                mCurrentlyEditedButton!!.controlView.alpha = mAlphaSeekbar!!.progress / 100f
                setPercentageText(mAlphaPercentTextView!!, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        mStrokeWidthSeekbar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                mCurrentlyEditedButton!!.properties.strokeWidth = mStrokeWidthSeekbar!!.progress / 10f
                mCurrentlyEditedButton!!.setBackground()
                setPercentageText(mStrokePercentTextView!!, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        mCornerRadiusSeekbar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                mCurrentlyEditedButton!!.properties.cornerRadius = mCornerRadiusSeekbar!!.progress.toFloat()
                mCurrentlyEditedButton!!.setBackground()
                setPercentageText(mCornerRadiusPercentTextView!!, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        mRepeatedlyCpsSeekbar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                val cps = progress + 1
                mCurrentlyEditedButton!!.properties.repeatCps = cps
                mRepeatedlyCpsValueTextView!!.text = cps.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        mRepeatedlyDelaySeekbar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                val delay = progress * 100
                mCurrentlyEditedButton!!.properties.repeatLongPressDelayMs = delay
                mRepeatedlyDelayValueTextView!!.text = context.getString(R.string.customctrl_repeatedly_delay_value, delay)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        for (i in mKeycodeSpinners.indices) {
            val finalI = i
            mKeycodeTextviews[i]!!.setOnClickListener {
                keyboardDialog!!.setOnKeycodeSelectListener { index ->
                    mKeycodeSpinners[finalI]!!.setSelection(index)
                    updateKeycodeText(index, finalI)
                }.show()
            }
            mKeycodeSpinners[i]!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    updateKeycodeText(position, finalI)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            })
        }

        mOrientationSpinner!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (mCurrentlyEditedButton is ControlDrawer) {
                    (mCurrentlyEditedButton as ControlDrawer).drawerData.orientation = ControlDrawerData.intToOrientation(mOrientationSpinner!!.selectedItemPosition)!!
                    (mCurrentlyEditedButton as ControlDrawer).syncButtons()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        })

        mDisplayInGameCheckbox!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.properties.displayInGame = isChecked
        }

        mDisplayInMenuCheckbox!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.properties.displayInMenu = isChecked
        }

        mSelectStrokeColor!!.setOnClickListener {
            mColorSelector.setAlphaEnabled(false)
            mColorSelector.setColorSelectionListener(object : ColorSelectionListener {
                override fun onColorSelected(color: Int) {
                    mCurrentlyEditedButton!!.properties.strokeColor = color
                    mCurrentlyEditedButton!!.setBackground()
                }
            })
            appearColor(isAtRight(), mCurrentlyEditedButton!!.properties.strokeColor)
        }

        mSelectBackgroundColor!!.setOnClickListener {
            mColorSelector.setAlphaEnabled(true)
            mColorSelector.setColorSelectionListener(object : ColorSelectionListener {
                override fun onColorSelected(color: Int) {
                    mCurrentlyEditedButton!!.properties.bgColor = color
                    mCurrentlyEditedButton!!.setBackground()
                }
            })
            appearColor(isAtRight(), mCurrentlyEditedButton!!.properties.bgColor)
        }
    }

    /**
     * リピート設定の表示/非表示を更新します。
     * @param isVisible 表示する場合はtrue
     */
    private fun updateRepeatedlyVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        mRepeatedlyCpsTextView!!.visibility = visibility
        mRepeatedlyCpsSeekbar!!.visibility = visibility
        mRepeatedlyCpsValueTextView!!.visibility = visibility
        mRepeatedlyDelayTextView!!.visibility = visibility
        mRepeatedlyDelaySeekbar!!.visibility = visibility
        mRepeatedlyDelayValueTextView!!.visibility = visibility
    }

    /**
     * キーコードテキストを更新します。
     * @param index 選択されたインデックス
     * @param finalI キーコードスロットのインデックス
     */
    private fun updateKeycodeText(index: Int, finalI: Int) {
        if (index < mSpecialArray!!.size) {
            mCurrentlyEditedButton!!.properties.keycodes[finalI] = mKeycodeSpinners[finalI]!!.selectedItemPosition - mSpecialArray!!.size
        } else {
                mCurrentlyEditedButton!!.properties.keycodes[finalI] = EfficientAndroidLWJGLKeycode.getValueByIndex(mKeycodeSpinners[finalI]!!.selectedItemPosition - mSpecialArray!!.size).toInt()
        }
        mKeycodeTextviews[finalI]!!.text = mKeycodeSpinners[finalI]!!.selectedItem as String
    }

    /**
     * 文字列を安全にFloatにパースします。
     * @param string パースする文字列
     * @return Float値、失敗時は-1f
     */
    private fun safeParseFloat(string: String): Float {
        var out = -1f
        try {
            out = string.toFloat()
        } catch (e: NumberFormatException) {
            Logging.e("EditControlPopup", e.toString())
        }
        return out
    }

    /**
     * 現在編集中のボタンを設定します。
     * @param button 編集対象のコントロール
     */
    fun setCurrentlyEditedButton(button: ControlInterface) {
        mCurrentlyEditedButton?.let {
            it.controlView.removeOnLayoutChangeListener(mLayoutChangedListener)
        }
        mCurrentlyEditedButton = button
        mCurrentlyEditedButton!!.controlView.addOnLayoutChangeListener(mLayoutChangedListener)
    }
}