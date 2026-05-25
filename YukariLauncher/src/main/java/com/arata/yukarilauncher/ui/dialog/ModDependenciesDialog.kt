package com.arata.yukarilauncher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.DialogModDependenciesBinding
import com.arata.yukarilauncher.feature.download.ModDependenciesAdapter
import com.arata.yukarilauncher.feature.download.item.DependenciesInfoItem
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.Tools

/**
 * Mod依存関係表示ダイアログ
 */
class ModDependenciesDialog(
    context: Context,
    private val infoItem: InfoItem,
    private val dependenciesData: List<DependenciesInfoItem>,
    private val install: () -> Unit
) : FullScreenDialog(context) {
    private val binding = DialogModDependenciesBinding.inflate(layoutInflater)

    /**
     * ダイアログ作成時に初期化処理を実行する
     * @param savedInstanceState 保存されたインスタンス状態
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCancelable(false)
        setContentView(binding.root)

        window?.apply {
            val dimension = Tools.dpToPx(context.resources.getDimension(R.dimen._12sdp)).toInt()
            attributes.width = Tools.currentDisplayMetrics.widthPixels - 2 * dimension
            attributes.height = Tools.currentDisplayMetrics.heightPixels - 2 * dimension

            setGravity(Gravity.CENTER)

            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        runCatching {
            init()
        }.getOrElse {
            dismiss()
            Logging.e("ModDependenciesDialog", "Initialization failed, dismiss attempted.", it)
        }
    }

    /**
     * ダイアログを初期化する
     */
    private fun init() {
        val data = dependenciesData.toMutableList().apply { this.sort() }

        binding.apply {
            titleView.text = context.getString(R.string.download_install_dependencies, infoItem.title)
            downloadButton.text = context.getString(R.string.download_install, infoItem.title)

            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                layoutAnimation = LayoutAnimationController(AnimationUtils.loadAnimation(context, R.anim.fade_downwards))
                adapter = ModDependenciesAdapter(infoItem, data).apply {
                    setOnItemCLickListener { dismiss() }
                }
            }

            closeButton.setOnClickListener { dismiss() }
            downloadButton.setOnClickListener {
                install()
                dismiss()
            }
        }
    }
}
