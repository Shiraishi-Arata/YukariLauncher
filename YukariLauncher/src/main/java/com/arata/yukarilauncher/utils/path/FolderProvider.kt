package com.arata.yukarilauncher.utils.path

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import androidx.annotation.Nullable
import com.arata.yukarilauncher.BuildConfig
import com.arata.yukarilauncher.InfoDistributor
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.log.Logging
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.LinkedList

/** ストレージアクセスフレームワーク（SAF）用のDocumentsProvider実装。ゲームディレクトリへのアクセスを提供する。 */
class FolderProvider : DocumentsProvider() {
    /** ルートとなるベースディレクトリ。 */
    private var BASE_DIR: File? = null
    /** コンテントリゾルバ。 */
    private var mContentResolver: ContentResolver? = null
    /** ストレージプロバイダのオーソリティ文字列。 */
    private var mStorageProviderAuthortiy: String? = null

    /**
     * ルートドキュメントを問い合わせる。
     * @param projection 取得するカラム
     * @return ルート情報を含むカーソル
     */
    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)

        var summary = InfoDistributor.APP_NAME
        if (BuildConfig.DEBUG) {
            summary = "(" + context!!.getString(R.string.generic_debug) + ") " + summary
        }

        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(BASE_DIR!!))
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(BASE_DIR!!))
        row.add(Root.COLUMN_SUMMARY, summary)
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_SEARCH or Root.FLAG_SUPPORTS_IS_CHILD)
        row.add(Root.COLUMN_TITLE, InfoDistributor.APP_NAME)
        row.add(Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES)
        row.add(Root.COLUMN_AVAILABLE_BYTES, BASE_DIR!!.freeSpace)
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        return result
    }

    /**
     * 指定されたドキュメントIDの情報を問い合わせる。
     * @param documentId ドキュメントID
     * @param projection 取得するカラム
     * @return ドキュメント情報を含むカーソル
     * @throws FileNotFoundException ドキュメントが見つからない場合
     */
    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        result.setNotificationUri(mContentResolver, createUriForDocId(documentId))
        includeFile(result, documentId, null)
        return result
    }

    /**
     * 子ドキュメント一覧を問い合わせる。
     * @param parentDocumentId 親ドキュメントID
     * @param projection 取得するカラム
     * @param sortOrder ソート順
     * @return 子ドキュメント一覧を含むカーソル
     * @throws FileNotFoundException 親ディレクトリが見つからない場合
     */
    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<String>?, sortOrder: String?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(parentDocumentId)
        val children = parent.listFiles()
        if (children == null) throw FileNotFoundException("Unable to list files in ${parent.absolutePath}")
        for (file in children) {
            includeFile(result, null, file)
        }
        result.setNotificationUri(mContentResolver, createUriForDocId(parentDocumentId))
        return result
    }

    /**
     * ドキュメントを開いてParcelFileDescriptorを返す。
     * @param documentId ドキュメントID
     * @param mode アクセスモード
     * @param signal キャンセルシグナル
     * @return ファイルディスクリプタ
     * @throws FileNotFoundException ドキュメントが見つからない場合
     */
    @Throws(FileNotFoundException::class)
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        val file = getFileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    /**
     * ドキュメントのサムネイルを開く。
     * @param documentId ドキュメントID
     * @param sizeHint 推奨サイズ
     * @param signal キャンセルシグナル
     * @return アセットファイルディスクリプタ
     * @throws FileNotFoundException ドキュメントが見つからない場合
     */
    @Throws(FileNotFoundException::class)
    override fun openDocumentThumbnail(documentId: String, sizeHint: Point, signal: CancellationSignal?): AssetFileDescriptor {
        val file = getFileForDocId(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    /**
     * プロバイダ生成時の初期化処理。
     * @return 初期化成功時は true
     */
    override fun onCreate(): Boolean {
        if (Tools.checkStorageRoot(context!!)) {
            PathManager.initContextConstants(context!!)
            Tools.initStorageConstants(context!!)
        } else {
            return false
        }
        BASE_DIR = File(PathManager.DIR_GAME_HOME)
        mContentResolver = context!!.contentResolver
        mStorageProviderAuthortiy = context!!.getString(R.string.storageProviderAuthorities)
        return true
    }

    /**
     * 新しいドキュメントを作成する。
     * @param parentDocumentId 親ドキュメントID
     * @param mimeType MIMEタイプ
     * @param displayName 表示名
     * @return 作成したドキュメントのID
     * @throws FileNotFoundException 作成失敗時
     */
    @Throws(FileNotFoundException::class)
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        var newFile = File(parentDocumentId, displayName)
        var noConflictId = 2
        while (newFile.exists()) {
            newFile = File(parentDocumentId, "$displayName (${noConflictId++})")
        }
        try {
            val succeeded: Boolean = if (Document.MIME_TYPE_DIR == mimeType) {
                newFile.mkdir()
            } else {
                newFile.createNewFile()
            }
            if (!succeeded) {
                throw FileNotFoundException("Failed to create document with id ${newFile.path}")
            }
        } catch (e: IOException) {
            throw FileNotFoundException("Failed to create document with id ${newFile.path}")
        }
        notifyChange(createUriForDocId(parentDocumentId))
        return newFile.path
    }

    /**
     * ドキュメント名を変更する。
     * @param documentId ドキュメントID
     * @param displayName 新しい表示名
     * @return 新しいドキュメントID
     * @throws FileNotFoundException 名前変更失敗時
     */
    @Throws(FileNotFoundException::class)
    override fun renameDocument(documentId: String, displayName: String): String {
        val sourceFile = getFileForDocId(documentId)
        val sourceParent = sourceFile.parentFile
        if (sourceParent == null) throw FileNotFoundException("Cannot rename root")
        val targetFile = File("${getDocIdForFile(sourceParent)}/$displayName")
        if (!sourceFile.renameTo(targetFile)) {
            throw FileNotFoundException("Couldn't rename the document with id $documentId")
        }
        return getDocIdForFile(targetFile)
    }

    /**
     * ドキュメントを移動する。
     * @param sourceDocumentId 移動元ドキュメントID
     * @param sourceParentDocumentId 移動元親ドキュメントID
     * @param targetParentDocumentId 移動先親ドキュメントID
     * @return 新しいドキュメントID
     * @throws FileNotFoundException 移動失敗時
     */
    @Throws(FileNotFoundException::class)
    override fun moveDocument(sourceDocumentId: String, sourceParentDocumentId: String, targetParentDocumentId: String): String {
        val sourceFile = getFileForDocId(sourceParentDocumentId + sourceDocumentId)
        val targetFile = File(targetParentDocumentId + sourceDocumentId)
        if (!sourceFile.renameTo(targetFile)) {
            throw FileNotFoundException("Failed to move the document with id ${sourceFile.path}")
        }
        return getDocIdForFile(targetFile)
    }

    /**
     * 子ドキュメントを削除する。
     * @param documentId ドキュメントID
     * @param parentDocumentId 親ドキュメントID
     * @throws FileNotFoundException 削除失敗時
     */
    @Throws(FileNotFoundException::class)
    override fun removeDocument(documentId: String, parentDocumentId: String) {
        deleteDocument("$parentDocumentId/$documentId")
    }

    /**
     * ドキュメントを削除する。
     * @param documentId ドキュメントID
     * @throws FileNotFoundException 削除失敗時
     */
    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        val file = getFileForDocId(documentId)
        if (file.isDirectory) {
            try {
                FileUtils.deleteDirectory(file)
            } catch (e: IOException) {
                throw FileNotFoundException("Failed to delete document with id $documentId")
            }
        } else {
            if (!file.delete()) {
                throw FileNotFoundException("Failed to delete document with id $documentId")
            }
        }
        notifyChange(createUriForFile(file.parentFile!!))
    }

    /**
     * ドキュメントのMIMEタイプを取得する。
     * @param documentId ドキュメントID
     * @return MIMEタイプ文字列
     * @throws FileNotFoundException ドキュメントが見つからない場合
     */
    @Throws(FileNotFoundException::class)
    override fun getDocumentType(documentId: String): String {
        Logging.i("FolderPRovider", "getDocumentType($documentId)")
        val file = getFileForDocId(documentId)
        return getMimeType(file)
    }

    /**
     * ドキュメントを検索する。
     * @param rootId ルートID
     * @param query 検索クエリ
     * @param projection 取得するカラム
     * @return 検索結果カーソル
     * @throws FileNotFoundException ルートが見つからない場合
     */
    @Throws(FileNotFoundException::class)
    override fun querySearchDocuments(rootId: String, query: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(rootId)

        val pending = LinkedList<File>()
        pending.add(parent)

        val MAX_SEARCH_RESULTS = 50
        while (!pending.isEmpty() && result.count < MAX_SEARCH_RESULTS) {
            val file = pending.removeFirst()
            val isInsideHome: Boolean = try {
                file.canonicalPath.startsWith(PathManager.DIR_GAME_HOME)
            } catch (e: IOException) {
                true
            }
            if (isInsideHome) {
                if (file.isDirectory) {
                    val listing = file.listFiles()
                    if (listing != null) pending.addAll(listing)
                } else {
                    if (file.name.lowercase().contains(query)) {
                        includeFile(result, null, file)
                    }
                }
            }
        }

        return result
    }

    /**
     * 指定されたドキュメントが親の子かどうかを判定する。
     * @param parentDocumentId 親ドキュメントID
     * @param documentId 判定対象のドキュメントID
     * @return 子の場合は true
     */
    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    /**
     * ドキュメントのパスを検索する。
     * @param parentDocumentId 親ドキュメントID（省略可）
     * @param childDocumentId 子ドキュメントID
     * @return パス情報
     * @throws FileNotFoundException ドキュメントが見つからない場合
     */
    @Throws(FileNotFoundException::class)
    override fun findDocumentPath(@Nullable parentDocumentId: String?, childDocumentId: String): DocumentsContract.Path {
        var source = BASE_DIR!!
        if (parentDocumentId != null) source = getFileForDocId(parentDocumentId)
        var destination = getFileForDocId(childDocumentId)
        val pathIds = mutableListOf<String>()
        while (source != destination && destination != null) {
            pathIds.add(getDocIdForFile(destination))
            destination = destination.parentFile
        }
        pathIds.add(getDocIdForFile(source))
        pathIds.reverse()
        Logging.i("FolderProvider", pathIds.toString())
        return DocumentsContract.Path(getDocIdForFile(source), pathIds)
    }

    /**
     * ファイルのMIMEタイプを判定する。
     * @param file 判定対象ファイル
     * @return MIMEタイプ文字列
     */
    private fun getMimeType(file: File): String {
        if (file.isDirectory) {
            return Document.MIME_TYPE_DIR
        } else {
            val name = file.name
            val lastDot = name.lastIndexOf('.')
            if (lastDot >= 0) {
                val extension = name.substring(lastDot + 1).lowercase()
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (mime != null) return mime
            }
            return "application/octet-stream"
        }
    }

    /**
     * カーソルに行を追加する。
     * @param result 出力カーソル
     * @param docIdArg ドキュメントID（省略可）
     * @param fileArg ファイルオブジェクト（省略可）
     * @throws FileNotFoundException ファイルが見つからない場合
     */
    @Throws(FileNotFoundException::class)
    private fun includeFile(result: MatrixCursor, docIdArg: String?, fileArg: File?) {
        var docId = docIdArg
        var file = fileArg
        if (docId == null) {
            docId = getDocIdForFile(file!!)
        } else {
            file = getFileForDocId(docId)
        }

        var flags = 0
        if (file.isDirectory) {
            if (file.canWrite()) flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else if (file.canWrite()) {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }
        val parent = file.parentFile
        if (parent != null) {
            if (parent.canWrite()) flags = flags or Document.FLAG_SUPPORTS_DELETE
        }

        val displayName = file.name
        val mimeType = getMimeType(file)
        if (mimeType.startsWith("image/")) flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL

        val row = result.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, docId)
        row.add(Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(Document.COLUMN_SIZE, file.length())
        row.add(Document.COLUMN_MIME_TYPE, mimeType)
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
        row.add(Document.COLUMN_FLAGS, flags)
        row.add(Document.COLUMN_ICON, R.mipmap.ic_launcher)
    }

    /** ファイルの絶対パスをドキュメントIDとして返す。 @param file ファイル @return ドキュメントID */
    private fun getDocIdForFile(file: File): String = file.absolutePath

    /**
     * ドキュメントIDからファイルを取得する。
     * @param docId ドキュメントID
     * @return ファイルオブジェクト
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    @Throws(FileNotFoundException::class)
    private fun getFileForDocId(docId: String): File {
        val f = File(docId)
        if (!f.exists()) throw FileNotFoundException("${f.absolutePath} not found")
        return f
    }

    /**
     * ドキュメントIDからURIを生成する。
     * @param documentId ドキュメントID
     * @return 生成されたURI
     * @throws FileNotFoundException ファイルが見つからない場合
     */
    @Throws(FileNotFoundException::class)
    private fun createUriForDocId(documentId: String): Uri = createUriForFile(getFileForDocId(documentId))

    /** ファイルからコンテンツURIを生成する。 @param file ファイル @return コンテンツURI */
    private fun createUriForFile(file: File): Uri =
        DocumentsContract.buildDocumentUri(mStorageProviderAuthortiy, file.absolutePath)

    /** URIの変更を通知する。 @param uri 通知するURI */
    private fun notifyChange(uri: Uri) {
        mContentResolver!!.notifyChange(uri, null)
    }

    companion object {
        /** 全てのMIMEタイプにマッチするワイルドカード。 */
        private const val ALL_MIME_TYPES = "*/*"

        /** ルート問い合わせのデフォルトプロジェクション。 */
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        /** ドキュメント問い合わせのデフォルトプロジェクション。 */
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )
    }
}