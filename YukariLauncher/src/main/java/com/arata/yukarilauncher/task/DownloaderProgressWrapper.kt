package com.arata.yukarilauncher.task

import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.task.SpeedCalculator

/** ダウンロード進捗をラップしてProgressKeeperに通知するクラス。 */
class DownloaderProgressWrapper(
    private val mProgressString: Int,
    private val mProgressRecord: String
) : Tools.DownloaderFeedback {

    /** 速度計算機 */
    private val mSpeedCalculator = SpeedCalculator(128)
    /** 前回の進捗更新時刻 */
    private var progressUpdateTime: Long = 0

    /** 進捗を更新する。 @param curr 現在のバイト数 @param max 最大バイト数 */
    override fun updateProgress(curr: Long, max: Long) {
        val currentTime = YLTools.getCurrentTimeMillis()
        if (currentTime - progressUpdateTime < 150) return
        progressUpdateTime = currentTime

        val va = arrayOf<Any>(
            curr / Tools.BYTE_TO_MB,
            max / Tools.BYTE_TO_MB,
            FileTools.formatFileSize(mSpeedCalculator.feed(curr))
        )
        ProgressKeeper.submitProgress(mProgressRecord, Math.max((curr.toFloat() / max * 100).toInt(), 0), mProgressString, *va)
    }
}
