package com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.ui.dialog.KeyboardDialog
import com.arata.yukarilauncher.feature.awt.EfficientAndroidLWJGLKeycode
import com.arata.yukarilauncher.ui.view.GrabListener
import com.arata.yukarilauncher.Tools

class GamepadMapperAdapter(context: Context) : RecyclerView.Adapter<GamepadMapperAdapter.ViewHolder>(), GamepadDataProvider {
    companion object {
        private const val BUTTON_COUNT = 20
    }

    private val keyboardDialog: KeyboardDialog
    private lateinit var mSimulatedGamepadMap: GamepadMap
    private lateinit var mRebinderButtons: Array<RebinderButton>
    private lateinit var mRealButtons: Array<GamepadEmulatedButton>
    private val mKeyAdapter: ArrayAdapter<String>
    private val mSpecialKeycodeCount: Int
    private var mGamepadGrabListener: GrabListener? = null
    private var mGrabState = false
    private var mOldState = false

    init {
        GamepadMapStore.load()
        mKeyAdapter = ArrayAdapter(context, R.layout.item_centered_textview_large)
        val specialKeycodeNames = GamepadMap.getSpecialKeycodeNames(context)
        mSpecialKeycodeCount = specialKeycodeNames.size
        mKeyAdapter.addAll(*specialKeycodeNames)
        mKeyAdapter.addAll(*EfficientAndroidLWJGLKeycode.generateKeyName().filterNotNull().toTypedArray())
        createRebinderMap()
        updateRealButtons()

        keyboardDialog = KeyboardDialog(context, true)
    }

    private fun createRebinderMap() {
        val rebinderButtons = arrayOfNulls<RebinderButton>(BUTTON_COUNT)
        val realButtons = arrayOfNulls<GamepadEmulatedButton>(BUTTON_COUNT)
        mSimulatedGamepadMap = GamepadMap()
        var index = 0
        mSimulatedGamepadMap.BUTTON_A = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.button_a, R.string.controller_button_a) }
        index++
        mSimulatedGamepadMap.BUTTON_B = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.button_b, R.string.controller_button_b) }
        index++
        mSimulatedGamepadMap.BUTTON_X = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.button_x, R.string.controller_button_x) }
        index++
        mSimulatedGamepadMap.BUTTON_Y = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.button_y, R.string.controller_button_y) }
        index++
        mSimulatedGamepadMap.BUTTON_START = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.button_start, R.string.controller_button_start) }
        index++
        mSimulatedGamepadMap.BUTTON_SELECT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.button_select, R.string.generic_select) }
        index++
        mSimulatedGamepadMap.TRIGGER_RIGHT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.trigger_right, R.string.controller_button_trigger_right) }
        index++
        mSimulatedGamepadMap.TRIGGER_LEFT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.trigger_left, R.string.controller_button_trigger_left) }
        index++
        mSimulatedGamepadMap.SHOULDER_RIGHT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.shoulder_right, R.string.controller_button_shoulder_right) }
        index++
        mSimulatedGamepadMap.SHOULDER_LEFT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.shoulder_left, R.string.controller_button_shoulder_left) }
        index++
        mSimulatedGamepadMap.DIRECTION_FORWARD = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.stick_right, R.string.controller_direction_forward) }
        index++
        mSimulatedGamepadMap.DIRECTION_RIGHT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.stick_right, R.string.controller_direction_right) }
        index++
        mSimulatedGamepadMap.DIRECTION_LEFT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.stick_right, R.string.controller_direction_left) }
        index++
        mSimulatedGamepadMap.DIRECTION_BACKWARD = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.stick_right, R.string.controller_direction_backward) }
        index++
        mSimulatedGamepadMap.THUMBSTICK_RIGHT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.stick_right_click, R.string.controller_stick_press_r) }
        index++
        mSimulatedGamepadMap.THUMBSTICK_LEFT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.stick_left_click, R.string.controller_stick_press_l) }
        index++
        mSimulatedGamepadMap.DPAD_UP = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.dpad_up, R.string.controller_dpad_up) }
        index++
        mSimulatedGamepadMap.DPAD_DOWN = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.dpad_down, R.string.controller_dpad_down) }
        index++
        mSimulatedGamepadMap.DPAD_RIGHT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.dpad_right, R.string.controller_dpad_right) }
        index++
        mSimulatedGamepadMap.DPAD_LEFT = rebinderButtons[index].also { rebinderButtons[index] = RebinderButton(R.drawable.dpad_left, R.string.controller_dpad_left) }
        @Suppress("UNCHECKED_CAST")
        mRebinderButtons = rebinderButtons as Array<RebinderButton>
        @Suppress("UNCHECKED_CAST")
        mRealButtons = realButtons as Array<GamepadEmulatedButton>
    }

    private fun updateRealButtons() {
        val currentRealMap = if (mGrabState) GamepadMapStore.getGameMap() else GamepadMapStore.getMenuMap()
        var index = 0
        mRealButtons[index++] = currentRealMap.BUTTON_A!!
        mRealButtons[index++] = currentRealMap.BUTTON_B!!
        mRealButtons[index++] = currentRealMap.BUTTON_X!!
        mRealButtons[index++] = currentRealMap.BUTTON_Y!!
        mRealButtons[index++] = currentRealMap.BUTTON_START!!
        mRealButtons[index++] = currentRealMap.BUTTON_SELECT!!
        mRealButtons[index++] = currentRealMap.TRIGGER_RIGHT!!
        mRealButtons[index++] = currentRealMap.TRIGGER_LEFT!!
        mRealButtons[index++] = currentRealMap.SHOULDER_RIGHT!!
        mRealButtons[index++] = currentRealMap.SHOULDER_LEFT!!
        mRealButtons[index++] = currentRealMap.DIRECTION_FORWARD!!
        mRealButtons[index++] = currentRealMap.DIRECTION_RIGHT!!
        mRealButtons[index++] = currentRealMap.DIRECTION_LEFT!!
        mRealButtons[index++] = currentRealMap.DIRECTION_BACKWARD!!
        mRealButtons[index++] = currentRealMap.THUMBSTICK_RIGHT!!
        mRealButtons[index++] = currentRealMap.THUMBSTICK_LEFT!!
        mRealButtons[index++] = currentRealMap.DPAD_UP!!
        mRealButtons[index++] = currentRealMap.DPAD_DOWN!!
        mRealButtons[index++] = currentRealMap.DPAD_RIGHT!!
        mRealButtons[index] = currentRealMap.DPAD_LEFT!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.item_controller_mapping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.attach(position)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.detach()
    }

    override fun getItemCount(): Int = mRebinderButtons.size

    private fun updateStickIcons() {
        val stickIcon = if (mGrabState) R.drawable.stick_left else R.drawable.stick_right
        (mSimulatedGamepadMap.DIRECTION_FORWARD as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_BACKWARD as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_RIGHT as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_LEFT as RebinderButton).iconResourceId = stickIcon
    }

    override val menuMap: GamepadMap
        get() = mSimulatedGamepadMap

    override val gameMap: GamepadMap
        get() = mSimulatedGamepadMap

    override fun isGrabbing(): Boolean = mGrabState

    override fun attachGrabListener(grabListener: GrabListener) {
        mGamepadGrabListener = grabListener
        grabListener.onGrabState(mGrabState)
    }

    fun setGrabState(newState: Boolean) {
        mGrabState = newState
        mGamepadGrabListener?.onGrabState(newState)
        if (mGrabState == mOldState) return
        updateRealButtons()
        updateStickIcons()
        notifyItemRangeChanged(0, mRebinderButtons.size)
        mOldState = mGrabState
    }

    private class RebinderButton(iconResourceId: Int, localeResourceId: Int) : GamepadButton() {
        var iconResourceId: Int = iconResourceId
        val localeResourceId: Int = localeResourceId
        private var mButtonHolder: ViewHolder? = null

        fun changeViewHolder(viewHolder: ViewHolder?) {
            mButtonHolder = viewHolder
            if (mButtonHolder != null) mButtonHolder!!.setPressed(mIsDown)
        }

        override fun onDownStateChanged(isDown: Boolean) {
            if (mButtonHolder == null) return
            mButtonHolder!!.setPressed(isDown)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), AdapterView.OnItemSelectedListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        private val COLOR_ACTIVE_BUTTON = 0x2000FF00.toInt()

        val mContext: Context = itemView.context
        val mButtonIcon: ImageView = itemView.findViewById(R.id.controller_mapper_button)
        val mExpansionIndicator: ImageView = itemView.findViewById(R.id.controller_mapper_expand_button)
        val mKeySpinners: Array<Spinner>
        val mExpandedView: View = itemView.findViewById(R.id.controller_mapper_expanded_view)
        val mToggleableSwitch: SwitchCompat = itemView.findViewById(R.id.controller_mapper_toggleable_switch)
        val mKeycodeLabel: TextView = itemView.findViewById(R.id.controller_mapper_keycode_label)
        private var mAttachedPosition = -1
        private var mAttachedButton: GamepadEmulatedButton? = null
        private var mKeycodes: ShortArray = shortArrayOf()

        init {
            mToggleableSwitch.setOnCheckedChangeListener(this)
            val defaultView = itemView.findViewById<View>(R.id.controller_mapper_default_view)
            defaultView.setOnClickListener(this)
            mKeySpinners = arrayOf(
                itemView.findViewById(R.id.controller_mapper_key_spinner1),
                itemView.findViewById(R.id.controller_mapper_key_spinner2),
                itemView.findViewById(R.id.controller_mapper_key_spinner3),
                itemView.findViewById(R.id.controller_mapper_key_spinner4)
            )
            val mKeyClickViews = arrayOf(
                itemView.findViewById<View>(R.id.controller_mapper_key_view1),
                itemView.findViewById<View>(R.id.controller_mapper_key_view2),
                itemView.findViewById<View>(R.id.controller_mapper_key_view3),
                itemView.findViewById<View>(R.id.controller_mapper_key_view4)
            )
            for (i in mKeySpinners.indices) {
                mKeySpinners[i].adapter = mKeyAdapter
                val finalI = i
                mKeyClickViews[i].setOnClickListener {
                    keyboardDialog.setOnKeycodeSelectListener { index -> mKeySpinners[finalI].setSelection(index) }.show()
                }
                mKeySpinners[i].onItemSelectedListener = this
            }
        }

        fun attach(index: Int) {
            val rebinderButton = mRebinderButtons[index]
            mExpandedView.visibility = View.GONE
            mButtonIcon.setImageResource(rebinderButton.iconResourceId)
            val buttonName = mContext.getString(rebinderButton.localeResourceId)
            mButtonIcon.contentDescription = buttonName
            rebinderButton.changeViewHolder(this)

            val realButton = mRealButtons[index]
            mAttachedButton = realButton
            if (realButton is GamepadButton) {
                mToggleableSwitch.isChecked = realButton.isToggleable
                mToggleableSwitch.visibility = View.VISIBLE
            } else {
                mToggleableSwitch.visibility = View.GONE
            }

            mKeycodes = realButton.keycodes

            var spinnerIndex = 0
            while (spinnerIndex < mKeycodes.size) {
                val keySpinner = mKeySpinners[spinnerIndex]
                keySpinner.isEnabled = true
                val keyCode = mKeycodes[spinnerIndex]
                val selected: Int
                if (keyCode < 0) selected = keyCode + mSpecialKeycodeCount
                else selected = EfficientAndroidLWJGLKeycode.getIndexByValue(keyCode.toInt()) + mSpecialKeycodeCount
                keySpinner.setSelection(selected)
                spinnerIndex++
            }
            for (i in spinnerIndex until mKeySpinners.size) {
                mKeySpinners[i].isEnabled = false
            }
            updateKeycodeLabel()
            updateIndicator(mExpandedView.visibility != View.VISIBLE)

            mAttachedPosition = index
        }

        fun detach() {
            if (mAttachedPosition >= 0) {
                mRebinderButtons[mAttachedPosition].changeViewHolder(null)
            }
            mAttachedPosition = -1
            mAttachedButton = null
        }

        fun setPressed(pressed: Boolean) {
            itemView.setBackgroundColor(if (pressed) COLOR_ACTIVE_BUTTON else Color.TRANSPARENT)
        }

        private fun updateKeycodeLabel() {
            val labelBuilder = StringBuilder()
            var first = true
            val unspecifiedPosition = GamepadMap.UNSPECIFIED + mSpecialKeycodeCount
            for (keySpinner in mKeySpinners) {
                if (keySpinner.selectedItemPosition == unspecifiedPosition) continue
                if (!first) labelBuilder.append(" + ")
                else first = false
                labelBuilder.append(keySpinner.selectedItem.toString())
            }
            if (labelBuilder.isEmpty()) labelBuilder.append(mKeyAdapter.getItem(unspecifiedPosition))
            mKeycodeLabel.text = labelBuilder.toString()
        }

        private fun updateIndicator(rotation: Boolean) {
            mExpansionIndicator.rotation = if (rotation) 180f else 0f
        }

        override fun onItemSelected(adapterView: AdapterView<*>, view: View?, selectionIndex: Int, selectionId: Long) {
            if (mAttachedPosition == -1) return
            var editedKeycodeIndex = -1
            for (i in mKeySpinners.indices) {
                if (i >= mKeycodes.size) break
                if (adapterView !== mKeySpinners[i]) continue
                editedKeycodeIndex = i
                break
            }
            if (editedKeycodeIndex == -1) return
            val keycode_offset = selectionIndex - mSpecialKeycodeCount
            mKeycodes[editedKeycodeIndex] = if (selectionIndex <= mSpecialKeycodeCount) {
                keycode_offset.toShort()
            } else {
                EfficientAndroidLWJGLKeycode.getValueByIndex(keycode_offset)
            }
            updateKeycodeLabel()
            try {
                GamepadMapStore.save()
            } catch (e: Exception) {
                Tools.showError(adapterView.context, e)
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>) {}

        override fun onClick(view: View) {
            val visibility = mExpandedView.visibility
            when (visibility) {
                View.INVISIBLE, View.GONE -> {
                    updateIndicator(false)
                    mExpandedView.visibility = View.VISIBLE
                }
                View.VISIBLE -> {
                    updateIndicator(true)
                    mExpandedView.visibility = View.GONE
                }
            }
        }

        override fun onCheckedChanged(compoundButton: CompoundButton, checked: Boolean) {
            if (mAttachedButton !is GamepadButton) return
            (mAttachedButton as GamepadButton).isToggleable = checked
            try {
                GamepadMapStore.save()
            } catch (e: Exception) {
                Tools.showError(compoundButton.context, e)
            }
        }
    }
}