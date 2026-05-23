package net.kdt.pojavlaunch.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kdt.mcgui.ProgressLayout;
import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome;
import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.task.Task;
import com.arata.yukarilauncher.utils.path.PathManager;

import net.kdt.pojavlaunch.JAssetInfo;
import net.kdt.pojavlaunch.JAssets;
import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.mirrors.DownloadMirror;
import net.kdt.pojavlaunch.mirrors.MirrorTamperedException;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.value.DependentLibrary;
import net.kdt.pojavlaunch.value.MinecraftClientInfo;
import net.kdt.pojavlaunch.value.MinecraftLibraryArtifact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * マインクラフトのゲームファイルをダウンロードするクラス。
 * バージョンメタデータ、アセット、ライブラリ、ネイティブファイルを並列ダウンロードします。
 */
public class MinecraftDownloader {
    private static final double ONE_MEGABYTE = (1024d * 1024d);
    public static final String MINECRAFT_RES = "https://resources.download.minecraft.net/";
    private static final String MAVEN_CENTRAL_REPO1 = "https://repo1.maven.org/maven2/";
    private AtomicReference<Exception> mDownloaderThreadException;
    private ArrayList<DownloaderTask> mScheduledDownloadTasks;
    private ArrayList<File> mDeclaredNatives;
    private AtomicLong mProcessedFileCounter;
    private AtomicLong mProcessedSizeCounter;
    private AtomicLong mInternetUsageCounter;
    private long mTotalFileCount;
    private long mTotalSize;
    private File mSourceJarFile;
    private File mTargetJarFile;
    private boolean mUseFileCounter;

    private static final ThreadLocal<byte[]> sThreadLocalDownloadBuffer = new ThreadLocal<>();

    /**
     * ゲームバージョンのダウンロードプロセスをグローバルエグゼキュータで開始します。
     * @param version バージョンリストからのJMinecraftVersionList.Version（利用可能な場合）
     * @param realVersion バージョンID
     * @param listener ダウンロードステータスリスナー
     */
    public void start(@Nullable JMinecraftVersionList.Version version,
                      @NonNull String realVersion,
                      @NonNull AsyncMinecraftDownloader.DoneListener listener) {
        Task.runTask(() -> {
            downloadGame(version, realVersion);
            listener.onDownloadDone();
            return null;
        }).onThrowable(listener::onDownloadFailed)
                .finallyTask(() -> ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT))
                .execute();
    }

    /**
     * ゲームバージョンをダウンロードします。
     * @param verInfo バージョンリストからのバージョン情報
     * @param versionName バージョンID
     */
    private void downloadGame(JMinecraftVersionList.Version verInfo, String versionName) throws Exception {
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0, R.string.newdl_starting);
        SpeedCalculator speedCalculator = new SpeedCalculator();

        mTargetJarFile = createGameJarPath(versionName);
        mScheduledDownloadTasks = new ArrayList<>();
        mDeclaredNatives = new ArrayList<>();
        mProcessedFileCounter = new AtomicLong(0);
        mProcessedSizeCounter = new AtomicLong(0);
        mInternetUsageCounter = new AtomicLong(0);
        mDownloaderThreadException = new AtomicReference<>(null);
        mUseFileCounter = false;

        downloadAndProcessMetadata(verInfo, versionName);

        ThreadPoolExecutor downloaderPool = createThreadPoolExecutor();

        for(DownloaderTask scheduledTask : mScheduledDownloadTasks) downloaderPool.execute(scheduledTask);
        downloaderPool.shutdown();

        try {
            while (mDownloaderThreadException.get() == null &&
                    !downloaderPool.awaitTermination(33, TimeUnit.MILLISECONDS)) {
                double speed = speedCalculator.feed(mInternetUsageCounter.get()) / ONE_MEGABYTE;
                if(mUseFileCounter) reportProgressFileCounter(speed);
                else reportProgressSizeCounter(speed);
            }
            Exception thrownException = mDownloaderThreadException.get();
            if(thrownException != null) {
                throw thrownException;
            } else {
                ensureJarFileCopy();
                extractNatives(versionName);
            }
        }catch (InterruptedException e) {
            downloaderPool.shutdownNow();
        }
    }

    /**
     * ダウンロードスレッドプールを作成します。
     */
    @NonNull
    private ThreadPoolExecutor createThreadPoolExecutor() {
        int maxThreads = AllSettings.getMaxDownloadThreads().getValue();
        if (mScheduledDownloadTasks.size() <= maxThreads) {
            maxThreads = mScheduledDownloadTasks.size();
        }
        return new ThreadPoolExecutor(
                Math.max(1, (int) (maxThreads / 2)),
                maxThreads,
                500,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(mScheduledDownloadTasks.size(), false)
        );
    }

    /**
     * ファイルカウンタベースで進捗状況を報告します。
     */
    private void reportProgressFileCounter(double speed) {
        long dlFileCounter = mProcessedFileCounter.get();
        int progress = (int)((dlFileCounter * 100L) / mTotalFileCount);
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, progress,
                R.string.newdl_downloading_game_files, dlFileCounter,
                mTotalFileCount, speed);
    }

    /**
     * サイズカウンタベースで進捗状況を報告します。
     */
    private void reportProgressSizeCounter(double speed) {
        long dlFileSize = mProcessedSizeCounter.get();
        double dlSizeMegabytes = (double) dlFileSize / ONE_MEGABYTE;
        double dlTotalMegabytes = (double) mTotalSize / ONE_MEGABYTE;
        int progress = (int)((dlFileSize * 100L) / mTotalSize);
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, progress,
                R.string.newdl_downloading_game_files_size, dlSizeMegabytes, dlTotalMegabytes, speed);
    }

    /**
     * バージョンJSONファイルのパスを作成します。
     */
    private File createGameJsonPath(String versionId) {
        return new File(ProfilePathHome.getVersionsHome(), versionId + File.separator + versionId + ".json");
    }

    /**
     * バージョンJARファイルのパスを作成します。
     */
    private File createGameJarPath(String versionId) {
        return new File(ProfilePathHome.getVersionsHome(), versionId + File.separator + versionId + ".jar");
    }

    /**
     * 必要に応じて、バージョンフォルダにクライアントJARのコピーがあることを確認します。
     */
    private void ensureJarFileCopy() throws IOException {
        if(mSourceJarFile == null) return;
        if(mSourceJarFile.equals(mTargetJarFile)) return;
        if(mTargetJarFile.exists()) return;
        FileUtils.ensureParentDirectory(mTargetJarFile);
        Logging.i("NewMCDownloader", "Copying " + mSourceJarFile.getName() + " to "+mTargetJarFile.getAbsolutePath());
        org.apache.commons.io.FileUtils.copyFile(mSourceJarFile, mTargetJarFile, false);
    }

    /**
     * 宣言されたネイティブライブラリを抽出します。
     */
    private void extractNatives(String versionName) throws IOException {
        if(mDeclaredNatives.isEmpty()) return;
        int totalCount = mDeclaredNatives.size();

        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                R.string.newdl_extracting_native_libraries, 0, totalCount);

        File targetDirectory = new File(PathManager.DIR_CACHE, "natives/"+versionName);
        FileUtils.ensureDirectory(targetDirectory);
        NativesExtractor nativesExtractor = new NativesExtractor(targetDirectory);
        int extractedCount = 0;
        for(File source : mDeclaredNatives) {
            nativesExtractor.extractFromAar(source);
            extractedCount++;
            ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, extractedCount * 100 / totalCount,
                    R.string.newdl_extracting_native_libraries, extractedCount, totalCount);
        }
    }

    /**
     * ゲームバージョンJSONファイルをダウンロードします（未ダウンロードまたはSHA1不一致の場合）。
     */
    private File downloadGameJson(JMinecraftVersionList.Version verInfo) throws IOException, MirrorTamperedException {
        File targetFile = createGameJsonPath(verInfo.id);
        if(verInfo.sha1 == null && targetFile.canRead() && targetFile.isFile())
            return targetFile;
        FileUtils.ensureParentDirectory(targetFile);
        try {
            DownloadUtils.ensureSha1(targetFile, AllSettings.getVerifyManifest().getValue() ? verInfo.sha1 : null, () -> {
                ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                        R.string.newdl_downloading_metadata, targetFile.getName());
                DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, verInfo.url, targetFile);
                return null;
            });
        }catch (DownloadUtils.SHA1VerificationException e) {
            if(DownloadMirror.isMirrored()) throw new MirrorTamperedException();
            else throw e;
        }
        return targetFile;
    }

    /**
     * アセットインデックスをダウンロードします。
     */
    private JAssets downloadAssetsIndex(JMinecraftVersionList.Version verInfo) throws IOException{
        JMinecraftVersionList.AssetIndex assetIndex = verInfo.assetIndex;
        if(assetIndex == null || verInfo.assets == null) return null;
        File targetFile = new File(ProfilePathHome.getAssetsHome(), "indexes"+ File.separator + verInfo.assets + ".json");
        FileUtils.ensureParentDirectory(targetFile);
        DownloadUtils.ensureSha1(targetFile, assetIndex.sha1, ()-> {
            ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                    R.string.newdl_downloading_metadata, targetFile.getName());
            DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, assetIndex.url, targetFile);
            return null;
        });
        return Tools.GLOBAL_GSON.fromJson(Tools.read(targetFile), JAssets.class);
    }

    /**
     * バージョン情報からクライアントJAR情報を取得します。
     */
    private MinecraftClientInfo getClientInfo(JMinecraftVersionList.Version verInfo) {
        Map<String, MinecraftClientInfo> downloads = verInfo.downloads;
        if(downloads == null) return null;
        return downloads.get("client");
    }

    /**
     * バージョンのメタデータをダウンロード（必要に応じて）し、処理します。
     * このバージョンが必要とするすべてのダウンロードをスケジュールします。
     */
    private void downloadAndProcessMetadata(JMinecraftVersionList.Version verInfo, String versionName) throws IOException, MirrorTamperedException {
        File versionJsonFile;
        if(verInfo != null) versionJsonFile = downloadGameJson(verInfo);
        else versionJsonFile = createGameJsonPath(versionName);
        if(versionJsonFile.canRead())  {
            verInfo = Tools.GLOBAL_GSON.fromJson(Tools.read(versionJsonFile), JMinecraftVersionList.Version.class);
        } else {
            throw new IOException("Unable to read Version JSON for version " + versionName);
        }

        JAssets assets = downloadAssetsIndex(verInfo);
        if(assets != null) scheduleAssetDownloads(assets);

        MinecraftClientInfo minecraftClientInfo = getClientInfo(verInfo);
        if(minecraftClientInfo != null) scheduleGameJarDownload(minecraftClientInfo, versionName);

        if(verInfo.libraries != null) scheduleLibraryDownloads(verInfo.libraries);

        if(Tools.isValidString(verInfo.inheritsFrom)) {
            JMinecraftVersionList.Version inheritedVersion = AsyncMinecraftDownloader.getListedVersion(verInfo.inheritsFrom);
            downloadAndProcessMetadata(inheritedVersion, verInfo.inheritsFrom);
        }
    }

    /**
     * ダウンロードリストの容量を拡張します。
     */
    private void growDownloadList(int addedElementCount) {
        mScheduledDownloadTasks.ensureCapacity(mScheduledDownloadTasks.size() + addedElementCount);
    }

    /**
     * ダウンロードタスクをスケジュールに追加します。
     */
    private void scheduleDownload(File targetFile, int downloadClass, String url, String sha1,
                                  long size, boolean skipIfFailed) throws IOException {
        FileUtils.ensureParentDirectory(targetFile);
        mTotalFileCount++;
        if(size <= 0 && !mUseFileCounter) {
            try {
                size = DownloadMirror.getContentLengthMirrored(downloadClass, url);
            } catch (Exception e) {
                size = -1;
            }
        }
        if(size < 0) {
            size = 0;
            mUseFileCounter = true;
            Logging.i("MinecraftDownloader", "Failed to determine size of "+targetFile.getName()+", switching to file counter");
        }else {
            mTotalSize += size;
        }
        mScheduledDownloadTasks.add(
                new DownloaderTask(targetFile, downloadClass, url, sha1, size, skipIfFailed)
        );
    }

    /**
     * AARライブラリ（ネイティブを含む）のダウンロードをスケジュールします。
     */
    private void scheduleNativeLibraryDownload(String baseRepository, DependentLibrary dependentLibrary) throws IOException {
        String libArtifactPath = Tools.artifactToPath(dependentLibrary);
        if (libArtifactPath == null) return;
        String path = FileUtils.removeExtension(libArtifactPath) + ".aar";
        String downloadUrl = baseRepository + path;
        File targetPath = new File(ProfilePathHome.getLibrariesHome(), path);
        mDeclaredNatives.add(targetPath);
        scheduleDownload(targetPath, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, downloadUrl, null, 0, true);
    }

    /**
     * 依存ライブラリのダウンロードをスケジュールします。
     */
    private void scheduleLibraryDownloads(DependentLibrary[] dependentLibraries) throws IOException {
        Tools.preProcessLibraries(dependentLibraries);
        growDownloadList(dependentLibraries.length);
        for(DependentLibrary dependentLibrary : dependentLibraries) {
            if(dependentLibrary.name.startsWith("org.lwjgl")) continue;
            if(dependentLibrary.name.startsWith("net.java.dev.jna:jna:")) {
                scheduleNativeLibraryDownload(MAVEN_CENTRAL_REPO1, dependentLibrary);
            }

            String libArtifactPath = Tools.artifactToPath(dependentLibrary);
            if (libArtifactPath == null) continue;

            String sha1 = null, url = null;
            long size = 0;
            boolean skipIfFailed = false;
            if(dependentLibrary.downloads != null) {
                if(dependentLibrary.downloads.artifact != null) {
                    MinecraftLibraryArtifact artifact = dependentLibrary.downloads.artifact;
                    sha1 = artifact.sha1;
                    url = artifact.url;
                    size = artifact.size;
                } else {
                    Logging.i("NewMCDownloader", "Skipped library " + dependentLibrary.name + " due to lack of artifact");
                    continue;
                }
            }
            if(url == null) {
                url = (dependentLibrary.url == null
                        ? "https://libraries.minecraft.net/"
                        : dependentLibrary.url.replace("http://","https://")) + libArtifactPath;
                skipIfFailed = true;
            }
            if(!AllSettings.getCheckLibraries().getValue()) sha1 = null;
            scheduleDownload(new File(ProfilePathHome.getLibrariesHome(), libArtifactPath),
                    DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                    url, sha1, size, skipIfFailed
            );
        }
    }

    /**
     * アセットのダウンロードをスケジュールします。
     */
    private void scheduleAssetDownloads(JAssets assets) throws IOException {
        Map<String, JAssetInfo> assetObjects = assets.objects;
        if(assetObjects == null) return;
        Set<String> assetNames = assetObjects.keySet();
        growDownloadList(assetNames.size());
        for(String asset : assetNames) {
            JAssetInfo assetInfo = assetObjects.get(asset);
            if(assetInfo == null) continue;
            File targetFile;
            String hashedPath = assetInfo.hash.substring(0, 2) + File.separator + assetInfo.hash;
            String basePath = assets.mapToResources ? ProfilePathHome.getResourcesHome() : ProfilePathHome.getAssetsHome();
            if(assets.virtual || assets.mapToResources) {
                targetFile = new File(basePath, asset);
            } else {
                targetFile = new File(basePath, "objects" + File.separator + hashedPath);
            }
            String sha1 = AllSettings.getCheckLibraries().getValue() ? assetInfo.hash : null;
            scheduleDownload(targetFile,
                    DownloadMirror.DOWNLOAD_CLASS_ASSETS,
                    MINECRAFT_RES + hashedPath,
                    sha1,
                    assetInfo.size,
                    false);
        }
    }

    /**
     * ゲームJARのダウンロードをスケジュールします。
     */
    private void scheduleGameJarDownload(MinecraftClientInfo minecraftClientInfo, String versionName) throws IOException {
        File clientJar = createGameJarPath(versionName);
        String clientSha1 = AllSettings.getCheckLibraries().getValue() ? minecraftClientInfo.sha1 : null;
        growDownloadList(1);
        scheduleDownload(clientJar,
                DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                minecraftClientInfo.url,
                clientSha1,
                minecraftClientInfo.size,
                false
        );
        mSourceJarFile = clientJar;
    }

    /**
     * スレッドローカルなダウンロードバッファを取得します。
     */
    private static byte[] getLocalBuffer() {
        byte[] tlb = sThreadLocalDownloadBuffer.get();
        if(tlb != null) return tlb;
        tlb = new byte[32768];
        sThreadLocalDownloadBuffer.set(tlb);
        return tlb;
    }

    /**
     * 個別のファイルのダウンロードを実行する内部タスククラス。
     */
    private final class DownloaderTask implements Runnable, Tools.DownloaderFeedback {
        private final File mTargetPath;
        private final String mTargetUrl;
        private String mTargetSha1;
        private final int mDownloadClass;
        private final boolean mSkipIfFailed;
        private long mLastCurr;
        private final long mDownloadSize;

        /**
         * ダウンロードタスクを初期化します。
         * @param targetPath ダウンロード先のファイルパス
         * @param downloadClass ダウンロードクラス（ミラー用）
         * @param targetUrl ダウンロードURL
         * @param targetSha1 期待されるSHA1ハッシュ
         * @param downloadSize ダウンロードサイズ
         * @param skipIfFailed 失敗時にスキップするかどうか
         */
        DownloaderTask(File targetPath, int downloadClass, String targetUrl, String targetSha1,
                       long downloadSize, boolean skipIfFailed) {
            this.mTargetPath = targetPath;
            this.mTargetUrl = targetUrl;
            this.mTargetSha1 = targetSha1;
            this.mDownloadClass = downloadClass;
            this.mDownloadSize = downloadSize;
            this.mSkipIfFailed = skipIfFailed;
        }

        /**
         * SHA1ハッシュファイルをダウンロードします。
         */
        private String downloadSha1() throws IOException {
            String downloadedHash = DownloadMirror.downloadStringMirrored(
                    mDownloadClass, mTargetUrl + ".sha1"
            );
            if(!Tools.isValidString(downloadedHash)) return null;
            downloadedHash = downloadedHash.trim();
            if(downloadedHash.length() != 40) return null;
            return downloadedHash;
        }

        /**
         * MavenリポジトリからSHA1ハッシュを取得しようとします。
         */
        private void tryGetLibrarySha1() {
            String resultHash = null;
            try {
                resultHash = downloadSha1();
                mInternetUsageCounter.getAndAdd(40);
            }catch (IOException e) {
                Logging.i("MinecraftDownloader", "Failed to download hash", e);
            }
            if(resultHash != null) {
                Logging.i("MinecraftDownloader", "Got hash: "+resultHash+ " for "+FileUtils.getFileName(mTargetUrl));
                mTargetSha1 = resultHash;
            }
        }

        /**
         * ダウンロードタスクを実行します。
         */
        @Override
        public void run() {
            try {
                runCatching();
            }catch (Exception e) {
                mDownloaderThreadException.set(e);
            }
        }

        /**
         * 例外をキャッチしながらダウンロード処理を実行します。
         */
        private void runCatching() throws Exception {
            if(mDownloadClass == DownloadMirror.DOWNLOAD_CLASS_LIBRARIES && !Tools.isValidString(mTargetSha1)) {
                tryGetLibrarySha1();
            }
            if(Tools.isValidString(mTargetSha1)) {
                verifyFileSha1();
            }else {
                mTargetSha1 = null;
                if(mTargetPath.exists()) finishWithoutDownloading();
                else downloadFile();
            }
        }

        /**
         * ファイルのSHA1ハッシュを検証します。
         */
        private void verifyFileSha1() throws Exception {
            if(mTargetPath.isFile() && mTargetPath.canRead() && Tools.compareSHA1(mTargetPath, mTargetSha1)) {
                finishWithoutDownloading();
            } else {
                downloadFile();
            }
        }

        /**
         * ファイルをダウンロードします。
         */
        private void downloadFile() throws Exception {
            try {
                DownloadUtils.ensureSha1(mTargetPath, mTargetSha1, () -> {
                    DownloadMirror.downloadFileMirrored(mDownloadClass, mTargetUrl, mTargetPath,
                            getLocalBuffer(), this);
                    return null;
                });
            }catch (Exception e) {
                if(!mSkipIfFailed) throw e;
            }
            mProcessedFileCounter.incrementAndGet();
        }

        /**
         * ダウンロードせずにタスクを完了します（既存ファイルを利用）。
         */
        private void finishWithoutDownloading() {
            mProcessedFileCounter.incrementAndGet();
            mProcessedSizeCounter.addAndGet(mDownloadSize);
        }

        /**
         * ダウンロード進捗を更新します。
         */
        @Override
        public void updateProgress(long curr, long max) {
            long delta = curr - mLastCurr;
            mProcessedSizeCounter.addAndGet(delta);
            mInternetUsageCounter.addAndGet(delta);
            mLastCurr = curr;
        }
    }
}
