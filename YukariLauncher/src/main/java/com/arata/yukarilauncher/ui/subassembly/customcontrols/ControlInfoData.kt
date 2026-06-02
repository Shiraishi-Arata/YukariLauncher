package com.arata.yukarilauncher.ui.subassembly.customcontrols

import kotlin.math.min

/**
 * コントロール設定のメタ情報を保持するデータクラス
 */
class ControlInfoData : Comparable<ControlInfoData?> {
    @JvmField
    var fileName: String? = null
    @JvmField
    var name: String = "null"
    @JvmField
    var version: String = "null"
    @JvmField
    var author: String = "null"
    @JvmField
    var desc: String = "null"

    /**
     * 他のControlInfoDataと比較します（ファイル名・名前の大文字小文字を区別しない比較）。
     */
    override fun compareTo(other: ControlInfoData?): Int {
        other ?: run { throw NullPointerException("Cannot compare to null.") }

        val thisName = this.fileName ?: this.name
        val otherName = other.fileName ?: other.name

        return compareChar(thisName, otherName)
    }

    /**
     * 2つの文字列を文字単位で比較する
     */
    private fun compareChar(first: String?, second: String?): Int {
        val firstLength = first!!.length
        val secondLength = second!!.length

        for (i in 0 until min(firstLength.toDouble(), secondLength.toDouble()).toInt()) {
            val firstChar = first[i].lowercaseChar()
            val secondChar = second[i].lowercaseChar()

            val compare = firstChar.compareTo(secondChar)
            if (compare != 0) {
                return compare
            }
        }

        return firstLength.compareTo(secondLength)
    }
}