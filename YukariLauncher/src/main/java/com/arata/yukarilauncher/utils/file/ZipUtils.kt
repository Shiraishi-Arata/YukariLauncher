package com.arata.yukarilauncher.utils.file

import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile

/** ZIPファイル操作用のユーティリティを提供するオブジェクト。 */
object ZipUtils {

    /**
     * ZIPファイル内のエントリの入力ストリームを取得する。
     * @param zipFile ZIPファイル
     * @param entryPath エントリのパス
     * @return エントリの入力ストリーム
     */
    fun getEntryStream(zipFile: ZipFile, entryPath: String): InputStream {
        val entry = zipFile.getEntry(entryPath)
        if (entry == null) throw java.io.IOException("No entry in ZIP file: $entryPath")
        return zipFile.getInputStream(entry)
    }

    /**
     * ZIPファイル内の特定ディレクトリ以下のファイルを展開する。
     * @param zipFile ZIPファイル
     * @param dirName 展開するディレクトリ名
     * @param destination 展開先ディレクトリ
     */
    fun zipExtract(zipFile: ZipFile, dirName: String, destination: File) {
        val zipEntries = zipFile.entries()

        val dirNameLen = dirName.length
        while (zipEntries.hasMoreElements()) {
            val zipEntry = zipEntries.nextElement()
            val entryName = zipEntry.name
            if (!entryName.startsWith(dirName) || zipEntry.isDirectory) continue
            val zipDestination = File(destination, entryName.substring(dirNameLen))
            FileUtils.ensureParentDirectory(zipDestination)
            val inputStream = zipFile.getInputStream(zipEntry)
            val outputStream: OutputStream = FileOutputStream(zipDestination)
            inputStream.use { `is` ->
                outputStream.use { os ->
                    IOUtils.copy(`is`, os)
                }
            }
        }
    }
}