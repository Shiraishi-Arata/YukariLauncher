package net.kdt.pojavlaunch.progresskeeper;

import static net.kdt.pojavlaunch.Tools.BYTE_TO_MB;

import com.arata.yukarilauncher.utils.YLTools;
import com.arata.yukarilauncher.utils.file.FileTools;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.tasks.SpeedCalculator;

/**
 * ダウンロード進行状況をProgressKeeperに転送するシンプルなラッパークラス。
 */
public class DownloaderProgressWrapper implements Tools.DownloaderFeedback {
    private final SpeedCalculator mSpeedCalculator = new SpeedCalculator(128);
    private final int mProgressString;
    private final String mProgressRecord;
    private long progressUpdateTime = 0;

    /**
     * ダウンロード進行状況をProgressKeeperに送信するラッパーを作成します。
     * @param progressString 進捗レポートで使用される文字列リソースID
     * @param progressRecord ProgressKeeperのレコードキー
     */
    public DownloaderProgressWrapper(int progressString, String progressRecord) {
        this.mProgressString = progressString;
        this.mProgressRecord = progressRecord;
    }

    /**
     * 進行状況を更新します。150ms未満の間隔での更新はスキップされます。
     */
    @Override
    public void updateProgress(long curr, long max) {
        long currentTime = YLTools.getCurrentTimeMillis();
        if (currentTime - progressUpdateTime < 150) return;
        progressUpdateTime = currentTime;

        Object[] va;
        va = new Object[3];
        va[0] = curr / BYTE_TO_MB;
        va[1] = max / BYTE_TO_MB;
        va[2] = FileTools.formatFileSize(mSpeedCalculator.feed(curr));
        ProgressKeeper.submitProgress(mProgressRecord, (int) Math.max((float) curr / max * 100, 0), mProgressString, va);
    }
}
