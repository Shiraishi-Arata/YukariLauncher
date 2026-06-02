package com.arata.yukarilauncher.ui.subassembly.view

import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.arata.yukarilauncher.R
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.listener.control.IFxScopeControl
import com.petterp.floatingx.view.FxViewHolder

/**
 * 検索ビューのフローティングウィンドウをラップするクラス
 */
class SearchViewWrapper(private val fragment: Fragment) {
    private lateinit var mSearchEditText: EditText
    private var searchListener: SearchListener? = null
    private var showSearchResultsListener: ShowSearchResultsListener? = null
    private var searchAsynchronousUpdatesListener: SearchAsynchronousUpdatesListener? = null
    private var isShow = false

    private var scopeFx: IFxScopeControl? = null

    /**
     * フローティングウィンドウの制御インスタンスを取得する
     */
    private fun getWindow(): IFxScopeControl {
        return FxScopeHelper.Builder().apply {
            setLayout(R.layout.view_search)
            setEnableEdgeAdsorption(false)
            addViewLifecycle(object : IFxViewLifecycle {
                override fun initView(holder: FxViewHolder) {
                    mSearchEditText = holder.getView(R.id.edit_text)
                    val caseSensitive = holder.getView<CheckBox>(R.id.case_sensitive)
                    val searchCountText = holder.getView<TextView>(R.id.text)

                    holder.getView<ImageButton>(R.id.search_button).setOnClickListener {
                        search(searchCountText, caseSensitive.isChecked)
                    }
                    holder.getView<CheckBox>(R.id.show_search_results_only).setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                        showSearchResultsListener?.apply { onSearch(isChecked) }
                        if (mSearchEditText.getText().toString().isNotEmpty()) search(searchCountText, caseSensitive.isChecked)
                    }
                }
            })
            setGravity(FxGravity.TOP_OR_CENTER)
        }.build().toControl(fragment)
    }

    /**
     * 検索を実行し、結果件数を表示する
     */
    private fun search(searchCountText: TextView, caseSensitive: Boolean) {
        val searchCount: Int
        val string = mSearchEditText.text.toString()
        searchListener?.apply {
            searchCount = onSearch(string, caseSensitive)
            searchCountText.text = searchCountText.context.getString(R.string.search_count, searchCount)
            if (searchCount != 0) searchCountText.visibility = View.VISIBLE
            return
        }
        searchAsynchronousUpdatesListener?.apply { onSearch(searchCountText, string, caseSensitive) }
    }

    /**
     * 検索リスナーを設定する
     */
    fun setSearchListener(listener: SearchListener?) {
        this.searchListener = listener
    }

    /**
     * 非同期更新リスナーを設定する
     */
    fun setAsynchronousUpdatesListener(listener: SearchAsynchronousUpdatesListener?) {
        this.searchAsynchronousUpdatesListener = listener
    }

    /**
     * 検索結果表示リスナーを設定する
     */
    fun setShowSearchResultsListener(listener: ShowSearchResultsListener?) {
        this.showSearchResultsListener = listener
    }

    /** 表示中かどうかを返す */
    fun isVisible() = isShow

    /**
     * 表示状態を切り替える
     */
    fun setVisibility() {
        isShow = !isShow
        setVisibility(isShow)
    }

    /**
     * 指定された表示状態に設定する
     */
    fun setVisibility(visible: Boolean) {
        if (visible) {
            scopeFx ?: run {
                scopeFx = getWindow().apply {
                    show()
                }
            }
        } else {
            scopeFx?.cancel()
            scopeFx = null
        }
    }

    /**
     * 検索処理のリスナーインターフェース
     */
    interface SearchListener {
        fun onSearch(string: String?, caseSensitive: Boolean): Int
    }

    /**
     * 非同期検索更新のリスナーインターフェース
     */
    interface SearchAsynchronousUpdatesListener {
        fun onSearch(searchCount: TextView?, string: String?, caseSensitive: Boolean)
    }

    /**
     * 検索結果表示のリスナーインターフェース
     */
    interface ShowSearchResultsListener {
        fun onSearch(show: Boolean)
    }
}