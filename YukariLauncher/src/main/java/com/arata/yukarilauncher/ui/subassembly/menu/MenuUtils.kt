package com.arata.yukarilauncher.ui.subassembly.menu

import android.widget.SeekBar
import android.widget.CompoundButton
import android.widget.TextView

class MenuUtils {
    companion object {
        /**
         * 调整滑动条的值
         * @param seekBar 滑动条
         * @param v 需要调整的值的大小
         */
        @JvmStatic
        fun adjustSeekbar(seekBar: SeekBar, v: Int) {
            seekBar.progress += v
        }

        /**
         * 反转Switch当前的选中状态
         */
        @JvmStatic
        fun toggleSwitchState(switchView: CompoundButton) {
            switchView.isChecked = !switchView.isChecked
        }

        /**
         * 初始化Seekbar的值
         */
        @JvmStatic
        fun initSeekBarValue(seek: SeekBar, value: Int, valueView: TextView, suffix: String) {
            seek.progress = value
            updateSeekbarValue(value, valueView, suffix)
        }

        /**
         * 更新Seekbar旁边数值的文本值
         */
        @JvmStatic
        fun updateSeekbarValue(value: Int, valueView: TextView, suffix: String) {
            val valueText = "$value $suffix"
            valueView.text = valueText.trim { it <= ' ' }
        }
    }
}