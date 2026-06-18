package com.arata.yukarilauncher.ui.subassembly.filelist

import android.graphics.drawable.Drawable
import com.arata.yukarilauncher.utils.stringutils.SortStrings.Companion.compareChar
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.Date

/**
 * ファイル一覧のアイテムデータを保持するBeanクラス
 */
class FileItemBean(
    @JvmField val name: String,
    @JvmField val date: Date?,
    @JvmField val size: Long?
) : Comparable<FileItemBean?> {
    @JvmField var image: Drawable? = null
    @JvmField var file: File? = null
    @JvmField var isHighlighted: Boolean = false
    @JvmField var isCanCheck: Boolean = true

    /**
     * ファイル情報からアイテムを構築する
     */
    constructor(file: File) : this(
        file.name,
        Date(file.lastModified()),
        if (file.isFile) FileUtils.sizeOf(file) else null
    ) {
        this.file = file
    }

    /**
     * 名前とアイコンからアイテムを構築します。
     */
    constructor(name: String, image: Drawable?) : this(name, null as Date?, null) {
        this.image = image
    }

    /**
     * 名前、日付、アイコンからアイテムを構築します。
     */
    constructor(name: String, date: Date, image: Drawable?) : this(name, date, null as Long?) {
        this.image = image
    }

    /**
     * 指定されたアイテムと比較します（ディレクトリ優先、大文字小文字を区別しない）。
     */
    override fun compareTo(other: FileItemBean?): Int {
        other ?: run { throw NullPointerException("Cannot compare to null.") }

        val thisName = file?.name ?: name
        val otherName = other.file?.name ?: other.name

        if (this.file != null && file!!.isDirectory) {
            if (other.file != null && !other.file!!.isDirectory) {
                return -1
            }
        } else if (other.file != null && other.file!!.isDirectory) {
            return 1
        }

        return compareChar(thisName, otherName)
    }

    /**
     * アイテムの文字列表現を返します。
     */
    override fun toString(): String {
        return "FileItemBean{" +
                "file=" + file +
                ", name='" + name + '\'' +
                '}'
    }
}