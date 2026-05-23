package net.kdt.pojavlaunch.memory;

import net.kdt.pojavlaunch.Architecture;

/**
 * プロセスのメモリマップから最大の空き領域（ホール）を見つけるためのコールバック実装。
 */
public class MemoryHoleFinder implements SelfMapsParser.Callback {
    private long mPreviousEnd = 0;
    private long mLargestHole = -1;
    private final long mAddressingLimit = Architecture.getAddressSpaceLimit();

    /**
     * 各行のメモリ領域を処理し、最大の空き領域を追跡します。
     */
    @Override
    public boolean process(long begin, long end, String wholeLine) {
        if(begin >= mAddressingLimit) begin = mAddressingLimit;
        long holeSize = begin - mPreviousEnd;
        if(mLargestHole < holeSize) mLargestHole = holeSize;
        if(begin == mAddressingLimit) return false;
        mPreviousEnd = end;
        return true;
    }

    /**
     * @return 見つかった最大の連続空き領域（バイト単位）。見つからなければ-1。
     */
    public long getLargestHole() {
        return mLargestHole;
    }
}
