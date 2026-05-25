package com.arata.yukarilauncher.feature.download

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.ItemModVersionBinding
import com.arata.yukarilauncher.feature.download.enums.VersionType
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.item.ModLikeVersionItem
import com.arata.yukarilauncher.feature.download.item.ModVersionItem
import com.arata.yukarilauncher.feature.download.item.VersionItem
import com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper
import com.arata.yukarilauncher.ui.dialog.ModDependenciesDialog
import com.arata.yukarilauncher.utils.NumberWithUnits.Companion.formatNumberWithUnit
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.anim.ViewAnimUtils.Companion.setViewAnim
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.task.ProgressKeeper
import java.util.Locale
import java.util.StringJoiner
import java.util.TimeZone

/**
 * Modのバージョン一覧を表示するRecyclerViewアダプター。
 * 各バージョンのダウンロード数、日付、Modローダー、依存関係を表示し、インストール処理を行う。
 */
class VersionAdapter(
    private val infoItem: InfoItem,
    private val platformHelper: AbstractPlatformHelper,
    private val mData: List<VersionItem>?
) : RecyclerView.Adapter<VersionAdapter.InnerHolder>() {

    /**
     * 初期化時にデータをアップロード日でソートする。
     */
    init {
        mData?.sortedWith { o1, o2 ->
            o1.uploadDate.compareTo(o2.uploadDate)
        }
    }

    /**
     * 新しいViewHolderを生成する。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerHolder {
        return InnerHolder(ItemModVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    /**
     * 指定位置のバージョンデータをViewHolderにバインドする。
     */
    override fun onBindViewHolder(holder: InnerHolder, position: Int) {
        holder.setData(mData!![position])
    }

    /**
     * バージョンアイテムの総数を返す。
     */
    override fun getItemCount(): Int = mData?.size ?: 0

    /**
     * バージョン1項目分のViewHolder。
     * バージョン情報の表示とインストール処理を担当する。
     */
    inner class InnerHolder(private val binding: ItemModVersionBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        private val mContext: Context = itemView.context

        /**
         * バージョンアイテムのデータをビューに設定する。
         * バージョンタイトル、ダウンロード数、日付、Modローダー、タグを表示する。
         * クリック時は依存関係ダイアログを表示し、インストールを開始する。
         * @param versionItem 表示するバージョンアイテム
         */
        fun setData(versionItem: VersionItem) {
            binding.downloadImageview.setImageResource(getDownloadType(versionItem.versionType))

            binding.titleTextview.text = versionItem.title

            val downloadCountText = formatNumberWithUnit(versionItem.downloadCount, YLTools.isEnglish(mContext))
            val dateText = StringUtils.formatDate(versionItem.uploadDate, Locale.getDefault(),
                TimeZone.getDefault())

            binding.tagsLayout.removeAllViews()
            binding.tagsLayout.addView(getTagTextView(R.string.download_info_downloads, downloadCountText))
            binding.tagsLayout.addView(getTagTextView(R.string.download_info_date, dateText))

            if (versionItem is ModLikeVersionItem) {
                val sj = StringJoiner(", ")
                for (modloader in versionItem.modloaders) {
                    sj.add(modloader.loaderName)
                }
                val modloaderText = if (sj.length() > 0) sj.toString()
                else mContext.getString(R.string.generic_unknown)
                binding.tagsLayout.addView(getTagTextView(R.string.download_info_modloader, modloaderText))
            }
            binding.tagsLayout.addView(getTagTextView(getDownloadTypeText(versionItem.versionType)))

            binding.downloadLink.setOnClickListener { YLTools.openLink(mContext, versionItem.fileUrl) }

            itemView.setOnClickListener {
                if (versionItem is ModVersionItem && versionItem.dependencies.isNotEmpty()) {
                    ModDependenciesDialog(mContext, infoItem, versionItem.dependencies) {
                        startInstall(versionItem)
                    }.show()
                } else {
                    startInstall(versionItem)
                }
            }
        }

        /**
         * バージョンのインストールを開始する。
         * 進行中のタスクがある場合はシェイクアニメーションを表示し、トーストで通知する。
         * @param versionItem インストールするバージョンアイテム
         */
        private fun startInstall(versionItem: VersionItem) {
            platformHelper.install(mContext, infoItem, versionItem) { key ->
                val containsProgress = ProgressKeeper.containsProgress(key)
                if (containsProgress) {
                    setViewAnim(itemView, Animations.Shake)
                    Toast.makeText(mContext, mContext.getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT).show()
                }
                containsProgress
            }
        }

        /**
         * バージョンタイプに対応するダウンロードアイコンのリソースIDを取得する。
         * @param versionType バージョンタイプ
         * @return ダウンロードアイコンのリソースID
         */
        private fun getDownloadType(versionType: VersionType): Int {
            return when (versionType) {
                VersionType.BETA -> R.drawable.ic_download_beta
                VersionType.ALPHA -> R.drawable.ic_download_alpha
                VersionType.RELEASE -> R.drawable.ic_download_release
            }
        }

        /**
         * バージョンタイプに対応する表示テキストを取得する。
         * @param versionType バージョンタイプ
         * @return 表示用テキスト
         */
        private fun getDownloadTypeText(versionType: VersionType): String {
            val text = when (versionType) {
                VersionType.RELEASE -> mContext.getString(R.string.generic_release)
                VersionType.BETA -> mContext.getString(R.string.generic_beta)
                VersionType.ALPHA -> mContext.getString(R.string.generic_alpha)
            }
            return text
        }

        /**
         * リソースIDと値からタグ用のTextViewを生成する。
         * @param string ラベルのリソースID
         * @param value 表示する値
         * @return 生成されたTextView
         */
        private fun getTagTextView(string: Int, value: String): TextView {
            return getTagTextView(StringUtils.insertSpace(mContext.getString(string), value))
        }

        /**
         * 指定されたテキストでタグ用のTextViewを生成する。
         * @param value 表示するテキスト
         * @return 生成されたTextView
         */
        private fun getTagTextView(value: String): TextView {
            val textView = TextView(mContext)
            textView.text = value
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
