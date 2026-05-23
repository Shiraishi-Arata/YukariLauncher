package com.arata.yukarilauncher.ui.subassembly.modlist

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.CheckBox
import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentModDownloadBinding
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.ui.fragment.FragmentWithAnim
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.anim.AnimUtils
import com.arata.yukarilauncher.utils.anim.AnimUtils.Companion.playVisibilityAnim
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import java.util.concurrent.Future


/**
 * Modダウンロードリストのベースフラグメント
 */
abstract class ModListFragment : FragmentWithAnim(R.layout.fragment_mod_download) {
    private lateinit var binding: FragmentModDownloadBinding
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var releaseCheckBox: CheckBox
    protected var fragmentActivity: FragmentActivity? = null
    private var parentAdapter: RecyclerView.Adapter<*>? = null
    protected var currentTask: Future<*>? = null
    private var releaseCheckBoxVisible = true
    private val parentElementAnimPlayer = AnimPlayer()
    private var isInitialized: Boolean = false

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentModDownloadBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        releaseCheckBox = binding.releaseVersion
        return binding.root
    }

    /**
     * ビュー作成後の初期化を行います。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView1: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView1, dx, dy)
                    val layoutManager = recyclerView1.layoutManager as LinearLayoutManager?
                    if (layoutManager != null && recyclerView1.adapter != null) {
                        val lastPosition = layoutManager.findFirstVisibleItemPosition()
                        val b = lastPosition >= 12

                        AnimUtils.setVisibilityAnim(binding.backToTop, b)
                    }
                }
            })
            recyclerView.layoutAnimation = LayoutAnimationController(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_downwards))
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            refreshButton.setOnClickListener { refreshTask() }
            releaseVersion.setOnClickListener { initRefresh() }
            returnButton.setOnClickListener { YLTools.onBackPressed(requireActivity()) }

            backToTop.setOnClickListener { recyclerView.smoothScrollToPosition(0) }
        }

        if (!isInitialized) {
            isInitialized = true
            init()
        }
        refreshCreatedView()
    }

    @CallSuper
    protected open fun init() {
        currentTask = initRefresh()
    }

    /**
     * ビュー作成後に追加の更新処理を実行します。
     */
    protected open fun refreshCreatedView() {}

    /**
     * フラグメントがアクティビティにアタッチされたときに呼ばれます。
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.fragmentActivity = requireActivity()
    }

    /**
     * フラグメントが一時停止するときにタスクをキャンセルします。
     */
    override fun onPause() {
        cancelTask()
        super.onPause()
    }

    /**
     * フラグメントが破棄されるときにタスクをキャンセルします。
     */
    override fun onDestroy() {
        cancelTask()
        super.onDestroy()
    }

    /**
     * バックキー押下時の処理を行います。
     */
    override fun onBackPressed(): Boolean {
        return parentAdapter?.let { adapter ->
            hideParentElement(false)
            recyclerView.adapter = adapter
            recyclerView.scheduleLayoutAnimation()
            parentAdapter = null
            false
        } ?: true
    }

    /**
     * 親要素の表示/非表示を切り替える
     */
    private fun hideParentElement(hide: Boolean) {
        cancelTask()

        binding.apply {
            refreshButton.isEnabled = !hide
            releaseVersion.isEnabled = !hide

            parentElementAnimPlayer.clearEntries()
            parentElementAnimPlayer
                .duration((AllSettings.animationSpeed.getValue() * 0.7).toLong())
                .apply(AnimPlayer.Entry(selectTitle, if (hide) Animations.FadeIn else Animations.FadeOut))
                .apply(AnimPlayer.Entry(refreshButton, if (hide) Animations.FadeOut else Animations.FadeIn))

            if (releaseCheckBoxVisible)
                parentElementAnimPlayer.apply(AnimPlayer.Entry(releaseVersion, if (hide) Animations.FadeOut else Animations.FadeIn))

            parentElementAnimPlayer.setOnStart {
                selectTitle.visibility = View.VISIBLE
                refreshButton.visibility = View.VISIBLE
                if (releaseCheckBoxVisible) releaseVersion.visibility = View.VISIBLE
            }

            parentElementAnimPlayer.setOnEnd {
                if (!hide) selectTitle.visibility = View.GONE
                else {
                    refreshButton.visibility = View.GONE
                    if (releaseCheckBoxVisible) releaseVersion.visibility = View.GONE
                }
            }

            parentElementAnimPlayer.start()
        }
    }

    /**
     * 現在のタスクをキャンセルする
     */
    private fun cancelTask() {
        currentTask?.apply { if (!isDone) cancel(true) }
    }

    /**
     * 更新タスクを実行する
     */
    private fun refreshTask() {
        currentTask = refresh()
    }

    /**
     * 最初の更新処理を初期化します。
     */
    protected abstract fun initRefresh(): Future<*>?
    /**
     * データの更新処理を実行します。
     */
    protected abstract fun refresh(): Future<*>?

    /**
     * 処理中状態の表示/非表示を設定する
     */
    protected fun componentProcessing(state: Boolean) {
        binding.apply {
            playVisibilityAnim(loadingLayout, state)
            recyclerView.visibility = if (state) View.GONE else View.VISIBLE
            refreshButton.isEnabled = !state
            releaseVersion.isEnabled = !state
        }
    }

    /**
     * Mapに指定されたKeyのListが存在しない場合は新規作成して要素を追加する
     */
    protected fun <K, E> addIfAbsent(map: MutableMap<K, MutableList<E>>, key: K, element: E) {
        map.computeIfAbsent(key) { ArrayList() }
            .add(element)
    }

    /** タイトルテキストを設定する */
    protected fun setTitleText(nameText: String?) {
        binding.title.text = nameText
    }

    /** 説明文を設定する */
    protected fun setDescription(text: String) {
        binding.description.apply {
            this.visibility = View.VISIBLE
            this.text = text
        }
    }

    /** アイコンを設定する */
    protected fun setIcon(icon: Drawable?) {
        binding.icon.setImageDrawable(icon)
    }

    /** アイコンビューを取得する */
    protected fun getIconView() = binding.icon

    /** リリース版チェックボックスを非表示にする */
    protected fun setReleaseCheckBoxGone() {
        releaseCheckBoxVisible = false
        binding.releaseVersion.visibility = View.GONE
    }

    /** 読み込み失敗を表示する */
    protected fun setFailedToLoad(reasons: String?) {
        val text = fragmentActivity!!.getString(R.string.mod_failed_to_load_list)
        binding.failedToLoad.text = if (reasons == null) text else StringUtils.insertNewline(text, reasons)
        playVisibilityAnim(binding.failedToLoad, true)
    }

    /** 読み込み失敗表示を解除する */
    protected fun cancelFailedToLoad() {
        playVisibilityAnim(binding.failedToLoad, false)
    }

    /** 外部リンクを設定する */
    protected fun setLink(link: String?) {
        link?.let { uri ->
            binding.launchLink.apply {
                this.setOnClickListener { YLTools.openLink(fragmentActivity, uri) }
                AnimUtils.setVisibilityAnim(this, true)
            }
        }
    }

    /** MCModリンクを設定する */
    protected fun setMCMod(link: String?) {
        if (YLTools.areaChecks("zh")) {
            link?.let { uri ->
                binding.mcmodLink.apply {
                    this.visibility = View.VISIBLE
                    this.paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    this.setOnClickListener { YLTools.openLink(fragmentActivity, uri) }
                }
            }
        }
    }

    /** 追加ビューを追加する */
    protected fun addMoreView(view: View) {
        binding.moreLayout.addView(view)
    }

    /** 追加ビューを削除する */
    protected fun removeMoreView(view: View) {
        binding.moreLayout.removeView(view)
    }

    /**
     * 子アダプターに切り替える
     */
    fun switchToChild(adapter: RecyclerView.Adapter<*>?, title: String?) {
        if (currentTask!!.isDone && adapter != null) {
            binding.apply {
                parentAdapter = recyclerView.adapter
                selectTitle.text = title
                hideParentElement(true)
                recyclerView.adapter = adapter
                recyclerView.scheduleLayoutAnimation()
            }
        }
    }

    /**
     * スライドインアニメーションを設定します。
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(modsLayout, Animations.BounceInDown))
                .apply(AnimPlayer.Entry(operateLayout, Animations.BounceInLeft))
                .apply(AnimPlayer.Entry(icon, Animations.Wobble))
                .apply(AnimPlayer.Entry(title, Animations.FadeInLeft))
                .apply(AnimPlayer.Entry(description, Animations.FadeInLeft))
        }
    }

    /**
     * スライドアウトアニメーションを設定します。
     */
    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.modsLayout, Animations.FadeOutUp))
            .apply(AnimPlayer.Entry(binding.operateLayout, Animations.FadeOutRight))
    }
}
