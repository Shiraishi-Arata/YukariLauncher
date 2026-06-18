package com.arata.yukarilauncher.utils

import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.context.ContextExecutor.Companion.getString
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class NumberWithUnits {
    companion object {
        private val UNITS_EN = arrayOf("", "K", "M")
        private val UNITS_ZH = arrayOf(
            "",
            getString(R.string.generic_wan),
            getString(R.string.generic_yi)
        )

        /**
         * 数値に単位を付けてフォーマットする
         * 英語の場合は千（K）・百万（M）、中国語の場合は万・億の単位を使用する
         */
        @JvmStatic
        fun formatNumberWithUnit(number: Long, isEnglish: Boolean): String {
            return if (isEnglish) {
                formatNumberWithUnitEnglish(number)
            } else {
                formatNumberWithUnitChinese(number)
            }
        }

        /**
         * 中国語の単位（万・億）で数値をフォーマットする
         */
        private fun formatNumberWithUnitChinese(number: Long): String {
            return formatNumber(number, 10000, UNITS_ZH)
        }

        /**
         * 英語の単位（K・M）で数値をフォーマットする
         */
        private fun formatNumberWithUnitEnglish(number: Long): String {
            return formatNumber(number, 1000, UNITS_EN)
        }

        /**
         * 指定されたステップ値と単位配列を使用して数値をフォーマットする
         * 単位が空の場合はフォーマットせずに元の値を返す
         */
        private fun formatNumber(number: Long, stage: Int, units: Array<String>): String {
            var bigDecimal = BigDecimal(number)
            var unitIndex = 0

            while (bigDecimal >= BigDecimal.valueOf(stage.toLong()) && unitIndex < units.size - 1) {
                bigDecimal = bigDecimal.divide(BigDecimal.valueOf(stage.toLong()), 2, RoundingMode.DOWN)
                unitIndex++
            }

            // 単位が空の場合はフォーマットせずに元の値を返す
            if (units[unitIndex].isEmpty()) {
                return number.toString()
            } else {
                val df = DecimalFormat("#.00")
                val formattedNumber = df.format(bigDecimal.setScale(2, RoundingMode.DOWN).toDouble())
                return StringUtils.insertSpace(formattedNumber, units[unitIndex])
            }
        }
    }
}