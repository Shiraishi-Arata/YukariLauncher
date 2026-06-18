package com.arata.yukarilauncher.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.NonNull
import androidx.annotation.Nullable

/** 特定の拡張子のファイルを開くためのActivityResultContract。 */
class OpenDocumentWithExtension : ActivityResultContract<Any, List<Uri>> {
    /** 対象のMIMEタイプ。 */
    private val mimeType: String
    /** 複数ファイル選択を許可するか。 */
    private val allowMultiple: Boolean

    /** 拡張子のみ指定してインスタンスを生成する。複数選択は不可。 @param extension 対象のファイル拡張子 */
    constructor(extension: String?) : this(extension, false)

    /** 拡張子と複数選択フラグを指定してインスタンスを生成する。 @param extension 対象のファイル拡張子 @param allowMultiple 複数選択を許可するか */
    constructor(extension: String?, allowMultiple: Boolean) {
        var extensionMimeType: String? = null
        if (extension != null) {
            extensionMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        if (extensionMimeType == null) extensionMimeType = "*/*"
        mimeType = extensionMimeType
        this.allowMultiple = allowMultiple
    }

    /** ファイル選択Intentを生成する。 @param context コンテキスト @param input 入力（未使用） @return ACTION_OPEN_DOCUMENT Intent */
    @NonNull
    override fun createIntent(@NonNull context: Context, @NonNull input: Any): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType

        if (allowMultiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        return intent
    }

    /** 同期結果は常にnullを返す（非同期のみ）。 */
    @Nullable
    override fun getSynchronousResult(@NonNull context: Context, @NonNull input: Any): SynchronousResult<List<Uri>>? {
        return null
    }

    /** Intentの結果をUriのリストとしてパースする。 @param resultCode 結果コード @param intent 結果Intent @return 選択されたファイルのUriリスト、失敗時はnull */
    @Nullable
    override fun parseResult(resultCode: Int, @Nullable intent: Intent?): List<Uri> {
        if (intent == null || resultCode != Activity.RESULT_OK) return emptyList()

        val uris = mutableListOf<Uri>()
        if (intent.clipData != null) {
            for (i in 0 until intent.clipData!!.itemCount) {
                uris.add(intent.clipData!!.getItemAt(i).uri)
            }
        } else {
            uris.add(intent.data!!)
        }

        return uris
    }
}