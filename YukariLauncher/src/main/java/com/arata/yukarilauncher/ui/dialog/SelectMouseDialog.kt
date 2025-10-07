package com.arata.yukarilauncher.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.ui.subassembly.filelist.FileIcon
import com.arata.yukarilauncher.ui.subassembly.filelist.FileItemBean
import com.arata.yukarilauncher.ui.subassembly.filelist.FileRecyclerViewCreator
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.file.FileTools.Companion.mkdirs
import com.arata.yukarilauncher.utils.image.ImageUtils.Companion.isImage
import java.io.File

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

    private fun initView(mMouseListView: RecyclerView) {
        FileRecyclerViewCreator(
            context,
            mMouseListView,
            { position: Int, fileItemBean: FileItemBean ->
                val file = fileItemBean.file
                file?.apply {
                    if (exists() && isImage(this)) {
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

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getItems(): MutableList<FileItemBean> {
        val fileItemBeans = FileRecyclerViewCreator.loadItemBeansFromPath(
            context,
            mousePath(),
            FileIcon.FILE,
            showFile = true,
            showFolder = false
        )
        fileItemBeans.add(0, FileItemBean(
            context.getString(R.string.custom_mouse_default),
            context.getDrawable(R.drawable.ic_mouse_pointer)
        ))
        return fileItemBeans
    }

    private fun mousePath(): File {
        val path = File(PathManager.DIR_CUSTOM_MOUSE)
        if (!path.exists()) mkdirs(path)
        return path
    }

    interface MouseSelectedListener {
        fun onSelectedListener()
    }
}
