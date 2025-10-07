package com.arata.yukarilauncher.ui.dialog

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.version.favorites.FavoritesVersionUtils
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.subassembly.version.FavoritesVersionAdapter

class FavoritesVersionDialog(
    context: Context,
    private val versionName: String,
    private val favoritesChanged: () -> Unit
) : AbstractSelectDialog(context) {
    private val mFavoritesAdapter = FavoritesVersionAdapter(versionName)

    override fun initDialog(recyclerView: RecyclerView) {
        setTitleText(R.string.version_manager_favorites_dialog_title)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = mFavoritesAdapter
    }

    override fun dismiss() {
        super.dismiss()
        Task.runTask {
            FavoritesVersionUtils.updateVersionFolders(versionName, mFavoritesAdapter.getSelectedCategorySet())
        }.ended(TaskExecutors.getAndroidUI()) {
            favoritesChanged()
        }.execute()
    }
}
