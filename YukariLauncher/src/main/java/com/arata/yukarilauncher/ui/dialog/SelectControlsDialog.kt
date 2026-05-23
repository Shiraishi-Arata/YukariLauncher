package com.arata.yukarilauncher.ui.dialog

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlSelectedListener
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlsListViewCreator
import java.io.File

/**
 * コントロール設定選択ダイアログ
 */
class SelectControlsDialog(
    context: Context,
    private val selectedListener: SelectedListener
) : AbstractSelectDialog(context) {

    override fun initDialog(recyclerView: RecyclerView) {
        ControlsListViewCreator(context, recyclerView).apply {
            listAtPath()
            setSelectedListener(object : ControlSelectedListener() {
                override fun onItemSelected(file: File) {
                    selectedListener.onSelected(file)
                    dismiss()
                }

                override fun onItemLongClick(file: File) {
                }
            })
        }
    }

    /**
     * 選択結果のコールバックインターフェース
     */
    interface SelectedListener {
        fun onSelected(file: File)
    }
}
