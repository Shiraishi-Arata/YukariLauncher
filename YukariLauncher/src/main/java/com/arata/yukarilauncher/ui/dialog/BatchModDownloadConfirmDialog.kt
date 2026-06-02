package com.arata.yukarilauncher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.DialogBatchModDownloadBinding
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.item.VersionItem

/**
 * 一括Modダウンロード確認ダイアログ
 */
class BatchModDownloadConfirmDialog(
    context: Context,
    private val items: List<ResolvedDownloadItem>,
    private val onConfirm: (List<ResolvedDownloadItem>) -> Unit
) : FullScreenDialog(context) {

    /**
     * 解決済みダウンロードアイテム
     */
    data class ResolvedDownloadItem(
        val info: InfoItem,
        val version: VersionItem,
        val dependencies: List<ResolvedDownloadItem>,
        val selectedDirectly: Boolean
    )

    private val binding = DialogBatchModDownloadBinding.inflate(layoutInflater)
    private val selectedDependencies = LinkedHashMap<String, ResolvedDownloadItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setCancelable(false)

        binding.titleView.gravity = Gravity.START
        buildContent()

        binding.closeButton.setOnClickListener { dismiss() }
        binding.confirmButton.setOnClickListener {
            val merged = items + selectedDependencies.values
            onConfirm(merged)
            dismiss()
        }
    }

    /**
     * ダイアログのコンテンツを構築する
     */
    private fun buildContent() {
        binding.contentLayout.removeAllViews()
        items.forEach { item ->
            val parent = createItemRow(item, false)
            binding.contentLayout.addView(parent)
            if (item.dependencies.isNotEmpty()) {
                val toggle = TextView(context).apply {
                    text = "▶ ${context.getString(R.string.download_install_dependencies, item.info.title)}"
                    setPadding(24, 4, 0, 4)
                }
                val deps = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    visibility = View.GONE
                }

                item.dependencies.forEach { dep ->
                    selectedDependencies[dep.info.projectId] = dep
                    deps.addView(createDependencyRow(dep))
                }
                toggle.setOnClickListener {
                    val expanded = deps.visibility != View.VISIBLE
                    deps.visibility = if (expanded) View.VISIBLE else View.GONE
                    toggle.text = "${if (expanded) "▼" else "▶"} ${context.getString(R.string.download_install_dependencies, item.info.title)}"
                }
                binding.contentLayout.addView(toggle)
                binding.contentLayout.addView(deps)
            }
        }
    }

    /**
     * アイテム行のビューを生成する
     */
    private fun createItemRow(item: ResolvedDownloadItem, dependency: Boolean): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(if (dependency) 24 else 0, 8, 0, 8)

            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(52, 52)
            }
            Glide.with(context).load(item.info.iconUrl).into(icon)
            addView(icon)

            addView(TextView(context).apply {
                text = if (item.selectedDirectly && dependency) {
                    "${item.info.title}\n${context.getString(R.string.download_batch_selected_tag)}"
                } else item.info.title
                setPadding(12, 0, 0, 0)
            })
        }
    }

    /**
     * 依存関係アイテムの行ビューを生成する
     */
    private fun createDependencyRow(item: ResolvedDownloadItem): View {
        val row = createItemRow(item, true) as LinearLayout
        if (item.selectedDirectly) return row
        val checkBox = CheckBox(context).apply {
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedDependencies[item.info.projectId] = item
                else selectedDependencies.remove(item.info.projectId)
            }
        }
        row.addView(checkBox, 0)
        return row
    }
}