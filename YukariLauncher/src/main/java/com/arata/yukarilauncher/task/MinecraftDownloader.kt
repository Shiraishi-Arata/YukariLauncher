package com.arata.yukarilauncher.task

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.utils.path.PathManager
import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.value.JAssetInfo
import com.arata.yukarilauncher.value.JAssets
import com.arata.yukarilauncher.value.JMinecraftVersionList
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.http.DownloadMirror
import com.arata.yukarilauncher.utils.http.MirrorTamperedException
import com.arata.yukarilauncher.utils.http.DownloadUtils
import com.arata.yukarilauncher.utils.file.FileUtils
import com.arata.yukarilauncher.value.DependentLibrary
import com.arata.yukarilauncher.value.MinecraftClientInfo
import com.arata.yukarilauncher.value.MinecraftLibraryArtifact
import java.io.File
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/** Minecraftゲームファイルのダウンロードを管理するクラス。 */
class MinecraftDownloader {

    /** ダウンロードスレッドの例外 */
    private var mDownloaderThreadException: AtomicReference<Exception>? = null
    /** スケジュールされたダウンロードタスクリスト */
    private var mScheduledDownloadTasks: ArrayList<DownloaderTask>? = null
    /** 宣言されたネイティブライブラリ */
    private var mDeclaredNatives: ArrayList<File>? = null
    /** 処理済みファイルカウンター */
    private var mProcessedFileCounter: AtomicLong? = null
    /** 処理済みサイズカウンター */
    private var mProcessedSizeCounter: AtomicLong? = null
    /** インターネット使用量カウンター */
    private var mInternetUsageCounter: AtomicLong? = null
    /** 総ファイル数 */
    private var mTotalFileCount: Long = 0
    /** 総サイズ */
    private var mTotalSize: Long = 0
    /** ソースJarファイル */
    private var mSourceJarFile: File? = null
    /** ターゲットJarファイル */
    private var mTargetJarFile: File? = null
    /** ファイルカウンターモードを使用するか */
    private var mUseFileCounter: Boolean = false

    /** ダウンロードを開始する。 @param version バージョン情報 @param realVersion 実際のバージョン文字列 @param listener 完了リスナー */
    fun start(
        @Nullable version: JMinecraftVersionList.Version?,
        @NonNull realVersion: String,
        @NonNull listener: AsyncMinecraftDownloader.DoneListener
    ) {
        @Suppress("RETURN_TYPE_MISMATCH")
        Task.runTask<Void> {
            downloadGame(version, realVersion)
            listener.onDownloadDone()
            null
        }.onThrowable { listener.onDownloadFailed(it) }
            .finallyTask { ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT) }
            .execute()
    }

    /** ゲームをダウンロードする。 @param verInfo バージョン情報 @param versionName バージョン名 */
    @Throws(Exception::class)
    private fun downloadGame(verInfo: JMinecraftVersionList.Version?, versionName: String) {
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0, R.string.newdl_starting)
        val speedCalculator = SpeedCalculator()

        mTargetJarFile = createGameJarPath(versionName)
        mScheduledDownloadTasks = ArrayList()
        mDeclaredNatives = ArrayList()
        mProcessedFileCounter = AtomicLong(0)
        mProcessedSizeCounter = AtomicLong(0)
        mInternetUsageCounter = AtomicLong(0)
        mDownloaderThreadException = AtomicReference(null)
        mUseFileCounter = false

        downloadAndProcessMetadata(verInfo, versionName)

        val downloaderPool = createThreadPoolExecutor()

        for (scheduledTask in mScheduledDownloadTasks!!) downloaderPool.execute(scheduledTask)
        downloaderPool.shutdown()

        try {
            while (mDownloaderThreadException!!.get() == null &&
                !downloaderPool.awaitTermination(33, TimeUnit.MILLISECONDS)
            ) {
                val speed = speedCalculator.feed(mInternetUsageCounter!!.get()) / ONE_MEGABYTE
                if (mUseFileCounter) reportProgressFileCounter(speed)
                else reportProgressSizeCounter(speed)
            }
            val thrownException = mDownloaderThreadException!!.get()
            if (thrownException != null) {
                throw thrownException
            } else {
                ensureJarFileCopy()
                extractNatives(versionName)
            }
        } catch (e: InterruptedException) {
            downloaderPool.shutdownNow()
        }
    }

    /** スレッドプールエグゼキュータを作成する。 @return スレッドプールエグゼキュータ */
    @NonNull
    private fun createThreadPoolExecutor(): ThreadPoolExecutor {
        val settingsMaxThreads = AllSettings.maxDownloadThreads.getValue()
        var maxThreads = settingsMaxThreads
        if (mScheduledDownloadTasks!!.size <= maxThreads) {
            maxThreads = mScheduledDownloadTasks!!.size
        }
        return ThreadPoolExecutor(
            Math.max(1, maxThreads / 2),
            maxThreads,
            500,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(mScheduledDownloadTasks!!.size, false)
        )
    }

    /** ファイルカウンターモードで進捗を報告する。 @param speed ダウンロード速度 */
    private fun reportProgressFileCounter(speed: Double) {
        val dlFileCounter = mProcessedFileCounter!!.get()
        val progress = (dlFileCounter * 100L / mTotalFileCount).toInt()
        ProgressLayout.setProgress(
            ProgressLayout.DOWNLOAD_MINECRAFT, progress,
            R.string.newdl_downloading_game_files, dlFileCounter,
            mTotalFileCount, speed
        )
    }

    /** サイズカウンターモードで進捗を報告する。 @param speed ダウンロード速度 */
    private fun reportProgressSizeCounter(speed: Double) {
        val dlFileSize = mProcessedSizeCounter!!.get()
        val dlSizeMegabytes = dlFileSize.toDouble() / ONE_MEGABYTE
        val dlTotalMegabytes = mTotalSize.toDouble() / ONE_MEGABYTE
        val progress = (dlFileSize * 100L / mTotalSize).toInt()
        ProgressLayout.setProgress(
            ProgressLayout.DOWNLOAD_MINECRAFT, progress,
            R.string.newdl_downloading_game_files_size, dlSizeMegabytes, dlTotalMegabytes, speed
        )
    }

    /** ゲームJSONファイルのパスを作成する。 @param versionId バージョンID @return JSONファイル */
    private fun createGameJsonPath(versionId: String): File =
        File(ProfilePathHome.getVersionsHome(), "$versionId${File.separator}$versionId.json")

    /** ゲームJarファイルのパスを作成する。 @param versionId バージョンID @return Jarファイル */
    private fun createGameJarPath(versionId: String): File =
        File(ProfilePathHome.getVersionsHome(), "$versionId${File.separator}$versionId.jar")

    /** Jarファイルのコピーを確保する。 */
    @Throws(IOException::class)
    private fun ensureJarFileCopy() {
        if (mSourceJarFile == null) return
        if (mSourceJarFile == mTargetJarFile) return
        if (mTargetJarFile!!.exists()) return
        FileUtils.ensureParentDirectory(mTargetJarFile!!)
        Logging.i("NewMCDownloader", "Copying ${mSourceJarFile!!.name} to ${mTargetJarFile!!.absolutePath}")
        org.apache.commons.io.FileUtils.copyFile(mSourceJarFile, mTargetJarFile, false)
    }

    /** ネイティブライブラリを展開する。 @param versionName バージョン名 */
    @Throws(IOException::class)
    private fun extractNatives(versionName: String) {
        if (mDeclaredNatives!!.isEmpty()) return
        val totalCount = mDeclaredNatives!!.size

        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0,
            R.string.newdl_extracting_native_libraries, 0, totalCount)

        val targetDirectory = File(PathManager.DIR_CACHE, "natives/$versionName")
        FileUtils.ensureDirectory(targetDirectory)
        val nativesExtractor = NativesExtractor(targetDirectory)
        var extractedCount = 0
        for (source in mDeclaredNatives!!) {
            nativesExtractor.extractFromAar(source)
            extractedCount++
            ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, extractedCount * 100 / totalCount,
                R.string.newdl_extracting_native_libraries, extractedCount, totalCount)
        }
    }

    /** ゲームJSONファイルをダウンロードする。 @param verInfo バージョン情報 @return JSONファイル */
    @Throws(IOException::class, MirrorTamperedException::class)
    private fun downloadGameJson(verInfo: JMinecraftVersionList.Version): File {
        val targetFile = createGameJsonPath(verInfo.id!!)
        if (verInfo.sha1 == null && targetFile.canRead() && targetFile.isFile)
            return targetFile
        FileUtils.ensureParentDirectory(targetFile)
        try {
                DownloadUtils.ensureSha1(targetFile, if (AllSettings.verifyManifest.getValue()) verInfo.sha1 else null) {
                ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                    R.string.newdl_downloading_metadata, targetFile.name)
                DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, verInfo.url ?: "", targetFile)
                null
            }
        } catch (e: DownloadUtils.SHA1VerificationException) {
            if (DownloadMirror.isMirrored()) throw MirrorTamperedException()
            else throw e
        }
        return targetFile
    }

    /** アセットインデックスをダウンロードする。 @param verInfo バージョン情報 @return アセット情報 */
    @Throws(IOException::class)
    private fun downloadAssetsIndex(verInfo: JMinecraftVersionList.Version): JAssets? {
        val assetIndex = verInfo.assetIndex ?: return null
        if (verInfo.assets == null) return null
        val targetFile = File(ProfilePathHome.getAssetsHome(), "indexes${File.separator}${verInfo.assets}.json")
        FileUtils.ensureParentDirectory(targetFile)
        DownloadUtils.ensureSha1(targetFile, assetIndex.sha1) {
            ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                R.string.newdl_downloading_metadata, targetFile.name)
            DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, assetIndex.url ?: "", targetFile)
            null
        }
        return Tools.GLOBAL_GSON.fromJson(Tools.read(targetFile), JAssets::class.java)
    }

    /** クライアント情報を取得する。 @param verInfo バージョン情報 @return クライアント情報 */
    private fun getClientInfo(verInfo: JMinecraftVersionList.Version): MinecraftClientInfo? {
        val downloads = verInfo.downloads ?: return null
        return downloads["client"]
    }

    /** メタデータをダウンロードし、ダウンロードタスクをスケジュールする。 @param verInfo バージョン情報 @param versionName バージョン名 */
    @Throws(IOException::class, MirrorTamperedException::class)
    private fun downloadAndProcessMetadata(verInfo: JMinecraftVersionList.Version?, versionName: String) {
        val versionJsonFile: File
        if (verInfo != null) versionJsonFile = downloadGameJson(verInfo)
        else versionJsonFile = createGameJsonPath(versionName)
        if (versionJsonFile.canRead()) {
            val readVerInfo = Tools.GLOBAL_GSON.fromJson(Tools.read(versionJsonFile), JMinecraftVersionList.Version::class.java)
            if (readVerInfo != null) {
                val assets = downloadAssetsIndex(readVerInfo)
                if (assets != null) scheduleAssetDownloads(assets)

                val minecraftClientInfo = getClientInfo(readVerInfo)
                if (minecraftClientInfo != null) scheduleGameJarDownload(minecraftClientInfo, versionName)

                if (readVerInfo.libraries != null) {
                    val libraries = readVerInfo.libraries!!
                    scheduleLibraryDownloads(libraries)
                }

                if (Tools.isValidString(readVerInfo.inheritsFrom)) {
                    val inheritsFrom = readVerInfo.inheritsFrom!!
                    val inheritedVersion = AsyncMinecraftDownloader.getListedVersion(inheritsFrom)
                    downloadAndProcessMetadata(inheritedVersion, inheritsFrom)
                }
            }
        } else {
            throw IOException("Unable to read Version JSON for version $versionName")
        }
    }

    /** ダウンロードリストの容量を拡張する。 @param addedElementCount 追加要素数 */
    private fun growDownloadList(addedElementCount: Int) {
        mScheduledDownloadTasks!!.ensureCapacity(mScheduledDownloadTasks!!.size + addedElementCount)
    }

    /** ダウンロードをスケジュールに追加する。 @param targetFile ターゲットファイル @param downloadClass ダウンロードクラス @param url URL @param sha1 SHA1 @param size サイズ @param skipIfFailed 失敗時にスキップするか */
    @Throws(IOException::class)
    private fun scheduleDownload(
        targetFile: File, downloadClass: Int, url: String, sha1: String?,
        size: Long, skipIfFailed: Boolean
    ) {
        FileUtils.ensureParentDirectory(targetFile)
        mTotalFileCount++
        var finalSize = size
        if (finalSize <= 0 && !mUseFileCounter) {
            try {
                finalSize = DownloadMirror.getContentLengthMirrored(downloadClass, url)
            } catch (e: Exception) {
                finalSize = -1
            }
        }
        if (finalSize < 0) {
            finalSize = 0
            mUseFileCounter = true
            Logging.i("MinecraftDownloader", "Failed to determine size of ${targetFile.name}, switching to file counter")
        } else {
            mTotalSize += finalSize
        }
        mScheduledDownloadTasks!!.add(
            DownloaderTask(targetFile, downloadClass, url, sha1, finalSize, skipIfFailed)
        )
    }

    /** ネイティブライブラリのダウンロードをスケジュールする。 @param baseRepository ベースリポジトリ @param dependentLibrary 依存ライブラリ */
    @Throws(IOException::class)
    private fun scheduleNativeLibraryDownload(baseRepository: String, dependentLibrary: DependentLibrary) {
        val libArtifactPath = Tools.artifactToPath(dependentLibrary) ?: return
        val path = FileUtils.removeExtension(libArtifactPath) + ".aar"
        val downloadUrl = "$baseRepository$path"
        val targetPath = File(ProfilePathHome.getLibrariesHome(), path)
        mDeclaredNatives!!.add(targetPath)
        scheduleDownload(targetPath, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, downloadUrl, null, 0, true)
    }

    /** ライブラリのダウンロードをスケジュールする。 @param dependentLibraries 依存ライブラリ配列 */
    @Throws(IOException::class)
    private fun scheduleLibraryDownloads(dependentLibraries: Array<DependentLibrary>) {
        Tools.preProcessLibraries(dependentLibraries)
        growDownloadList(dependentLibraries.size)
        for (dependentLibrary in dependentLibraries) {
            val libName = dependentLibrary.name ?: continue
            if (libName.startsWith("org.lwjgl")) continue
            if (libName.startsWith("net.java.dev.jna:jna:")) {
                scheduleNativeLibraryDownload(MAVEN_CENTRAL_REPO1, dependentLibrary)
            }

            val libArtifactPath = Tools.artifactToPath(dependentLibrary) ?: continue

            var sha1: String? = null
            var url: String? = null
            var size = 0L
            var skipIfFailed = false
            if (dependentLibrary.downloads != null) {
                val artifact = dependentLibrary.downloads!!.artifact
                if (artifact != null) {
                    sha1 = artifact.sha1
                    url = artifact.url
                    size = artifact.size.toLong()
                } else {
                    Logging.i("NewMCDownloader", "Skipped library ${dependentLibrary.name} due to lack of artifact")
                    continue
                }
            }
            if (url == null) {
                url = (dependentLibrary.url
                    ?: "https://libraries.minecraft.net/").replace("http://", "https://") + libArtifactPath
                skipIfFailed = true
            }
            if (!AllSettings.checkLibraries.getValue()) sha1 = null
            scheduleDownload(
                File(ProfilePathHome.getLibrariesHome(), libArtifactPath),
                DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                url!!, sha1, size, skipIfFailed
            )
        }
    }

    /** アセットのダウンロードをスケジュールする。 @param assets アセット情報 */
    @Throws(IOException::class)
    private fun scheduleAssetDownloads(assets: JAssets) {
        val assetObjects = assets.objects ?: return
        val assetNames = assetObjects.keys
        growDownloadList(assetNames.size)
        for (asset in assetNames) {
            val assetInfo = assetObjects[asset] ?: continue
            val targetFile: File
            val hash = assetInfo.hash ?: continue
            val hashedPath = "${hash.substring(0, 2)}${File.separator}$hash"
            val basePath = if (assets.mapToResources) ProfilePathHome.getResourcesHome() else ProfilePathHome.getAssetsHome()
            if (assets.virtual || assets.mapToResources) {
                targetFile = File(basePath, asset)
            } else {
                targetFile = File(basePath, "objects${File.separator}$hashedPath")
            }
            val sha1 = if (AllSettings.checkLibraries.getValue()) assetInfo.hash else null
            scheduleDownload(
                targetFile,
                DownloadMirror.DOWNLOAD_CLASS_ASSETS,
                "${MINECRAFT_RES}$hashedPath",
                sha1,
                assetInfo.size.toLong(),
                false
            )
        }
    }

    /** ゲームJarのダウンロードをスケジュールする。 @param minecraftClientInfo クライアント情報 @param versionName バージョン名 */
    @Throws(IOException::class)
    private fun scheduleGameJarDownload(minecraftClientInfo: MinecraftClientInfo, versionName: String) {
        val clientJar = createGameJarPath(versionName)
        val clientSha1 = if (AllSettings.checkLibraries.getValue()) minecraftClientInfo.sha1 else null
        growDownloadList(1)
        scheduleDownload(
            clientJar,
            DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
            minecraftClientInfo.url ?: "",
            clientSha1,
            minecraftClientInfo.size.toLong(),
            false
        )
        mSourceJarFile = clientJar
    }

    /** 個別のダウンロードタスクを表す内部クラス。 */
    private inner class DownloaderTask(
        private val mTargetPath: File,
        private val mDownloadClass: Int,
        private val mTargetUrl: String,
        private var mTargetSha1: String?,
        private val mDownloadSize: Long,
        private val mSkipIfFailed: Boolean
    ) : Runnable, Tools.DownloaderFeedback {

        /** 前回の進捗位置 */
        private var mLastCurr: Long = 0

        /** SHA1ハッシュをダウンロードする。 @return SHA1ハッシュ */
        @Throws(IOException::class)
        private fun downloadSha1(): String? {
            val downloadedHash = DownloadMirror.downloadStringMirrored(
                mDownloadClass, "$mTargetUrl.sha1"
            )
            if (!Tools.isValidString(downloadedHash)) return null
            val trimmed = downloadedHash!!.trim { it <= ' ' }
            return if (trimmed.length != 40) null else trimmed
        }

        /** ライブラリのSHA1ハッシュを取得する。 */
        private fun tryGetLibrarySha1() {
            var resultHash: String? = null
            try {
                resultHash = downloadSha1()
                mInternetUsageCounter!!.getAndAdd(40)
            } catch (e: IOException) {
                Logging.i("MinecraftDownloader", "Failed to download hash", e)
            }
            if (resultHash != null) {
                Logging.i("MinecraftDownloader", "Got hash: $resultHash for ${FileUtils.getFileName(mTargetUrl)}")
                mTargetSha1 = resultHash
            }
        }

        /** タスクを実行する。 */
        override fun run() {
            try {
                runCatching()
            } catch (e: Exception) {
                mDownloaderThreadException!!.set(e)
            }
        }

        /** 例外処理を含む実行処理。 */
        @Throws(Exception::class)
        private fun runCatching() {
            if (mDownloadClass == DownloadMirror.DOWNLOAD_CLASS_LIBRARIES && !Tools.isValidString(mTargetSha1)) {
                tryGetLibrarySha1()
            }
            if (Tools.isValidString(mTargetSha1)) {
                verifyFileSha1()
            } else {
                mTargetSha1 = null
                if (mTargetPath.exists()) finishWithoutDownloading()
                else downloadFile()
            }
        }

        /** SHA1を検証する。 */
        @Throws(Exception::class)
        private fun verifyFileSha1() {
            if (mTargetPath.isFile && mTargetPath.canRead() && Tools.compareSHA1(mTargetPath, mTargetSha1)) {
                finishWithoutDownloading()
            } else {
                downloadFile()
            }
        }

        /** ファイルをダウンロードする。 */
        @Throws(Exception::class)
        private fun downloadFile() {
            try {
                DownloadUtils.ensureSha1(mTargetPath, mTargetSha1) {
                    DownloadMirror.downloadFileMirrored(mDownloadClass, mTargetUrl, mTargetPath,
                        localBuffer, this)
                    null
                }
            } catch (e: Exception) {
                if (!mSkipIfFailed) throw e
            }
            mProcessedFileCounter!!.incrementAndGet()
        }

        /** ダウンロードせずに完了としてマークする。 */
        private fun finishWithoutDownloading() {
            mProcessedFileCounter!!.incrementAndGet()
            mProcessedSizeCounter!!.addAndGet(mDownloadSize)
        }

        /** ダウンロード進捗を更新する。 @param curr 現在のバイト数 @param max 最大バイト数 */
        override fun updateProgress(curr: Long, max: Long) {
            val delta = curr - mLastCurr
            mProcessedSizeCounter!!.addAndGet(delta)
            mInternetUsageCounter!!.addAndGet(delta)
            mLastCurr = curr
        }
    }

    companion object {
        /** 1メガバイト（バイト単位） */
        private const val ONE_MEGABYTE = 1024.0 * 1024.0
        /** MinecraftリソースURL */
        const val MINECRAFT_RES = "https://resources.download.minecraft.net/"
        /** Maven CentralリポジトリURL */
        private const val MAVEN_CENTRAL_REPO1 = "https://repo1.maven.org/maven2/"

        /** スレッドローカルなダウンロードバッファ */
        private val sThreadLocalDownloadBuffer = ThreadLocal<ByteArray>()

        /** スレッドローカルなバッファを取得する。 @return バッファ */
        private val localBuffer: ByteArray
            get() {
                val tlb = sThreadLocalDownloadBuffer.get()
                if (tlb != null) return tlb
                val newBuf = ByteArray(32768)
                sThreadLocalDownloadBuffer.set(newBuf)
                return newBuf
            }
    }
}
