package com.arata.yukarilauncher.utils.file

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.EditText
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.ui.dialog.EditTextDialog
import com.arata.yukarilauncher.ui.dialog.EditTextDialog.ConfirmListener
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileTools {
    companion object {
        const val INVALID_CHARACTERS_REGEX = "[\\\\/:*?\"<>|\\t\\n]"

        /**
         * ディレクトリを作成する
         */
        @JvmStatic
        fun mkdir(dir: File): Boolean {
            return dir.mkdir()
        }

        /**
         * 親ディレクトリも含めてディレクトリを作成する
         */
        @JvmStatic
        fun mkdirs(dir: File): Boolean {
            return dir.mkdirs()
        }

        /**
         * URIからファイルをバックグラウンドでコピーする（出力先はルートパス+ファイル名）
         */
        @JvmStatic
        fun copyFileInBackground(context: Context, fileUri: Uri, rootPath: String): File {
            val fileName = Tools.getFileName(context, fileUri)
            val outputFile = File(rootPath, fileName)
            return copyFileInBackground(context, fileUri, outputFile)
        }

        /**
         * URIからファイルをバックグラウンドで指定された出力先にコピーする
         */
        @JvmStatic
        fun copyFileInBackground(context: Context, fileUri: Uri, outputFile: File): File {
            context.contentResolver.openInputStream(fileUri).use { inputStream ->
                FileUtils.copyInputStreamToFile(inputStream, outputFile)
            }
            return outputFile
        }

        /**
         * ファイル名として無効な文字を置換し、長さを255文字以内に制限する
         */
        @JvmStatic
        fun ensureValidFilename(str: String): String =
            str.trim().replace(INVALID_CHARACTERS_REGEX.toRegex(), "-").run {
                if (length > 255) substring(0, 255)
                else this
            }

        /**
         * ファイル名の妥当性をチェックする
         * 不正な文字が含まれている場合や長さが255を超えている場合に例外をスローする
         */
        @Throws(InvalidFilenameException::class)
        @JvmStatic
        fun checkFilenameValidity(str: String) {
            val illegalCharsRegex = INVALID_CHARACTERS_REGEX.toRegex()

            val illegalChars = illegalCharsRegex.findAll(str)
                .map { it.value }
                .distinct()
                .joinToString("")

            if (illegalChars.isNotEmpty()) {
                throw InvalidFilenameException("The filename contains illegal characters", illegalChars)
            }

            if (str.length > 255) {
                throw InvalidFilenameException("Invalid filename length", str.length)
            }
        }

        /**
         * ファイル名が無効かどうかをコールバックで判定する
         */
        @JvmStatic
        fun isFilenameInvalid(
            str: String,
            containsIllegalCharacters: (illegalCharacters: String) -> Unit,
            isInvalidLength: (invalidLength: Int) -> Unit
        ): Boolean {
            try {
                checkFilenameValidity(str)
            } catch (e: InvalidFilenameException) {
                if (e.containsIllegalCharacters()) {
                    containsIllegalCharacters(e.illegalCharacters)
                    return true
                } else if (e.isInvalidLength) {
                    isInvalidLength(e.invalidLength)
                    return true
                }
            }
            return false
        }

        /**
         * EditTextの内容がファイル名として有効かどうかをチェックする
         */
        @JvmStatic
        fun isFilenameInvalid(editText: EditText): Boolean {
            val str = editText.text.toString()
            return isFilenameInvalid(
                str,
                { illegalCharacters ->
                    editText.error = editText.context.getString(R.string.generic_input_invalid_character, illegalCharacters)
                },
                { invalidLength ->
                    editText.error = editText.context.getString(R.string.file_invalid_length, invalidLength, 255)
                }
            )
        }

        /**
         * フォルダ内の最新ファイルを取得する
         */
        @JvmStatic
        fun getLatestFile(folderPath: String?, modifyTime: Int): File? {
            if (folderPath == null) return null
            return getLatestFile(File(folderPath), modifyTime.toLong())
        }

        /**
         * フォルダ内で指定した時間以内に更新された最新ファイルを取得する
         * modifyTime（秒）以内に更新されたファイルがない場合はnullを返す
         */
        @JvmStatic
        fun getLatestFile(folder: File?, modifyTime: Long): File? {
            if (folder == null || !folder.isDirectory) {
                return null
            }

            val files = folder.listFiles(FilenameFilter { _: File?, name: String ->
                !name.startsWith(
                    "."
                )
            })
            if (files == null || files.isEmpty()) {
                return null
            }

            val fileList: List<File> = listOf(*files)
            fileList.sortedWith(Comparator.comparingLong { obj: File -> obj.lastModified() }
                .reversed())

            if (modifyTime > 0) {
                val difference =
                    (YLTools.getCurrentTimeMillis() - fileList[0].lastModified()) / 1000
                if (difference >= modifyTime) {
                    return null
                }
            }

            return fileList[0]
        }

        /**
         * ファイルを共有する
         */
        @JvmStatic
        fun shareFile(context: Context, file: File) {
            shareFile(context, file.name, file.absolutePath)
        }

        /**
         * ファイル名とパスを指定してファイルを共有する
         */
        @JvmStatic
        fun shareFile(context: Context, fileName: String, filePath: String) {
            val contentUri = DocumentsContract.buildDocumentUri(
                context.getString(R.string.storageProviderAuthorities),
                filePath
            )

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            shareIntent.setDataAndType(contentUri, "*/*")

            val sendIntent = Intent.createChooser(shareIntent, fileName)
            context.startActivity(sendIntent)
        }

        /**
         * ファイル名変更ダイアログを表示する（拡張子指定版）
         */
        @JvmStatic
        @SuppressLint("UseCompatLoadingForDrawables")
        fun renameFileListener(context: Context, endTask: Task<*>?, file: File, suffix: String) {
            val fileParent = file.parent
            val fileName = file.name

            EditTextDialog.Builder(context)
                .setTitle(R.string.generic_rename)
                .setEditText(getFileNameWithoutExtension(fileName, suffix))
                .setAsRequired()
                .setConfirmListener(ConfirmListener { editBox, _ ->
                    val newName = editBox.text.toString()

                    if (isFilenameInvalid(editBox)) {
                        return@ConfirmListener false
                    }

                    if (fileName == newName) {
                        return@ConfirmListener true
                    }

                    val newFile = File(fileParent, newName + suffix)
                    if (newFile.exists()) {
                        editBox.error = context.getString(R.string.file_rename_exitis)
                        return@ConfirmListener false
                    }

                    val renamed = file.renameTo(newFile)
                    if (renamed) {
                        endTask?.execute()
                    }
                    true
                }).showDialog()
        }

        /**
         * ファイル名変更ダイアログを表示する（拡張子なし版）
         */
        @JvmStatic
        @SuppressLint("UseCompatLoadingForDrawables")
        fun renameFileListener(context: Context, endTask: Task<*>?, file: File) {
            val fileParent = file.parent
            val fileName = file.name

            EditTextDialog.Builder(context)
                .setTitle(R.string.generic_rename)
                .setEditText(fileName)
                .setAsRequired()
                .setConfirmListener(ConfirmListener { editBox, _ ->
                    val newName = editBox.text.toString()

                    if (isFilenameInvalid(editBox)) {
                        return@ConfirmListener false
                    }

                    if (fileName == newName) {
                        return@ConfirmListener true
                    }

                    val newFile = File(fileParent, newName)
                    if (newFile.exists()) {
                        editBox.error = context.getString(R.string.file_rename_exitis)
                        return@ConfirmListener false
                    }

                    val renamed = renameFile(file, newFile)
                    if (renamed) {
                        endTask?.execute()
                    }
                    true
                }).showDialog()
        }

        /**
         * ファイル名を変更する
         */
        @JvmStatic
        fun renameFile(origin: File, target: File): Boolean {
            return origin.renameTo(target)
        }

        /**
         * ファイルまたはディレクトリをコピーする
         */
        @JvmStatic
        fun copyFile(file :File, target: File) {
            if (file.isFile) FileUtils.copyFile(file, target)
            else if (file.isDirectory) FileUtils.copyDirectory(file, target)
        }

        /**
         * ファイルまたはディレクトリを移動する
         */
        @JvmStatic
        fun moveFile(file :File, target: File) {
            if (file.isFile) FileUtils.moveFile(file, target)
            else if (file.isDirectory) FileUtils.moveDirectory(file, target)
        }

        /**
         * ファイル名から拡張子を除いた部分を取得する
         */
        @JvmStatic
        fun getFileNameWithoutExtension(fileName: String, fileExtension: String?): String {
            val dotIndex = if (fileExtension == null) {
                fileName.lastIndexOf('.')
            } else {
                fileName.lastIndexOf(fileExtension)
            }
            return if (dotIndex == -1) fileName else fileName.substring(0, dotIndex)
        }

        /**
         * Fileオブジェクトから拡張子を除いたファイル名を取得する
         */
        @JvmStatic
        fun getFileNameWithoutExtension(file: File): String = file.nameWithoutExtension

        /**
         * ファイルサイズを人間が読みやすい形式にフォーマットする
         */
        @JvmStatic
        @SuppressLint("DefaultLocale")
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"

            val units = arrayOf("B", "KB", "MB", "GB")
            var unitIndex = 0
            var value = bytes.toDouble()
            // 適切な単位をループで選択する
            while (value >= 1024 && unitIndex < units.size - 1) {
                value /= 1024.0
                unitIndex++
            }
            return String.format("%.2f %s", value, units[unitIndex])
        }

        /**
         * ディレクトリをZIPに圧縮する（フィルター付き）
         */
        @JvmStatic
        @Throws(IOException::class)
        fun zipDirectory(folder: File, parentPath: String, filter: (File) -> Boolean, zos: ZipOutputStream) {
            val files = folder.listFiles()?.filter(filter) ?: return
            for (file in files) {
                if (file.isDirectory) {
                    zipDirectory(file, parentPath + file.name + "/", filter, zos)
                } else {
                    zipFile(file, parentPath + file.name, zos)
                }
            }
        }

        /**
         * 単一ファイルをZIPエントリとして追加する
         */
        @JvmStatic
        @Throws(IOException::class)
        fun zipFile(file: File, entryName: String, zos: ZipOutputStream) {
            FileInputStream(file).use { fis ->
                val zipEntry = ZipEntry(entryName)
                zipEntry.time = file.lastModified() // ファイルの更新日時を保持する
                zos.putNextEntry(zipEntry)

                val buffer = ByteArray(4096)
                var length: Int
                while ((fis.read(buffer).also { length = it }) >= 0) {
                    zos.write(buffer, 0, length)
                }
                zos.closeEntry()
            }
        }

        /**
         * ファイルのハッシュ値を計算する（デフォルトはSHA-256）
         */
        @JvmStatic
        @Throws(Exception::class)
        fun calculateFileHash(file: File, algorithm: String = "SHA-256"): String {
            return calculateFileHash(file.inputStream(), algorithm)
        }

        /**
         * InputStreamからハッシュ値を計算する
         */
        @JvmStatic
        @Throws(Exception::class)
        fun calculateFileHash(inputStream: InputStream, algorithm: String = "SHA-256"): String {
            val digest = MessageDigest.getInstance(algorithm)
            inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            return digest.digest().toHex()
        }

        /**
         * バイト配列を16進数文字列に変換する（高効率実装）
         */
        private fun ByteArray.toHex(): String {
            val hexChars = "0123456789abcdef"
            return joinToString("") { byte ->
                "${hexChars[byte.toInt() shr 4 and 0x0F]}${hexChars[byte.toInt() and 0x0F]}"
            }
        }
    }
}
