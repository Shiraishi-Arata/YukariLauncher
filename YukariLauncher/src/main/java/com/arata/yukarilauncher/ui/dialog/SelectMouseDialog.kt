package com.arata.yukarilauncher.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.ui.subassembly.filelist.FileIcon
import com.arata.yukarilauncher.ui.subassembly.filelist.FileItemBean
import com.arata.yukarilauncher.ui.subassembly.filelist.FileRecyclerViewCreator
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.file.FileTools.Companion.mkdirs
import java.io.File

/**
 * カスタムマウスカーソル選択ダイアログ
 */
class SelectMouseDialog(
    context: Context,
    private val listener: MouseSelectedListener
) : AbstractSelectDialog(context) {

    override fun initDialog(recyclerView: RecyclerView) {
        initView(recyclerView)
        setTitleText(R.string.custom_mouse_title)
        setMessageText(
            context.getString(R.string.custom_mouse_dialog,
                context.getString(R.string.setting_category_control),
                context.getString(R.string.custom_mouse_title)
            )
        )
    }

    /**
     * ビューを初期化する
     */
    private fun initView(mMouseListView: RecyclerView) {
        FileRecyclerViewCreator(
            context,
            mMouseListView,
            { position: Int, fileItemBean: FileItemBean ->
                val file = fileItemBean.file
                file?.apply {
                    if (exists() && YLTools.isSupportedMouseSource(this)) {
                        AllSettings.customMouse.put(name).save()
                        listener.onSelectedListener()
                        dismiss()
                    }
                }
                if (position == 0) {
                    AllSettings.customMouse.put("").save()
                    listener.onSelectedListener()
                    this.dismiss()
                }
            },
            null,
            getItems()
        )
    }

    /**
     * マウスファイルのアイテムリストを取得する
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getItems(): MutableList<FileItemBean> {
        val fileItemBeans = FileRecyclerViewCreator.loadItemBeansFromPath(
            context,
            mousePath(),
            FileIcon.FILE,
            showFile = true,
            showFolder = true
        )
        fileItemBeans.add(0, FileItemBean(
            context.getString(R.string.custom_mouse_default),
            context.getDrawable(R.drawable.ic_mouse_pointer)
        ))
        return fileItemBeans
    }

    /**
     * カスタムマウスファイルのパスを取得する
     */
    private fun mousePath(): File {
        val path = File(PathManager.DIR_CUSTOM_MOUSE)
        if (!path.exists()) mkdirs(path)
        return path
    }

    /**
     * マウス選択のコールバックインターフェース
     */
    interface MouseSelectedListener {
        fun onSelectedListener()
    }
}
