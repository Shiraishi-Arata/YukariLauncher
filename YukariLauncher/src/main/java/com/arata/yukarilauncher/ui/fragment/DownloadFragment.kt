package com.arata.yukarilauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentDownloadBinding
import com.arata.yukarilauncher.event.value.DownloadPageEvent
import com.arata.yukarilauncher.event.value.DownloadPageEvent.PageSwapEvent.Companion.IN
import com.arata.yukarilauncher.event.value.DownloadPageEvent.PageSwapEvent.Companion.OUT
import com.arata.yukarilauncher.ui.fragment.download.resource.ModDownloadFragment
import com.arata.yukarilauncher.ui.fragment.download.resource.ModPackDownloadFragment
import com.arata.yukarilauncher.ui.fragment.download.resource.ResourcePackDownloadFragment
import com.arata.yukarilauncher.ui.fragment.download.resource.ShaderPackDownloadFragment
import com.arata.yukarilauncher.ui.fragment.download.resource.WorldDownloadFragment
import org.greenrobot.eventbus.EventBus

/**
 * ダウンロード管理フラグメント
 */
class DownloadFragment : FragmentWithAnim(R.layout.fragment_download) {
    companion object {
        const val TAG = "DownloadFragment"
    }

    private lateinit var binding: FragmentDownloadBinding

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadBinding.inflate(layoutInflater)
        return binding.root
    }

    /**
     * ビュー作成後の初期化処理を行います。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViewPager()

        binding.classifyTab.observeIndexChange { _, toIndex, reselect, fromUser ->
            if (reselect) return@observeIndexChange
            if (fromUser) binding.downloadViewpager.setCurrentItem(toIndex, false)
        }
    }

    /**
     * ViewPager2を初期化する
     */
    private fun initViewPager() {
        binding.downloadViewpager.apply {
            adapter = ViewPagerAdapter(this@DownloadFragment)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            offscreenPageLimit = 1
            isUserInputEnabled = false
            registerOnPageChangeCallback(object: OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    onFragmentSelect(position)
                    EventBus.getDefault().post(DownloadPageEvent.PageSwapEvent(position, IN))
                }
            })
        }
    }

    /**
     * フラグメント選択時にタブを更新する
     */
    private fun onFragmentSelect(position: Int) {
        binding.classifyTab.onPageSelected(position)
    }

    /**
     * スライドインアニメーションを実行します。
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.classifyLayout, Animations.BounceInRight))
    }

    /**
     * スライドアウトアニメーションを実行します。
     */
    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.classifyLayout, Animations.FadeOutLeft))
        EventBus.getDefault().post(DownloadPageEvent.PageSwapEvent(binding.classifyTab.currentItemIndex, OUT))
    }

    /**
     * ビュー破棄時にページ破棄イベントを発行します。
     */
    override fun onDestroyView() {
        EventBus.getDefault().post(DownloadPageEvent.PageDestroyEvent())
        super.onDestroyView()
    }

    /**
     * ViewPager2のアダプター
     */
    private class ViewPagerAdapter(private val fragment: Fragment): FragmentStateAdapter(fragment.requireActivity()) {
        override fun getItemCount(): Int = 5
        override fun createFragment(position: Int): Fragment {
            return when(position) {
                1 -> ModPackDownloadFragment(fragment)
                2 -> ResourcePackDownloadFragment(fragment)
                3 -> WorldDownloadFragment(fragment)
                4 -> ShaderPackDownloadFragment(fragment)
                else -> ModDownloadFragment(fragment)
            }
        }
    }
}