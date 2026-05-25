package com.arata.yukarilauncher.utils.runtime

import android.system.Os
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.unpack.Jre
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.stringutils.SortStrings
import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.MathUtils
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

/** マルチランタイム（JRE）の管理ユーティリティを提供するオブジェクト。 */
object MultiRTUtils {

    /** ランタイム情報のキャッシュ。 */
    private val sCache = HashMap<String, Runtime>()
    /** ランタイムルートフォルダ。 */
    private val RUNTIME_FOLDER = File(PathManager.DIR_MULTIRT_HOME)
    /** releaseファイル内のJAVA_VERSIONキー。 */
    private const val JAVA_VERSION_STR = "JAVA_VERSION=\""
    /** releaseファイル内のOS_ARCHキー。 */
    private const val OS_ARCH_STR = "OS_ARCH=\""

    /** インストール済みランタイムのリスト。バージョン順にソートされる。 */
    val runtimes: List<Runtime>
        get() {
            if (!RUNTIME_FOLDER.exists() && !RUNTIME_FOLDER.mkdirs()) {
                throw RuntimeException("Failed to create runtime directory")
            }

            val runtimes = ArrayList<Runtime>()
            val files = RUNTIME_FOLDER.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) continue

                    val fileName = file.name
                    val runtime = read(fileName)

                    for (jre in Jre.entries) {
                        if (jre.jreName == fileName) {
                            runtime.isProvidedByLauncher = true
                            break
                        }
                    }

                    runtimes.add(runtime)
                }
            } else {
                throw RuntimeException("The runtime directory does not exist")
            }

            runtimes.sortWith { o1, o2 ->
                val thisRuntime = o1.versionString ?: o1.name
                val otherRuntime = o2.versionString ?: o2.name
                -SortStrings.compareClassVersions(thisRuntime, otherRuntime)
            }

            return runtimes
        }

    /**
     * 指定メジャーバージョンに完全一致するJRE名を返す。
     * @param majorVersion Javaメジャーバージョン
     * @return JRE名、見つからない場合は null
     */
    fun getExactJreName(majorVersion: Int): String? {
        val runtimes = runtimes
        for (r in runtimes) {
            if (r.javaVersion == majorVersion) return r.name
        }
        return null
    }

    /**
     * 指定メジャーバージョン以上のうち最も近いJRE名を返す。
     * @param majorVersion Javaメジャーバージョン
     * @return JRE名、見つからない場合は null
     */
    fun getNearestJreName(majorVersion: Int): String? {
        val runtimes = runtimes
        val nearestRankedRuntime = MathUtils.findNearestPositive(majorVersion, runtimes, MathUtils.ValueProvider { it.javaVersion })
        val nearestRuntime = nearestRankedRuntime?.value ?: return null
        return nearestRuntime.name
    }

    /**
     * ランタイムをインストールする（tar.xz形式）。
     * @param nativeLibDir ネイティブライブラリディレクトリ
     * @param runtimeInputStream ランタイムの入力ストリーム
     * @param name ランタイム名
     * @throws IOException インストール失敗時
     */
    @Throws(IOException::class)
    fun installRuntimeNamed(nativeLibDir: String, runtimeInputStream: InputStream, name: String) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        uncompressTarXZ(runtimeInputStream, dest)
        runtimeInputStream.close()
        unpack200(nativeLibDir, RUNTIME_FOLDER.toString() + "/" + name)
        ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME)
        read(name)
    }

    /**
     * ランタイムインストール後の後処理を行う。
     * @param name ランタイム名
     * @throws IOException 処理失敗時
     */
    @Throws(IOException::class)
    fun postPrepare(name: String) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (!dest.exists()) return
        val runtime = read(name)
        var libFolder = "lib"
        if (File(dest, "$libFolder/${runtime.arch}").exists()) libFolder = "$libFolder/${runtime.arch}"
        val ftIn = File(dest, "$libFolder/libfreetype.so.6")
        val ftOut = File(dest, "$libFolder/libfreetype.so")
        if (ftIn.exists() && (!ftOut.exists() || ftIn.length() != ftOut.length())) {
            if (!ftIn.renameTo(ftOut)) throw IOException("Failed to rename freetype")
        }

        copyDummyNativeLib("libawt_xawt.so", dest, libFolder)
    }

    /**
     * バイナリパッケージからランタイムをインストールする。
     * @param universalFileInputStream ユニバーサルファイルの入力ストリーム
     * @param platformBinsInputStream プラットフォームバイナリの入力ストリーム
     * @param name ランタイム名
     * @param binpackVersion バイナリパッケージバージョン
     * @throws IOException インストール失敗時
     */
    @Throws(IOException::class)
    fun installRuntimeNamedBinpack(universalFileInputStream: InputStream, platformBinsInputStream: InputStream, name: String, binpackVersion: String) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        installRuntimeNamedNoRemove(universalFileInputStream, dest)
        installRuntimeNamedNoRemove(platformBinsInputStream, dest)

        unpack200(PathManager.DIR_NATIVE_LIB, RUNTIME_FOLDER.toString() + "/" + name)

        val binpackVerFile = File(RUNTIME_FOLDER, "/$name/pojav_version")
        FileOutputStream(binpackVerFile).use { fos ->
            fos.write(binpackVersion.toByteArray())
        }

        ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME)
        forceReread(name)
    }

    /**
     * ランタイムの内部バージョン文字列を読み取る。
     * @param name ランタイム名
     * @return バージョン文字列、存在しない場合は null
     */
    fun readInternalRuntimeVersion(name: String): String? {
        val versionFile = File(RUNTIME_FOLDER, "/$name/pojav_version")
        return try {
            if (versionFile.exists()) {
                Tools.read(versionFile.absolutePath)
            } else {
                null
            }
        } catch (e: IOException) {
            Logging.e("ReadInternalRuntimeVersion", Tools.printToString(e))
            null
        }
    }

    /**
     * 指定されたランタイムを削除する。
     * @param name ランタイム名
     * @throws IOException 削除失敗時
     */
    @Throws(IOException::class)
    fun removeRuntimeNamed(name: String) {
        val dest = File(RUNTIME_FOLDER, name)
        if (dest.exists()) {
            FileUtils.deleteDirectory(dest)
            sCache.remove(name)
        }
    }

    /**
     * ランタイムのホームディレクトリを取得する。
     * @param name ランタイム名
     * @return ホームディレクトリ
     */
    fun getRuntimeHome(name: String): File {
        val dest = File(RUNTIME_FOLDER, name)
        Logging.i("MiltiRTUitls", "Dest exists? ${dest.exists()}")
        if ((!dest.exists()) || forceReread(name).versionString == null) throw RuntimeException("Selected runtime is broken!")
        return dest
    }

    /** キャッシュを強制的に無効化してランタイム情報を再読込する。 @param name ランタイム名 @return 再読込後のRuntimeオブジェクト */
    fun forceReread(name: String): Runtime {
        sCache.remove(name)
        return read(name)
    }

    /**
     * ランタイム情報を読み込む（キャッシュ利用）。
     * @param name ランタイム名
     * @return Runtimeオブジェクト
     */
    fun read(name: String): Runtime {
        val cached = sCache[name]
        if (cached != null) return cached

        val release = File(RUNTIME_FOLDER, "$name/release")
        val returnRuntime: Runtime = if (!release.exists()) {
            Runtime(name)
        } else {
            try {
                val content = Tools.read(release.absolutePath)
                val javaVersion = Tools.extractUntilCharacter(content, JAVA_VERSION_STR, '"')
                val osArch = Tools.extractUntilCharacter(content, OS_ARCH_STR, '"')
                if (javaVersion != null && osArch != null) {
                    val javaVersionSplit = javaVersion.split('.')
                    val javaVersionInt: Int = if (javaVersionSplit[0] == "1") {
                        javaVersionSplit[1].toInt()
                    } else {
                        javaVersionSplit[0].toInt()
                    }
                    Runtime(name, javaVersion, osArch, javaVersionInt)
                } else {
                    Runtime(name)
                }
            } catch (e: IOException) {
                Runtime(name)
            }
        }
        sCache[name] = returnRuntime
        return returnRuntime
    }

    /**
     * pack200形式のファイルを展開する。
     * @param nativeLibraryDir ネイティブライブラリディレクトリ
     * @param runtimePath ランタイムパス
     */
    private fun unpack200(nativeLibraryDir: String, runtimePath: String) {
        val basePath = File(runtimePath)
        val files = FileUtils.listFiles(basePath, arrayOf("pack"), true)

        val workdir = File(nativeLibraryDir)
        val processBuilder = ProcessBuilder().directory(workdir)
        for (jarFile in files) {
            try {
                val process = processBuilder
                    .command("./libunpack200.so", "-r", jarFile.absolutePath, jarFile.absolutePath.replace(".pack", ""))
                    .start()
                process.waitFor()
            } catch (e: Exception) {
                Logging.e("MULTIRT", "Failed to unpack the runtime !")
            }
        }
    }

    /**
     * ダミーのネイティブライブラリをコピーする。
     * @param name ライブラリ名
     * @param dest コピー先ディレクトリ
     * @param libFolder ライブラリフォルダ名
     * @throws IOException コピー失敗時
     */
    @Throws(IOException::class)
    private fun copyDummyNativeLib(name: String, dest: File, libFolder: String) {
        val fileLib = File(dest, "/$libFolder/$name")
        FileInputStream(File(PathManager.DIR_NATIVE_LIB, name)).use { is_ ->
            FileOutputStream(fileLib).use { os ->
                IOUtils.copy(is_, os)
            }
        }
    }

    /**
     * ランタイムをインストールする（削除なし）。
     * @param runtimeInputStream 入力ストリーム
     * @param dest インストール先ディレクトリ
     * @throws IOException インストール失敗時
     */
    @Throws(IOException::class)
    private fun installRuntimeNamedNoRemove(runtimeInputStream: InputStream, dest: File) {
        uncompressTarXZ(runtimeInputStream, dest)
        runtimeInputStream.close()
    }

    /**
     * tar.xz形式のアーカイブを展開する。
     * @param tarFileInputStream 入力ストリーム
     * @param dest 展開先ディレクトリ
     * @throws IOException 展開失敗時
     */
    @Throws(IOException::class)
    private fun uncompressTarXZ(tarFileInputStream: InputStream, dest: File) {
        com.arata.yukarilauncher.utils.file.FileUtils.ensureDirectory(dest)

        val buffer = ByteArray(8192)
        val tarIn = TarArchiveInputStream(XZCompressorInputStream(tarFileInputStream))
        var tarEntry = tarIn.nextTarEntry

        while (tarEntry != null) {
            val tarEntryName = tarEntry.name
            ProgressLayout.setProgress(ProgressLayout.UNPACK_RUNTIME, 100, R.string.generic_unpacking, tarEntryName)

            val destPath = File(dest, tarEntry.name)
            com.arata.yukarilauncher.utils.file.FileUtils.ensureParentDirectory(destPath)
            if (tarEntry.isSymbolicLink) {
                try {
                    Os.symlink(tarEntry.name, tarEntry.linkName)
                } catch (e: Throwable) {
                    Logging.e("MultiRT", Tools.printToString(e))
                }
            } else if (tarEntry.isDirectory) {
                com.arata.yukarilauncher.utils.file.FileUtils.ensureDirectory(destPath)
            } else if (!destPath.exists() || destPath.length() != tarEntry.size) {
                FileOutputStream(destPath).use { os ->
                    IOUtils.copyLarge(tarIn, os, buffer)
                }
            }
            tarEntry = tarIn.nextTarEntry
        }
        tarIn.close()
    }
}
