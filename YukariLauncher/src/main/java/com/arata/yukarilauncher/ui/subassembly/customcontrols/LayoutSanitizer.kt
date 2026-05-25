package com.arata.yukarilauncher.ui.subassembly.customcontrols

/** コントロールレイアウトデータを検証し、無効なエントリを除去するユーティリティオブジェクト。 */
object LayoutSanitizer {
    /**
     * 数式にInfinityが含まれているかチェックします。
     * @param formula チェックする数式文字列
     * @return Infinityを含む場合はtrue
     */
    private fun isInvalidFormula(formula: String?): Boolean {
        return formula?.contains("Infinity") == true
    }

    /**
     * ControlDataが有効な値を持っているか判定します。
     * @param controlData 検証するデータ
     * @return 有効な場合はtrue
     */
    private fun isSaneData(controlData: ControlData): Boolean {
        if (controlData.getWidth() == 0f || controlData.getHeight() == 0f) return false
        if (isInvalidFormula(controlData.dynamicX) || isInvalidFormula(controlData.dynamicY)) return false
        return true
    }

    /**
     * データエントリからControlDataを取得します。
     * @param dataEntry ControlDataまたはControlDrawerData
     * @return 抽出されたControlData
     */
    private fun getControlData(dataEntry: Any): ControlData {
        return when (dataEntry) {
            is ControlData -> dataEntry
            is ControlDrawerData -> dataEntry.properties
            else -> throw RuntimeException("Encountered wrong type during ControlData sanitization")
        }
    }

    /**
     * リスト内の無効なデータを除去します。
     * @param controlDataList 検証するデータのリスト
     * @return 変更があった場合はtrue
     */
    private fun sanitizeList(controlDataList: MutableList<*>): Boolean {
        var madeChanges = false
        val iterator = controlDataList.iterator()
        while (iterator.hasNext()) {
            val controlData = getControlData(iterator.next()!!)
            if (!isSaneData(controlData)) {
                madeChanges = true
                iterator.remove()
            }
        }
        return madeChanges
    }

    /**
     * CustomControls全体を検証し、無効なエントリを除去します。
     * @param controls 検証するコントロールレイアウト
     * @return 変更があった場合はtrue
     */
    fun sanitizeLayout(controls: CustomControls): Boolean {
        var madeChanges = sanitizeList(controls.mControlDataList!!)
        if (sanitizeList(controls.mDrawerDataList!!)) madeChanges = true
        if (sanitizeList(controls.mJoystickDataList!!)) madeChanges = true
        return madeChanges
    }
}
