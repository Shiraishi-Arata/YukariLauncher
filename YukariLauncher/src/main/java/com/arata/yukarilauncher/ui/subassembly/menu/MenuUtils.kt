package com.arata.yukarilauncher.ui.subassembly.menu

import android.widget.SeekBar
import android.widget.CompoundButton
import android.widget.TextView

/**
 * メニュー操作用のユーティリティクラス
 */
class MenuUtils {
    companion object {
        /**
         * シークバーの値を調整する
         * @param seekBar シークバー
         * @param v 調整する値
         */
        @JvmStatic
        fun adjustSeekbar(seekBar: SeekBar, v: Int) {
            seekBar.progress += v
        }

        /**
         * Switchの選択状態を反転する
         */
        @JvmStatic
        fun toggleSwitchState(switchView: CompoundButton) {
            switchView.isChecked = !switchView.isChecked
        }

        /**
         * シークバーの値を初期化する
         */
        @JvmStatic
        fun initSeekBarValue(seek: SeekBar, value: Int, valueView: TextView, suffix: String) {
            seek.progress = value
            updateSeekbarValue(value, valueView, suffix)
        }

        /**
         * シークバーの横に表示する数値テキストを更新する
         */
        @JvmStatic
        fun updateSeekbarValue(value: Int, valueView: TextView, suffix: String) {
            val valueText = "$value $suffix"
            valueView.text = valueText.trim { it <= ' ' }
        }
    }
}
