package com.arata.yukarilauncher.task

import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.platform.Architecture
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/** AARファイルからネイティブライブラリを展開するクラス。 */
class NativesExtractor(private val mDestinationDir: File) {

    /** ライブラリのアーキテクチャパス */
    private val mLibraryLocation: String = "jni/${getAarArchitectureName()}/"

    /** AARファイルからネイティブライブラリを展開する。 @param source AARファイル */
    @Throws(IOException::class)
    fun extractFromAar(source: File) {
        val buffer = ByteArray(8192)
        FileInputStream(source).use { fileInputStream ->
            ZipInputStream(fileInputStream).use { zipInputStream ->
                val entryCopyStream = NonCloseableInputStream(zipInputStream)
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    var entryName = entry.name
                    if (!entryName.startsWith(mLibraryLocation) || entry.isDirectory) {
                        entry = zipInputStream.nextEntry
                        continue
                    }
                    entryName = com.arata.yukarilauncher.utils.file.FileUtils.getFileName(entryName)
                    if (entryName == null || LIBRARY_BLACKLIST.contains(entryName)) {
                        entry = zipInputStream.nextEntry
                        continue
                    }

                    processEntry(entryCopyStream, entry, File(mDestinationDir, entryName), buffer)
                    entry = zipInputStream.nextEntry
                }
            }
        }
    }

    /** クローズを防止する入力ストリームラッパー。 */
    private class NonCloseableInputStream(`in`: InputStream) : FilterInputStream(`in`) {
        override fun close() {
            // 何もしない
        }
    }

    companion object {
        /** ブラックリストに登録されたライブラリ名 */
        private val LIBRARY_BLACKLIST = createLibraryBlacklist()

        /** ライブラリブラックリストを作成する。 @return ブラックリスト */
        private fun createLibraryBlacklist(): ArrayList<String> {
            val includedLibraryNames = File(PathManager.DIR_NATIVE_LIB).list() ?: emptyArray()
            val blacklist = ArrayList<String>(includedLibraryNames.size)
            for (libraryName in includedLibraryNames) {
                if (libraryName == "libjnidispatch.so") continue
                blacklist.add(libraryName)
            }
            blacklist.trimToSize()
            return blacklist
        }

        /** AARのアーキテクチャ名を取得する。 @return アーキテクチャ名 */
        private fun getAarArchitectureName(): String {
            return when (Architecture.getDeviceArchitecture()) {
                Architecture.ARCH_ARM -> "armeabi-v7a"
                Architecture.ARCH_ARM64 -> "arm64-v8a"
                Architecture.ARCH_X86 -> "x86"
                Architecture.ARCH_X86_64 -> "x86_64"
                else -> throw RuntimeException("Unknown CPU architecture: ${Architecture.getDeviceArchitecture()}")
            }
        }

        /** ファイルのCRC32チェックサムを計算する。 @param target 対象ファイル @param buffer バッファ @return CRC32値 */
        @Throws(IOException::class)
        private fun fileCrc32(target: File, buffer: ByteArray): Long {
            FileInputStream(target).use { fis ->
                val crc32 = CRC32()
                var len: Int
                while (fis.read(buffer).also { len = it } != -1) {
                    crc32.update(buffer, 0, len)
                }
                return crc32.value
            }
        }
    }

    /** ZIPエントリを処理してファイルに展開する。 @param sourceStream 入力ストリーム @param zipEntry ZIPエントリ @param entryDestination 出力先 @param buffer バッファ */
    @Throws(IOException::class)
    private fun processEntry(sourceStream: InputStream, zipEntry: ZipEntry, entryDestination: File, buffer: ByteArray) {
        if (entryDestination.exists()) {
            val expectedSize = zipEntry.size
            val expectedCrc32 = zipEntry.crc
            val realSize = entryDestination.length()
            val realCrc32 = fileCrc32(entryDestination, buffer)
            if (realSize == expectedSize && realCrc32 == expectedCrc32) return
        }
        org.apache.commons.io.FileUtils.copyInputStreamToFile(sourceStream, entryDestination)
    }
}