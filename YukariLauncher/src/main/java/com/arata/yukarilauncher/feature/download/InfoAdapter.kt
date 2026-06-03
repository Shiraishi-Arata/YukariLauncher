package com.arata.yukarilauncher.feature.download

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.flexbox.FlexboxLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.ItemDownloadInfoBinding
import com.arata.yukarilauncher.databinding.ItemDownloadGridBinding
import com.arata.yukarilauncher.event.value.DownloadPageEvent
import com.arata.yukarilauncher.feature.download.enums.Category
import com.arata.yukarilauncher.feature.download.enums.ModLoader
import com.arata.yukarilauncher.feature.download.enums.Platform
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.item.ModInfoItem
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.fragment.DownloadModFragment
import com.arata.yukarilauncher.utils.NumberWithUnits
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import com.arata.yukarilauncher.Tools
import org.greenrobot.eventbus.EventBus
import org.jackhuang.hmcl.ui.versions.ModTranslations
import java.util.Collections
import java.util.Locale
import java.util.StringJoiner
import java.util.TimeZone
import java.util.WeakHashMap

class InfoAdapter(
    private val parentFragment: Fragment?,
    private val listener: CallSearchListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mViewHolderSet: MutableSet<RecyclerView.ViewHolder> = Collections.newSetFromMap(WeakHashMap<RecyclerView.ViewHolder, Boolean>())
    private var mItems: MutableList<InfoItem> = ArrayList()
    private var originalItems: MutableList<InfoItem> = ArrayList()
    private val selectedProjectIds: MutableSet<String> = HashSet()
    private var selectMode = false
    private var selectListener: ((InfoItem) -> Unit)? = null
    private var isGridView = false

    fun setGridView(grid: Boolean) {
        if (isGridView == grid) return
        isGridView = grid
        notifyDataSetChanged()
    }

    fun isGridView() = isGridView

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        val view: View
        when (viewType) {
            VIEW_TYPE_LIST_ITEM -> {
                return ListViewHolder(ItemDownloadInfoBinding.inflate(layoutInflater, viewGroup, false))
            }
            VIEW_TYPE_GRID_ITEM -> {
                return GridViewHolder(ItemDownloadGridBinding.inflate(layoutInflater, viewGroup, false))
            }
            VIEW_TYPE_LOADING -> {
                view = layoutInflater.inflate(R.layout.view_loading, viewGroup, false)
                return LoadingViewHolder(view)
            }
            else -> throw RuntimeException("Unimplemented view type!")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_LIST_ITEM -> (holder as ListViewHolder).setStateLimited(mItems[position])
            VIEW_TYPE_GRID_ITEM -> (holder as GridViewHolder).setStateLimited(mItems[position])
            VIEW_TYPE_LOADING -> listener.loadMoreResult()
            else -> throw RuntimeException("Unimplemented view type!")
        }
    }

    override fun getItemCount(): Int {
        if (listener.isLastPage() || mItems.isEmpty()) return mItems.size
        return mItems.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position < mItems.size) {
            return if (isGridView) VIEW_TYPE_GRID_ITEM else VIEW_TYPE_LIST_ITEM
        }
        return VIEW_TYPE_LOADING
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(item: List<InfoItem>) {
        originalItems = item.toMutableList()
        rebuildDisplayItems()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectMode(enabled: Boolean, listener: ((InfoItem) -> Unit)?) {
        selectMode = enabled
        selectListener = listener
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedProjectIds(ids: Set<String>) {
        selectedProjectIds.clear()
        selectedProjectIds.addAll(ids)
        rebuildDisplayItems()
        notifyDataSetChanged()
    }

    private fun rebuildDisplayItems() {
        mItems = originalItems.toMutableList()
    }

    inner class ListViewHolder(val binding: ItemDownloadInfoBinding) : RecyclerView.ViewHolder(binding.root) {
        private val mContext = binding.root.context
        private var item: InfoItem? = null

        init {
            mViewHolderSet.add(this)
        }

        @SuppressLint("CheckResult")
        fun setStateLimited(item: InfoItem) {
            this.item = item
            val mod = ModTranslations.getTranslationsByRepositoryType(item.classify)
                .getModByCurseForgeId(item.slug)

            binding.apply {
                parentFragment?.let { fragment ->
                    root.setOnClickListener {
                        if (selectMode) {
                            selectListener?.invoke(item)
                            return@setOnClickListener
                        }
                        EventBus.getDefault().post(DownloadPageEvent.RecyclerEnableEvent(false))

                        val infoViewModel = ViewModelProvider(fragment.requireActivity())[InfoViewModel::class.java]
                        infoViewModel.infoItem = item.copy()
                        infoViewModel.platformHelper = item.platform.helper.copy()

                        YLTools.swapFragmentWithAnim(fragment, DownloadModFragment::class.java, DownloadModFragment.TAG, null)
                    }
                }
                val isSelected = selectedProjectIds.contains(item.projectId)
                selectCorner.visibility = View.GONE
                root.foreground = if (isSelected) {
                    ContextCompat.getDrawable(mContext, R.drawable.bg_select_corner)
                } else null

                val displayTitle = if (YLTools.areaChecks("zh")) {
                    mod?.displayName ?: item.title
                } else {
                    item.title
                }
                val authorName = item.author?.joinToString(", ") ?: ""
                titleTextview.text = if (authorName.isNotEmpty()) {
                    "$displayTitle by $authorName"
                } else {
                    displayTitle
                }
                descriptionTextview.text = item.description

                val envCategories = item.category.filter { it == Category.ENV_CLIENT || it == Category.ENV_SERVER }
                val otherCategories = item.category.filter { it != Category.ENV_CLIENT && it != Category.ENV_SERVER }

                tagsLayout.removeAllViews()

                otherCategories.forEach { cat ->
                    val tv = TextView(mContext)
                    tv.text = mContext.getString(cat.resNameID)
                    tv.background = ContextCompat.getDrawable(mContext, R.drawable.background_mod_category)
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
                    val lp = FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, Tools.dpToPx(10f).toInt(), 0)
                    tv.layoutParams = lp
                    tagsLayout.addView(tv)
                }

                envCategories.forEach { env ->
                    val tv = TextView(mContext)
                    tv.text = mContext.getString(env.resNameID)
                    tv.background = ContextCompat.getDrawable(mContext, R.drawable.background_mod_category)
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
                    val lp = FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, Tools.dpToPx(10f).toInt(), 0)
                    tv.layoutParams = lp
                    tagsLayout.addView(tv)
                }

                if (item is ModInfoItem) {
                    item.modloaders.forEach { loader ->
                        val tv = TextView(mContext)
                        tv.text = loader.loaderName
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
                        tv.background = ContextCompat.getDrawable(mContext, R.drawable.background_mod_category)
                        tv.backgroundTintList = ContextCompat.getColorStateList(mContext, loader.tagColor)
                        val lp = FlexboxLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        lp.setMargins(0, 0, Tools.dpToPx(10f).toInt(), 0)
                        tv.layoutParams = lp
                        tagsLayout.addView(tv)
                    }
                }

                val downloadCount = NumberWithUnits.formatNumberWithUnit(item.downloadCount, YLTools.isEnglish(mContext))
                downloadCountTextview.text = StringUtils.insertSpace(mContext.getString(R.string.download_info_downloads), downloadCount)
                val displayDate = item.updatedDate ?: item.uploadDate
                dateTextview.text = StringUtils.formatDate(displayDate, Locale.getDefault(), TimeZone.getDefault())

                item.iconUrl?.apply {
                    Glide.with(mContext).load(this).apply {
                        if (!AllSettings.resourceImageCache.getValue()) diskCacheStrategy(DiskCacheStrategy.NONE)
                    }.into(iconImageview)
                }
            }
            binding.tagsLayout
        }

        private fun getPlatformIcon(platform: Platform): Drawable? {
            return when (platform) {
                Platform.MODRINTH -> ContextCompat.getDrawable(mContext, R.drawable.ic_modrinth)
                Platform.CURSEFORGE -> ContextCompat.getDrawable(mContext, R.drawable.ic_curseforge)
            }
        }

        private fun addCategoryView(layout: FlexboxLayout, text: String) {
            val textView = createCategoryView(mContext)
            textView.text = text
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
            layout.addView(textView)
        }
    }

    inner class GridViewHolder(val binding: ItemDownloadGridBinding) : RecyclerView.ViewHolder(binding.root) {
        private val mContext = binding.root.context
        private var item: InfoItem? = null

        init {
            mViewHolderSet.add(this)
        }

        @SuppressLint("CheckResult")
        fun setStateLimited(item: InfoItem) {
            this.item = item
            val mod = ModTranslations.getTranslationsByRepositoryType(item.classify)
                .getModByCurseForgeId(item.slug)

            binding.apply {
                parentFragment?.let { fragment ->
                    root.setOnClickListener {
                        if (selectMode) {
                            selectListener?.invoke(item)
                            return@setOnClickListener
                        }
                        EventBus.getDefault().post(DownloadPageEvent.RecyclerEnableEvent(false))

                        val infoViewModel = ViewModelProvider(fragment.requireActivity())[InfoViewModel::class.java]
                        infoViewModel.infoItem = item.copy()
                        infoViewModel.platformHelper = item.platform.helper.copy()

                        YLTools.swapFragmentWithAnim(fragment, DownloadModFragment::class.java, DownloadModFragment.TAG, null)
                    }
                }
                val isSelected = selectedProjectIds.contains(item.projectId)
                selectCorner.visibility = if (isSelected) View.VISIBLE else View.GONE
                root.foreground = if (isSelected) {
                    ContextCompat.getDrawable(mContext, R.drawable.bg_select_corner)
                } else null

                val displayTitle = if (YLTools.areaChecks("zh")) {
                    mod?.displayName ?: item.title
                } else {
                    item.title
                }
                val authorName = item.author?.joinToString(", ") ?: ""
                titleTextview.text = if (authorName.isNotEmpty()) {
                    "$displayTitle by $authorName"
                } else {
                    displayTitle
                }
                descriptionTextview.text = item.description
                platformImageview.setImageDrawable(getPlatformIcon(item.platform))

                val envCategories = item.category.filter { it == Category.ENV_CLIENT || it == Category.ENV_SERVER }
                val otherCategories = item.category.filter { it != Category.ENV_CLIENT && it != Category.ENV_SERVER }

                tagsLayout.removeAllViews()

                otherCategories.forEach { cat ->
                    addCategoryView(tagsLayout, mContext.getString(cat.resNameID))
                }

                envCategories.forEach { env ->
                    val tv = TextView(mContext)
                    tv.text = mContext.getString(env.resNameID)
                    tv.background = ContextCompat.getDrawable(mContext, R.drawable.background_mod_category)
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
                    val lp = FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, Tools.dpToPx(10f).toInt(), 0)
                    tv.layoutParams = lp
                    tagsLayout.addView(tv)
                }

                if (item is ModInfoItem) {
                    item.modloaders.forEach { loader ->
                        val tv = TextView(mContext)
                        tv.text = loader.loaderName
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
                        tv.background = ContextCompat.getDrawable(mContext, R.drawable.background_mod_category)
                        tv.backgroundTintList = ContextCompat.getColorStateList(mContext, loader.tagColor)
                        val lp = FlexboxLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        lp.setMargins(0, 0, Tools.dpToPx(10f).toInt(), 0)
                        tv.layoutParams = lp
                        tagsLayout.addView(tv)
                    }
                }

                val downloadCount = NumberWithUnits.formatNumberWithUnit(item.downloadCount, YLTools.isEnglish(mContext))
                downloadCountTextview.text = StringUtils.insertSpace(mContext.getString(R.string.download_info_downloads), downloadCount)
                dateTextview.text = StringUtils.formatDate(item.uploadDate, Locale.getDefault(), TimeZone.getDefault())

                item.iconUrl?.apply {
                    Glide.with(mContext).load(this).apply {
                        if (!AllSettings.resourceImageCache.getValue()) diskCacheStrategy(DiskCacheStrategy.NONE)
                    }.into(iconImageview)
                }

                val screenshotUrl = screenshotUrlCache[item.projectId]
                val imageUrl = screenshotUrl ?: item.iconUrl
                imageUrl?.apply {
                    Glide.with(mContext).load(this).apply {
                        if (!AllSettings.resourceImageCache.getValue()) diskCacheStrategy(DiskCacheStrategy.NONE)
                    }.into(thumbnailImageview)
                }

                if (screenshotUrl == null && item.iconUrl != null) {
                    TaskExecutors.getDefault().submit {
                        try {
                            val screenshots = item.platform.helper.getScreenshots(item.projectId)
                            val ssUrl = screenshots.firstOrNull()?.imageUrl
                            if (ssUrl != null) {
                                screenshotUrlCache[item.projectId] = ssUrl
                                TaskExecutors.runInUIThread {
                                    Glide.with(mContext).load(ssUrl).apply {
                                        if (!AllSettings.resourceImageCache.getValue()) diskCacheStrategy(DiskCacheStrategy.NONE)
                                    }.into(thumbnailImageview)
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        }

        private fun getPlatformIcon(platform: Platform): Drawable? {
            return when (platform) {
                Platform.MODRINTH -> ContextCompat.getDrawable(mContext, R.drawable.ic_modrinth)
                Platform.CURSEFORGE -> ContextCompat.getDrawable(mContext, R.drawable.ic_curseforge)
            }
        }

        private fun addCategoryView(layout: FlexboxLayout, text: String) {
            val textView = createCategoryView(mContext)
            textView.text = text
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
            layout.addView(textView)
        }
    }

    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface CallSearchListener {
        fun isLastPage(): Boolean
        fun loadMoreResult()
    }

    companion object {
        private const val VIEW_TYPE_LIST_ITEM = 0
        private const val VIEW_TYPE_GRID_ITEM = 2
        private const val VIEW_TYPE_LOADING = 1

        private val screenshotUrlCache = HashMap<String, String>()

        @JvmStatic
        fun createCategoryView(context: Context): TextView {
            return TextView(context).apply {
                id = View.generateViewId()
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = context.resources.getDimensionPixelSize(R.dimen._4sdp)
                    val verticalMargin = context.resources.getDimensionPixelSize(R.dimen._1sdp)
                    topMargin = verticalMargin
                    bottomMargin = verticalMargin
                }
                background = ContextCompat.getDrawable(context, R.drawable.background_mod_category)
            }
        }

        @JvmStatic
        fun getTagTextView(context: Context, string: Int, value: String): TextView {
            val textView = TextView(context)
            textView.text = StringUtils.insertSpace(context.getString(string), value)
            val layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, Tools.dpToPx(10f).toInt(), 0)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
            textView.layoutParams = layoutParams
            return textView
        }
    }
}
