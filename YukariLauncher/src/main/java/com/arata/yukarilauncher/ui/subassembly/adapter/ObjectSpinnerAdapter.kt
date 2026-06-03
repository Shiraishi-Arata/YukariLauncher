package com.arata.yukarilauncher.ui.subassembly.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import com.skydoves.powerspinner.PowerSpinnerInterface
import com.skydoves.powerspinner.PowerSpinnerView
import com.skydoves.powerspinner.databinding.PowerspinnerItemDefaultPowerBinding
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools

/**
 * PowerSpinner用のジェネリックアダプター
 * 元コード: [com.skydoves.powerspinner.DefaultSpinnerAdapter] を改変
 */
@SuppressLint("NotifyDataSetChanged")
class ObjectSpinnerAdapter<T>(
    powerSpinnerView: PowerSpinnerView,
    private val itemIconProvider: ((T) -> Drawable?)? = null,
    private val itemNameProvider: (T) -> String
) : RecyclerView.Adapter<ObjectSpinnerAdapter.ViewHolder<T>>(),
    PowerSpinnerInterface<T> {
    companion object {
        private const val NO_INT_VALUE = Int.MIN_VALUE
        private const val NO_SELECTED_INDEX = -1
    }

    override var index: Int = powerSpinnerView.selectedIndex
    override val spinnerView: PowerSpinnerView = powerSpinnerView
    override var onSpinnerItemSelectedListener: OnSpinnerItemSelectedListener<T>? = null

    private val spinnerItems: MutableList<T> = arrayListOf()

    /**
     * ビューホルダーを生成します。
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder<T> {
        val binding = PowerspinnerItemDefaultPowerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, itemIconProvider, itemNameProvider).apply {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?: return@setOnClickListener
                notifyItemSelected(position)
            }
        }
    }

    /**
     * ビューホルダーにデータをバインドします。
     */
    override fun onBindViewHolder(holder: ViewHolder<T>, position: Int) {
        holder.bind(spinnerView, spinnerItems[position], index == position)
    }

    /**
     * アイテムリストを設定する
     */
    override fun setItems(itemList: List<T>) {
        this.spinnerItems.clear()
        this.spinnerItems.addAll(itemList)
        notifyDataSetChanged()
    }

    /**
     * 指定インデックスのアイテムを選択状態にする
     */
    override fun notifyItemSelected(index: Int) {
        if (index == NO_SELECTED_INDEX) return
        val oldIndex = this.index
        this.index = index
        this.spinnerView.notifyItemSelected(
            index,
            itemNameProvider(spinnerItems[index])
        )
        notifyDataSetChanged()
        this.onSpinnerItemSelectedListener?.onItemSelected(
            oldIndex = oldIndex,
            oldItem = oldIndex.takeIf { it != NO_SELECTED_INDEX }?.let { spinnerItems[oldIndex] },
            newIndex = index,
            newItem = spinnerItems[index]
        )
    }

    override fun getItemCount(): Int = spinnerItems.size

    class ViewHolder<T>(
        private val binding: PowerspinnerItemDefaultPowerBinding,
        private val itemIconProvider: ((T) -> Drawable?)?,
        private val itemNameProvider: (T) -> String
    ) : RecyclerView.ViewHolder(binding.root) {

        internal fun bind(spinnerView: PowerSpinnerView, item: T, isSelectedItem: Boolean) {
            binding.itemDefaultText.apply {
                text = itemNameProvider(item)
                typeface = spinnerView.typeface
                gravity = spinnerView.gravity
                setTextSize(TypedValue.COMPLEX_UNIT_PX, spinnerView.textSize)
                setTextColor(spinnerView.currentTextColor)
                val icon = itemIconProvider?.invoke(item)
                if (icon != null) {
                    val size = Tools.dpToPx(16f).toInt()
                    icon.setBounds(0, 0, size, size)
                    setCompoundDrawablesRelative(icon, null, null, null)
                    compoundDrawablePadding = Tools.dpToPx(6f).toInt()
                } else {
                    setCompoundDrawablesRelative(null, null, null, null)
                    compoundDrawablePadding = 0
                }
            }
            binding.root.setPadding(
                spinnerView.paddingLeft,
                spinnerView.paddingTop,
                spinnerView.paddingRight,
                spinnerView.paddingBottom
            )
            if (spinnerView.spinnerItemHeight != NO_INT_VALUE) {
                binding.root.height = spinnerView.spinnerItemHeight
            }
            if (spinnerView.spinnerSelectedItemBackground != null && isSelectedItem) {
                binding.root.background = spinnerView.spinnerSelectedItemBackground
            } else {
                binding.root.background = null
            }
        }
    }
}