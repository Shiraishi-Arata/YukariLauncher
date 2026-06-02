package com.arata.yukarilauncher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.databinding.DialogSelectItemBinding

/**
 * 選択ダイアログの抽象基底クラス
 */
abstract class AbstractSelectDialog(context: Context) : FullScreenDialog(context) {
    protected val binding = DialogSelectItemBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.closeButton.setOnClickListener { this.dismiss() }
        initDialog(binding.recyclerView)
    }

    /** タイトルテキストを設定する */
    fun setTitleText(text: Int) {
        setTitleText(context.getString(text))
    }

    /** タイトルテキストを設定する */
    fun setTitleText(text: String) {
        binding.titleView.text = text
    }

    /** メッセージテキストを設定する */
    fun setMessageText(text: Int) {
        setMessageText(context.getString(text))
    }

    /** メッセージテキストを設定する */
    fun setMessageText(text: String?) {
        binding.messageView.text = text
        binding.messageView.visibility = if (text != null) View.VISIBLE else View.GONE
    }

    /** ダイアログの初期化処理 */
    abstract fun initDialog(recyclerView: RecyclerView)
}