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
import com.arata.yukarilauncher.event.value.DownloadPageEvent
import com.arata.yukarilauncher.feature.download.enums.Platform
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.item.ModInfoItem
import com.arata.yukarilauncher.setting.AllSettings
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

    private val mViewHolderSet: MutableSet<ViewHolder> = Collections.newSetFromMap(WeakHashMap())
    private var mItems: MutableList<InfoItem> = ArrayList()
    private var originalItems: MutableList<InfoItem> = ArrayList()
    private val selectedProjectIds: MutableSet<String> = HashSet()
    private var selectMode = false
    private var selectListener: ((InfoItem) -> Unit)? = null

    /**
     * 新しいViewHolderを作成する。アイテムの種類に応じてViewの種類を切り替える
     * @param viewGroup 親のViewGroup
     * @param viewType Viewの種類（MOD_ITEMまたはLOADING）
     * @return 作成されたViewHolder
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        val view: View
        when (viewType) {
            VIEW_TYPE_MOD_ITEM -> {
                // リストアイテムのUIを定義する新しいビューを作成
                return ViewHolder(ItemDownloadInfoBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false))
            }

            VIEW_TYPE_LOADING -> {
                // 実際にはプログレスバーのみのビューを作成
                view = layoutInflater.inflate(R.layout.view_loading, viewGroup, false)
                return LoadingViewHolder(view)
            }

            else -> throw RuntimeException("Unimplemented view type!")
        }
    }

    /**
     * 指定された位置のアイテムをViewHolderにバインドする
     * @param holder 対象のViewHolder
     * @param position アイテムの位置
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_MOD_ITEM -> (holder as ViewHolder).setStateLimited(mItems[position])
            VIEW_TYPE_LOADING -> listener.loadMoreResult()
            else -> throw RuntimeException("Unimplemented view type!")
        }
    }

    /**
     * アイテムの総数を返す。最終ページでなければ読み込み表示用に+1する
     * @return アイテム数
     */
    override fun getItemCount(): Int {
        if (listener.isLastPage() || mItems.isEmpty()) return mItems.size
        return mItems.size + 1
    }

    /**
     * 指定された位置のアイテムのViewタイプを返す
     * @param position アイテムの位置
     * @return アイテムのViewタイプ
     */
    override fun getItemViewType(position: Int): Int {
        if (position < mItems.size) return VIEW_TYPE_MOD_ITEM
        return VIEW_TYPE_LOADING
    }

    /**
     * アイテムリストを設定し、表示を更新する
     * @param item 設定するアイテムリスト
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setItems(item: List<InfoItem>) {
        originalItems = item.toMutableList()
        rebuildDisplayItems()
        notifyDataSetChanged()
    }

    /**
     * 選択モードの有効/無効を設定する
     * @param enabled 選択モードを有効にするかどうか
     * @param listener 選択時に呼び出されるコールバック
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setSelectMode(enabled: Boolean, listener: ((InfoItem) -> Unit)?) {
        selectMode = enabled
        selectListener = listener
        notifyDataSetChanged()
    }

    /**
     * 選択されたプロジェクトIDのセットを設定する
     * @param ids 選択されたプロジェクトIDのセット
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedProjectIds(ids: Set<String>) {
        selectedProjectIds.clear()
        selectedProjectIds.addAll(ids)
        rebuildDisplayItems()
        notifyDataSetChanged()
    }

    /**
     * 表示用アイテムリストを元のリストから再構築する
     */
    private fun rebuildDisplayItems() {
        mItems = originalItems.toMutableList()
    }

    /**
     * リストアイテムのViewHolder。バインディングを用いてUIを設定する
     * @param binding アイテムのバインディングオブジェクト
     */
    inner class ViewHolder(val binding: ItemDownloadInfoBinding) : RecyclerView.ViewHolder(binding.root) {
        private val mContext = binding.root.context
        private var item: InfoItem? = null

        init {
            mViewHolderSet.add(this)
        }

        /**
         * アイテムのデータをビューに設定する。アイコン、タイトル、説明、カテゴリ、タグなどを表示する
         * @param item 表示するInfoItem
         */
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

                titleTextview.text =
                    if (YLTools.areaChecks("zh")) {
                        mod?.displayName ?: item.title
                    } else {
                        item.title
                    }
                descriptionTextview.text = item.description
                platformImageview.setImageDrawable(getPlatformIcon(item.platform))
                platformTextview.text = item.platform.pName
                // カテゴリの設定
                categoriesLayout.removeAllViews()
                item.category.forEach { item ->
                    addCategoryView(categoriesLayout, mContext.getString(item.resNameID))
                }
                // タグの設定
                tagsLayout.removeAllViews()

                val downloadCount = NumberWithUnits.formatNumberWithUnit(item.downloadCount, YLTools.isEnglish(mContext))
                tagsLayout.addView(getTagTextView(mContext, R.string.download_info_downloads, downloadCount))

                item.author?.let {
                    val authorSJ = StringJoiner(", ")
                    for (s in it) {
                        authorSJ.add(s)
                    }
                    tagsLayout.addView(getTagTextView(mContext, R.string.download_info_author, authorSJ.toString()))
                }

                tagsLayout.addView(getTagTextView(mContext, R.string.download_info_date, StringUtils.formatDate(item.uploadDate, Locale.getDefault(), TimeZone.getDefault())))
                if (item is ModInfoItem) {
                    val modloaderSJ = StringJoiner(", ")
                    for (s in item.modloaders) {
                        modloaderSJ.add(s.loaderName)
                    }
                    val modloaderText = if (modloaderSJ.length() > 0) modloaderSJ.toString()
                    else mContext.getString(R.string.generic_unknown)
                    tagsLayout.addView(getTagTextView(mContext, R.string.download_info_modloader, modloaderText))
                }

                item.iconUrl?.apply {
                    Glide.with(mContext).load(this).apply {
                        if (!AllSettings.resourceImageCache.getValue()) diskCacheStrategy(DiskCacheStrategy.NONE)
                    }.into(thumbnailImageview)
                }
            }
            binding.tagsLayout
        }

        /**
         * プラットフォームに対応するアイコンのDrawableを取得する
         * @param platform 対象プラットフォーム
         * @return プラットフォームアイコンのDrawable
         */
        private fun getPlatformIcon(platform: Platform): Drawable? {
            return when (platform) {
                Platform.MODRINTH -> ContextCompat.getDrawable(mContext, R.drawable.ic_modrinth)
                Platform.CURSEFORGE -> ContextCompat.getDrawable(mContext, R.drawable.ic_curseforge)
            }
        }

        /**
         * FlexboxLayoutにカテゴリ表示用のTextViewを追加する
         * @param layout 追加先のFlexboxLayout
         * @param text 表示するテキスト
         */
        private fun addCategoryView(layout: FlexboxLayout, text: String) {
            val textView = createCategoryView(mContext)
            textView.text = text
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
            layout.addView(textView)
        }
    }

    /**
     * リスト末尾のプログレスバーを保持するViewHolder
     */
    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    /**
     * @see com.arata.yukarilauncher.ui.fragment.download.AbstractResourceDownloadFragment
     */
    interface CallSearchListener {
        /**
         * 現在の検索結果が最終ページかどうかを判定する
         * 最終ページの場合は、読み込みビューを表示せず、追加の検索結果も要求しない
         * @return 最終ページの場合はtrue
         */
        fun isLastPage(): Boolean

        /**
         * さらに結果を読み込むよう要求する
         */
        fun loadMoreResult()
    }

    companion object {
        private const val VIEW_TYPE_MOD_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1

        /**
         * カテゴリ表示用のTextViewを作成する
         * @param context コンテキスト
         * @return 作成されたTextView
         */
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

        /**
         * タグ表示用のTextViewを生成する
         * @param context コンテキスト
         * @param string リソース文字列のID
         * @param value タグの値
         * @return 作成されたTextView
         */
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
