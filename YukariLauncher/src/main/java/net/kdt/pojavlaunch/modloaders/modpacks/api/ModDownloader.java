package net.kdt.pojavlaunch.modloaders.modpacks.api;

import androidx.annotation.Nullable;

import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.setting.AllSettings;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ModDownloader {
    private static final ThreadLocal<byte[]> sThreadLocalBuffer = new ThreadLocal<>();
    private final ThreadPoolExecutor mDownloadPool;
    private final AtomicBoolean mTerminator = new AtomicBoolean(false);
    private final AtomicInteger mDownloadProgress = new AtomicInteger(0);
    private final AtomicLong mDownloadedSize = new AtomicLong(0);
    private final Object mExceptionSyncPoint = new Object();
    private final File mDestinationDirectory;
    private final boolean mUseFileCount;
    private IOException mFirstIOException;
    private long mTotalSize;

    /**
     * ModDownloaderを構築します。
     * @param destinationDirectory ダウンロード先ディレクトリ
     */
    public ModDownloader(File destinationDirectory) {
        this(destinationDirectory, false);
    }

    /**
     * ModDownloaderを構築します。
     * @param destinationDirectory ダウンロード先ディレクトリ
     * @param useFileCount ファイル数で進捗を計測する場合はtrue
     */
    public ModDownloader(File destinationDirectory, boolean useFileCount) {
        int maxThreads = AllSettings.getMaxDownloadThreads().getValue();
        this.mDownloadPool = new ThreadPoolExecutor(
                Math.max(1, (int) (maxThreads / 2)),
                maxThreads,
                100,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()
        );

        this.mDownloadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        this.mDestinationDirectory = destinationDirectory;
        this.mUseFileCount = useFileCount;
    }

    /**
     * ダウンロードタスクを送信します。
     * @param fileSize ファイルサイズ
     * @param relativePath 相対パス
     * @param downloadHash ダウンロードハッシュ（null可）
     * @param url ダウンロードURL
     */
    public void submitDownload(int fileSize, String relativePath, @Nullable String downloadHash, String... url) {
        if(mUseFileCount) mTotalSize += 1;
        else mTotalSize += fileSize;
        mDownloadPool.execute(new DownloadTask(url, new File(mDestinationDirectory, relativePath), downloadHash));
    }

    /**
     * ファイル情報プロバイダーを使用したダウンロードタスクを送信します。
     * @param infoProvider ファイル情報プロバイダー
     */
    public void submitDownload(FileInfoProvider infoProvider) {
        if(!mUseFileCount) throw new RuntimeException("This method can only be used in a file-counting ModDownloader");
        mTotalSize += 1;
        mDownloadPool.execute(new FileInfoQueryTask(infoProvider));
    }

    /**
     * すべてのダウンロードが完了するのを待機します。
     * @param feedback 進捗フィードバック
     * @throws IOException ダウンロード中にエラーが発生した場合
     */
    public void awaitFinish(Tools.DownloaderFeedback feedback) throws IOException {
        awaitFinish(() -> feedback.updateProgress(mDownloadedSize.get(), mTotalSize));
    }

    /**
     * カスタムリスナーでダウンロード完了を待機します。
     * @param listener ダウンロード進捗リスナー
     * @throws IOException ダウンロード中にエラーが発生した場合
     */
    public void awaitFinish(DownloadProgressListener listener) throws IOException {
        awaitFinish(() -> listener.feedback(mDownloadProgress.get(), (int) mTotalSize, mDownloadedSize.get()));
    }

    /**
     * 内部的な完了待機処理を実行します。
     */
    private void awaitFinish(OnFileDownloadedListener listener) throws IOException {
        try {
            mDownloadPool.shutdown();
            while(!mDownloadPool.awaitTermination(20, TimeUnit.MILLISECONDS) && !mTerminator.get()) {
                listener.downloaded();
            }
            if(mTerminator.get()) {
                mDownloadPool.shutdownNow();
                synchronized (mExceptionSyncPoint) {
                    if(mFirstIOException == null) mExceptionSyncPoint.wait();
                    throw mFirstIOException;
                }
            }
        }catch (InterruptedException e) {
            Logging.e("ModDownloader", Tools.printToString(e));
        }
    }

    /**
     * スレッドローカルバッファを取得します。
     */
    private static byte[] getThreadLocalBuffer() {
        byte[] buffer = sThreadLocalBuffer.get();
        if(buffer != null) return buffer;
        buffer = new byte[8192];
        sThreadLocalBuffer.set(buffer);
        return buffer;
    }

    /**
     * ダウンロード失敗を記録し、他のスレッドに通知します。
     */
    private void downloadFailed(IOException exception) {
        mTerminator.set(true);
        synchronized (mExceptionSyncPoint) {
            if(mFirstIOException == null) {
                mFirstIOException = exception;
                mExceptionSyncPoint.notify();
            }
        }
    }

    /**
     * ファイル情報を取得してからダウンロードを実行するタスク
     */
    class FileInfoQueryTask implements Runnable {
        private final FileInfoProvider mFileInfoProvider;
        public FileInfoQueryTask(FileInfoProvider fileInfoProvider) {
            this.mFileInfoProvider = fileInfoProvider;
        }
        @Override
        public void run() {
            try {
                FileInfo fileInfo = mFileInfoProvider.getFileInfo();
                if(fileInfo == null) return;
                new DownloadTask(new String[]{fileInfo.url},
                        new File(mDestinationDirectory, fileInfo.relativePath), fileInfo.sha1).run();
            }catch (IOException e) {
                downloadFailed(e);
            }
        }
    }

    /**
     * 実際のダウンロードを実行するタスク
     */
    class DownloadTask implements Runnable, Tools.DownloaderFeedback {
        private final String[] mDownloadUrls;
        private final File mDestination;
        private final String mSha1;
        private long last = 0L;

        public DownloadTask(String[] downloadurls,
                            File downloadDestination, String downloadHash) {
            this.mDownloadUrls = downloadurls;
            this.mDestination = downloadDestination;
            this.mSha1 = downloadHash;
        }

        @Override
        public void run() {
            for(String sourceUrl : mDownloadUrls) {
                try {
                    DownloadUtils.ensureSha1(mDestination, mSha1, (Callable<Void>) () -> {
                        IOException exception = tryDownload(sourceUrl);
                        if(exception != null) {
                            throw exception;
                        }
                        return null;
                    });

                } catch (IOException e) {
                    downloadFailed(e);
                }
            }
        }

        /**
         * 指定されたURLから5回までダウンロードを試行します。
         */
        private IOException tryDownload(String sourceUrl) throws InterruptedIOException {
            IOException exception = null;
            for (int i = 0; i < 5; i++) {
                try {
                    DownloadUtils.downloadFileMonitored(sourceUrl, mDestination, getThreadLocalBuffer(), this);
                    if (mUseFileCount) mDownloadProgress.addAndGet(1);
                    return null;
                } catch (InterruptedIOException e) {
                    throw e;
                } catch (IOException e) {
                    Logging.e("ModDownloader", Tools.printToString(e));
                    exception = e;
                }
                mDownloadedSize.addAndGet(-last);
                last = 0;
            }
            return exception;
        }

        /**
         * ダウンロード進捗を更新します。
         */
        @Override
        public void updateProgress(long curr, long max) {
            long size = curr - last;
            mDownloadedSize.addAndGet(size);
            last = curr;
        }
    }

    /**
     * ダウンロードするファイルの情報
     */
    public static class FileInfo {
        public final String url;
        public final String relativePath;
        public final String sha1;

        public FileInfo(String url, String relativePath, @Nullable String sha1) {
            this.url = url;
            this.relativePath = relativePath;
            this.sha1 = sha1;
        }
    }

    /**
     * ファイル情報を提供するインターフェース
     */
    public interface FileInfoProvider {
        FileInfo getFileInfo() throws IOException;
    }

    /**
     * ファイルダウンロード時の内部リスナー
     */
    private interface OnFileDownloadedListener {
        void downloaded();
    }

    /**
     * ダウンロード進捗リスナー
     */
    public interface DownloadProgressListener {
        void feedback(int downloadedCount, int totalCount, long downloadedSize);
    }
}
