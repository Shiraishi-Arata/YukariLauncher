package com.arata.yukarilauncher.utils.platform

import com.arata.yukarilauncher.utils.platform.Architecture

/** /proc/self/maps を解析してメモリホールの最大サイズを特定するクラス。 */
class MemoryHoleFinder : SelfMapsParser.Callback {
    /** 前回のエンドアドレス。 */
    private var mPreviousEnd: Long = 0
    /** 見つかった最大のホールサイズ。 */
    private var mLargestHole: Long = -1
    /** アーキテクチャに基づくアドレッシング上限。 */
    private val mAddressingLimit = Architecture.getAddressSpaceLimit()

    /**
     * マップエントリを処理し、ホールサイズを計算する。
     * @param begin 開始アドレス
     * @param end 終了アドレス
     * @param wholeLine マップ行全体
     * @return 処理を続行する場合は true
     */
    override fun process(begin: Long, end: Long, wholeLine: String): Boolean {
        var beginVar = begin
        if (beginVar >= mAddressingLimit) beginVar = mAddressingLimit
        val holeSize = beginVar - mPreviousEnd
        if (mLargestHole < holeSize) mLargestHole = holeSize
        if (beginVar == mAddressingLimit) return false
        mPreviousEnd = end
        return true
    }

    /** 見つかった最大のメモリホールサイズを返す。 @return 最大ホールサイズ */
    fun getLargestHole(): Long = mLargestHole
}