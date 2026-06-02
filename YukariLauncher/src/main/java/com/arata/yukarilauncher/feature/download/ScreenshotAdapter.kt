package com.arata.yukarilauncher.feature.download

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.arata.yukarilauncher.databinding.ViewInfoScreenshotBinding
import com.arata.yukarilauncher.feature.download.item.ScreenshotItem
import com.arata.yukarilauncher.setting.AllSettings

/**
 * スクリーンショットギャラリー用のRecyclerViewアダプター。
 * スクリーンショットの読み込み、表示、リトライ処理を担当する。
 */
class ScreenshotAdapter(private val screenshotItems: List<ScreenshotItem>) : RecyclerView.Adapter<ScreenshotAdapter.ViewHolder>() {

    /**
     * 新しいViewHolderを生成する。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ViewInfoScreenshotBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    /**
     * 指定位置のスクリーンショットデータをViewHolderにバインドする。
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setScreenshot(screenshotItems[position])
    }

    /**
     * スクリーンショット項目の総数を返す。
     */
    override fun getItemCount(): Int = screenshotItems.size

    /**
     * スクリーンショット1項目分のViewHolder。
     * 画像の読み込み状態管理と表示を担当する。
     */
    class ViewHolder(val binding: ViewInfoScreenshotBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * スクリーンショットアイテムのデータをビューに設定する。
         * 画像を読み込み、タイトルと説明があれば表示する。
         * @param item スクリーンショットアイテム
         */
        fun setScreenshot(item: ScreenshotItem) {
            binding.apply {
                retry.setOnClickListener { loadScreenshotImage(item.imageUrl) }

                loadScreenshotImage(item.imageUrl)

                title.setVisibleIfNotBlank(item.title)
                description.setVisibleIfNotBlank(item.description)
            }
        }

        /**
         * Glideを使用してスクリーンショット画像を非同期読み込みする。
         * 読み込み中はローディング表示、失敗時はリトライボタンを表示する。
         * @param imageUrl 画像のURL
         */
        @SuppressLint("CheckResult")
        private fun loadScreenshotImage(imageUrl: String) {
            binding.apply {
                setLoading(true)
                val requestBuilder = Glide.with(screenshot).load(imageUrl)
                if (!AllSettings.resourceImageCache.getValue()) requestBuilder.diskCacheStrategy(DiskCacheStrategy.NONE)
                requestBuilder.fitCenter()
                    .addListener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            setLoading(false)
                            setFailed()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            setLoading(false)
                            return false
                        }
                    })
                    .into(screenshot)
            }
        }

        /**
         * ローディング表示のON/OFFを切り替える。
         * @param loading trueの場合はローディング中表示
         */
        private fun setLoading(loading: Boolean) {
            binding.loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
            if (loading) binding.retry.visibility = View.GONE
        }

        /**
         * 読み込み失敗時の表示に切り替える（リトライボタンを表示）。
         */
        private fun setFailed() {
            binding.retry.visibility = View.VISIBLE
        }

        /**
         * テキストが空でない場合のみTextViewを表示する拡張関数。
         * @param text 表示するテキスト
         */
        private fun TextView.setVisibleIfNotBlank(text: String?) {
            visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
            this.text = text
        }
    }
}