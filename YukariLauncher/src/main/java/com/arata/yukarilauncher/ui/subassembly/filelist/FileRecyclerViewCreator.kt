package com.arata.yukarilauncher.ui.subassembly.filelist

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.mod.ModUtils
import com.arata.yukarilauncher.ui.subassembly.filelist.FileRecyclerAdapter.OnMultiSelectListener
import com.arata.yukarilauncher.utils.stringutils.StringFilter.Companion.containsSubstring
import java.io.File
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import java.util.zip.ZipFile
import org.json.JSONObject

/**
 * ファイル一覧のRecyclerView表示を管理するクラス。
 * @param context コンテキスト
 * @param recyclerView 表示に使用するRecyclerView
 * @param onItemClickListener アイテムクリックリスナー
 * @param onItemLongClickListener アイテム長押しリスナー
 * @param data 初期データリスト
 */
class FileRecyclerViewCreator(
    context: Context?,
    recyclerView: RecyclerView,
    onItemClickListener: FileRecyclerAdapter.OnItemClickListener?,
    onItemLongClickListener: FileRecyclerAdapter.OnItemLongClickListener?,
    data: MutableList<FileItemBean> = ArrayList()
) {
    @JvmField
    val fileRecyclerAdapter: FileRecyclerAdapter = FileRecyclerAdapter()
    private val mainRecyclerView: RecyclerView
    private val itemBeans: MutableList<FileItemBean> = mutableListOf()
    private var filterString: String? = null

    init {
        fileRecyclerAdapter.setOnItemClickListener(onItemClickListener)
        fileRecyclerAdapter.setOnItemLongClickListener(onItemLongClickListener)

        this.mainRecyclerView = recyclerView

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
        mainRecyclerView.layoutAnimation = LayoutAnimationController(
            AnimationUtils.loadAnimation(
                context,
                R.anim.fade_downwards
            )
        )
        mainRecyclerView.layoutManager = layoutManager
        mainRecyclerView.adapter = this.fileRecyclerAdapter

        if (data.isNotEmpty()) loadData(data)
    }

    /**
     * ファイル一覧データを読み込む
     */
    @SuppressLint("NotifyDataSetChanged")
    fun loadData(itemBeans: List<FileItemBean>) {
        this.itemBeans.apply {
            clear()
            addAll(itemBeans)
        }
        updateWithFilter()
    }

    /**
     * フィルター文字列を設定する
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setFilterString(string: String?) {
        this.filterString = string
        updateWithFilter()
    }

    private fun updateWithFilter() {
        fileRecyclerAdapter.updateItems(
            filterString?.takeIf { it.isNotEmpty() }?.let { string ->
                itemBeans.filter {
                    it.name.contains(string, true)
                }
            } ?: itemBeans
        )
        mainRecyclerView.scheduleLayoutAnimation()
    }

    /**
     * マルチ選択リスナーを設定する
     */
    fun setOnMultiSelectListener(listener: OnMultiSelectListener?) {
        fileRecyclerAdapter.setOnMultiSelectListener(listener)
    }

    /**
     * ファイルが存在しないかどうかを返す
     */
    fun isNoFile(): Boolean {
        return fileRecyclerAdapter.isNoFile
    }

    companion object {
        private const val ICON_SCAN_ENTRY_LIMIT = 256
        private const val ICON_SCAN_MAX_SIZE_BYTES = 512 * 1024L

        /**
         * パスからファイルアイテムのリストを読み込む
         */
        fun loadItemBeansFromPath(context: Context, path: File, fileIcon: FileIcon,
            showFile: Boolean, showFolder: Boolean
        ): MutableList<FileItemBean> {
            return loadItemBeansFromPath(
                context,
                null,
                showSearchResultsOnly = false,
                caseSensitive = false,
                null,
                path,
                fileIcon,
                showFile,
                showFolder
            )
        }

        /**
         * パスからフィルタリング付きでファイルアイテムのリストを読み込む
         */
        @JvmStatic
        @SuppressLint("UseCompatLoadingForDrawables")
        fun loadItemBeansFromPath(
            context: Context,
            filterString: String?,
            showSearchResultsOnly: Boolean,
            caseSensitive: Boolean,
            searchCount: AtomicInteger?,
            path: File,
            fileIcon: FileIcon,
            showFile: Boolean,
            showFolder: Boolean
        ): MutableList<FileItemBean> {
            val itemBeans: MutableList<FileItemBean> = ArrayList()
            val files = path.listFiles()
            if (files != null) {
                val resources = context.resources
                for (file in files) {
                    if (!showFileOrFolder(file, showFile, showFolder)) continue

                    val itemBean = FileItemBean(file)
                    if (!filterString.isNullOrEmpty()) {
                        if (containsSubstring(file.name, filterString, caseSensitive)) {
                            itemBean.isHighlighted = true
                            searchCount?.addAndGet(1)
                        } else if (showSearchResultsOnly) {
                            continue
                        }
                    }
                    itemBean.image = getIcon(context, file, fileIcon, resources)
                    itemBeans.add(itemBean)
                }
            }
            return itemBeans
        }

        /**
         * ファイル/フォルダの表示可否を判定する
         */
        private fun showFileOrFolder(file: File, showFile: Boolean, showFolder: Boolean): Boolean {
            if (file.isDirectory && !showFolder) return false
            return !file.isFile || showFile
        }

        /**
         * ファイルに対応するアイコンを取得する
         */
        private fun getIcon(context: Context, file: File, fileIcon: FileIcon, resources: Resources): Drawable? {
            return if (file.isFile) {
                when (fileIcon) {
                    FileIcon.MOD -> if (file.name.endsWith(ModUtils.DISABLE_JAR_FILE_SUFFIX)) {
                        ContextCompat.getDrawable(context, R.drawable.ic_disabled)
                    } else if (isModArchive(file.name)) {
                        getModArchiveIcon(context, file)
                            ?: ContextCompat.getDrawable(context, R.drawable.ic_profile_mods)
                    } else {
                        getFileIcon(file, resources)
                    }

                    FileIcon.FILE -> ContextCompat.getDrawable(context, R.drawable.ic_file)
                }
            } else {
                ContextCompat.getDrawable(context, R.drawable.ic_folder)
            }
        }

        /**
         * ファイル名がModアーカイブかどうかを判定します。
         */
        private fun isModArchive(fileName: String): Boolean {
            return fileName.endsWith(ModUtils.JAR_FILE_SUFFIX) ||
                    fileName.endsWith(ModUtils.DISABLE_JAR_FILE_SUFFIX)
        }

        /**
         * メタデータからModアイコンパスを読み取る
         */
        private fun getIconPathFromMetadata(zip: ZipFile): String? {
            val fabricEntry = zip.getEntry("fabric.mod.json")
            if (fabricEntry != null) {
                zip.getInputStream(fabricEntry).use { input ->
                    val jsonString = input.bufferedReader().readText()
                    try {
                        val json = JSONObject(jsonString)
                        val iconPath = json.optString("icon", null)
                        if (!iconPath.isNullOrEmpty()) {
                            return normalizeIconPath(iconPath)
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            fun parseTomlIconPath(entryName: String): String? {
                val entry = zip.getEntry(entryName) ?: return null
                zip.getInputStream(entry).use { input ->
                    val content = input.bufferedReader().readText()
                    val pattern = Pattern.compile("logoFile\\s*=\\s*\"([^\"]+)\"")
                    val matcher = pattern.matcher(content)
                    if (matcher.find()) {
                        return normalizeIconPath(matcher.group(1))
                    }
                }
                return null
            }

            parseTomlIconPath("META-INF/mods.toml")?.let { return it }
            parseTomlIconPath("META-INF/neoforge.mods.toml")?.let { return it }

            return null
        }

        /**
         * アイコンパスを正規化する（先頭のスラッシュを除去）
         */
        private fun normalizeIconPath(path: String): String {
            return path.trimStart('/')
        }

        /**
         * Modアーカイブからアイコンを取得する
         */
        private fun getModArchiveIcon(context: Context, file: File): Drawable? {
            return try {
                ZipFile(file).use { zip ->
                    val metadataIconPath = getIconPathFromMetadata(zip)
                    if (metadataIconPath != null) {
                        val entry = zip.getEntry(metadataIconPath)
                        if (entry != null && !entry.isDirectory && entry.size <= ICON_SCAN_MAX_SIZE_BYTES) {
                            zip.getInputStream(entry).use { input ->
                                BitmapFactory.decodeStream(input)?.let { bitmap ->
                                    return BitmapDrawable(context.resources, bitmap)
                                }
                            }
                        }
                    }

                    val entry = zip.entries().asSequence()
                        .take(ICON_SCAN_ENTRY_LIMIT)
                        .filter { !it.isDirectory && it.name.endsWith(".png", ignoreCase = true) }
                        .filter { it.size in 1..ICON_SCAN_MAX_SIZE_BYTES }
                        .minByOrNull { scoreIconEntry(it.name) }
                        ?: return null

                    zip.getInputStream(entry).use { input ->
                        BitmapFactory.decodeStream(input)?.let { bitmap ->
                            BitmapDrawable(context.resources, bitmap)
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

        /**
         * エントリ名からアイコンのスコアを計算する
         */
        private fun scoreIconEntry(name: String): Int {
            val lower = name.lowercase()
            return when {
                lower.endsWith("assets/icon.png") -> 0
                lower.endsWith("icon.png") -> 1
                lower.endsWith("logo.png") -> 2
                lower.endsWith("pack.png") -> 3
                else -> 10
            }
        }

        /**
         * 名前と日付のペアからファイルアイテムリストを読み込む
         */
        @JvmStatic
        fun loadItemBean(drawable: Drawable, namesPair: Array<Pair<String, Date>>): List<FileItemBean> {
            val itemBeans: MutableList<FileItemBean> = ArrayList()
            for (pair in namesPair) {
                itemBeans.add(FileItemBean(pair.first, pair.second, drawable))
            }
            return itemBeans
        }

        /**
         * ファイルタイプに応じたデフォルトアイコンを取得する
         */
        @SuppressLint("UseCompatLoadingForDrawables")
        private fun getFileIcon(file: File, resources: Resources): Drawable {
            return if (file.isDirectory) {
                resources.getDrawable(R.drawable.ic_folder, resources.newTheme())
            } else {
                resources.getDrawable(R.drawable.ic_file, resources.newTheme())
            }
        }
    }
}