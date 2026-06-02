package com.arata.yukarilauncher.task

/** ダウンロード速度を計算するクラス。移動平均を使用する。 */
class SpeedCalculator(averageDepth: Int = 64) {

    /** 前回のミリ秒 */
    private var mLastMillis: Long = 0
    /** 前回のバイト数 */
    private var mLastBytes: Long = 0
    /** リングバッファインデックス */
    private var mIndex: Int = 0
    /** 過去の入力値リングバッファ */
    private val mPreviousInputs = LongArray(averageDepth)
    /** 合計値 */
    private var mSum: Long = 0

    /** 移動平均に速度を追加する。 @param speed 速度 @return 平均速度 */
    private fun addToAverage(speed: Long): Long {
        mSum -= mPreviousInputs[mIndex]
        mSum += speed
        mPreviousInputs[mIndex] = speed
        mIndex++
        if (mIndex == mPreviousInputs.size) mIndex = 0
        return mSum / mPreviousInputs.size
    }

    /** 現在の累積バイト数を入力し、速度を計算する。 @param bytes 累積バイト数 @return 平均速度（バイト/秒） */
    fun feed(bytes: Long): Long {
        val millis = System.currentTimeMillis()
        val deltaBytes = bytes - mLastBytes
        val deltaMillis = millis - mLastMillis
        mLastBytes = bytes
        mLastMillis = millis

        if (deltaMillis <= 0) return 0
        var speed = deltaBytes * 1000L / deltaMillis
        speed = minOf(speed, Long.MAX_VALUE / 2)

        return addToAverage(speed)
    }
}