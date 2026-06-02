package com.arata.yukarilauncher.ui.dialog

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.utils.runtime.RuntimeSelectedListener
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils
import com.arata.yukarilauncher.utils.runtime.RTRecyclerViewAdapter
import com.arata.yukarilauncher.utils.runtime.Runtime

/**
 * Javaランタイム選択ダイアログ
 */
class SelectRuntimeDialog(
    context: Context,
    private val listener: RuntimeSelectedListener
) : AbstractSelectDialog(context) {

    override fun initDialog(recyclerView: RecyclerView) {
        setCancelable(false)
        setTitleText(R.string.install_select_jre_environment)
        setMessageText(R.string.install_recommend_use_jre8)

        val runtimes: MutableList<Runtime> = ArrayList(MultiRTUtils.runtimes)
        if (runtimes.isNotEmpty()) runtimes.add(Runtime("auto"))
        val adapter = RTRecyclerViewAdapter(runtimes, listener, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
}