package com.arata.yukarilauncher.utils.file

import java.io.File
import java.io.IOException

/** ファイル・ディレクトリ操作のユーティリティを提供するオブジェクト。 */
object FileUtils {

    /** 指定されたパスのファイルまたはディレクトリが存在するか確認する。 @param filePath 確認するパス @return 存在する場合は true */
    fun exists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    /** パスまたはURLからファイル名を取得する。 @param pathOrUrl パスまたはURL @return ファイル名、取得不可の場合は null */
    fun getFileName(pathOrUrl: String): String? {
        val lastSlashIndex = pathOrUrl.lastIndexOf('/')
        if (lastSlashIndex == -1) return null
        return pathOrUrl.substring(lastSlashIndex)
    }

    /** パスまたはURLから拡張子を取り除く。 @param pathOrUrl パスまたはURL @return 拡張子除去後の文字列 */
    fun removeExtension(pathOrUrl: String): String {
        val lastDotIndex = pathOrUrl.lastIndexOf('.')
        if (lastDotIndex == -1) return pathOrUrl
        return pathOrUrl.substring(0, lastDotIndex)
    }

    /** ディレクトリが存在しない場合は作成する。 @param targetFile 対象ディレクトリ @return 成功した場合は true */
    fun ensureDirectorySilently(targetFile: File): Boolean {
        if (targetFile.isFile) return false
        return if (targetFile.exists()) targetFile.canWrite()
        else targetFile.mkdirs()
    }

    /** 親ディレクトリが存在しない場合は作成する（エラー無視）。 @param targetFile 対象ファイル @return 成功した場合は true */
    fun ensureParentDirectorySilently(targetFile: File): Boolean {
        val parentFile = targetFile.parentFile
        if (parentFile == null) return false
        return ensureDirectorySilently(parentFile)
    }

    /**
     * ディレクトリが存在しない場合は作成する。エラー時は例外をスローする。
     * @param targetFile 対象ディレクトリ
     * @throws IOException 作成失敗または書き込み不可の場合
     */
    @JvmStatic fun ensureDirectory(targetFile: File) {
        if (targetFile.isFile) throw IOException("Target directory is a file")
        if (targetFile.exists()) {
            if (!targetFile.canWrite()) throw IOException("Target directory is not writable")
        } else if (!targetFile.mkdirs()) throw IOException("Unable to create target directory")
    }

    /**
     * 親ディレクトリが存在しない場合は作成する。
     * @param targetFile 対象ファイル
     * @throws IOException 親ディレクトリが存在しない、または作成失敗時
     */
    fun ensureParentDirectory(targetFile: File) {
        val parentFile = targetFile.parentFile
        if (parentFile == null) throw IOException("targetFile does not have a parent")
        ensureDirectory(parentFile)
    }
}
