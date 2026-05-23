package com.arata.yukarilauncher.ui.fragment.settings.wrapper

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging.e
import com.arata.yukarilauncher.setting.unit.IntSettingUnit
import com.arata.yukarilauncher.ui.dialog.EditTextDialog

/**
 * シークバー形式の設定ラッパークラスです。
 * スライダーと手入力の両方で数値設定を編集できます。
 */
@SuppressLint("UseSwitchCompatOrMaterialCode", "StringFormatInvalid")
class SeekBarSettingsWrapper(
    val context: Context,
    val unit: IntSettingUnit,
    val mainView: View,
    val titleView: TextView,
    val summaryView: TextView,
    val valueView: TextView,
    val seekbarView: SeekBar,
    val suffix: String,
    onStartListener: OnStartInit?
) : AbstractSettingsWrapper(mainView) {
    private var listener: OnSeekBarProgressChangeListener? = null

    /**
     * 簡易コンストラクタです。初期化リスナーなしでインスタンスを生成します。
     */
    constructor(
        context: Context,
        unit: IntSettingUnit,
        mainView: View,
        titleView: TextView,
        summaryView: TextView,
        valueView: TextView,
        seekbarView: SeekBar,
        suffix: String,
    ) : this(
        context,
        unit,
        mainView,
        titleView,
        summaryView,
        valueView,
        seekbarView,
        suffix,
        null
    )

    /**
     * 初期化ブロックです。シークバーと値テキストの設定、クリックリスナーを設定します。
     */
    init {
        onStartListener?.onStart(this)

        seekbarView.progress = unit.getValue()
        valueView.background = ContextCompat.getDrawable(context, R.drawable.background_text)
        setSeekBarValueTextView()

        seekbarView.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            fun updateSeekbarValue(saveValue: Boolean) {
                val progress = seekbarView.progress

                listener?.onChange(progress)
                if (saveValue) unit.put(progress).save()
                setSeekBarValueTextView()
                checkShowRebootDialog(context)
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSeekbarValue(!fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateSeekbarValue(true)
            }
        })

        mainView.setOnClickListener {
            EditTextDialog.Builder(context)
                .setEditText(seekbarView.progress.toString())
                .setInputType(InputType.TYPE_CLASS_NUMBER)
                .setTitle(titleView.text.toString())
                .setMessage(summaryView.text.toString())
                .setAsRequired()
                .setConfirmListener { editBox, _ ->
                    val string = editBox.text.toString()

                    val value: Int
                    try {
                        value = string.toInt()
                    } catch (e: NumberFormatException) {
                        e("SeekBarSettingsWrapper", "The data is illegal", e)

                        editBox.error = context.getString(R.string.generic_input_invalid)
                        return@setConfirmListener false
                    }

                    if (value < seekbarView.min) {
                        val minValue =
                            String.format("%s %s", seekbarView.min, suffix)
                        editBox.error =
                            context.getString(R.string.generic_input_too_small, minValue)
                        return@setConfirmListener false
                    }
                    if (value > seekbarView.max) {
                        val maxValue =
                            String.format("%s %s", seekbarView.max, suffix)
                        editBox.error = context.getString(R.string.generic_input_too_big, maxValue)
                        return@setConfirmListener false
                    }

                    seekbarView.progress = value
                    true
                }.showDialog()
        }
    }

    /**
     * 現在のシークバーの値と接尾辞を組み合わせてテキストビューに表示します。
     */
    fun setSeekBarValueTextView() {
        val text = "${seekbarView.progress} $suffix".trim()
        valueView.text = text
    }

    /**
     * シークバーの進行値変更リスナーを設定します。
     * @param listener 進行値変更リスナー
     */
    fun setOnSeekBarProgressChangeListener(listener: OnSeekBarProgressChangeListener) {
        this.listener = listener
    }

    /**
     * シークバーがラップされる前に動的に最大値や最小値を調整するためのインターフェースです。
     */
    fun interface OnStartInit {
        fun onStart(wrapper: SeekBarSettingsWrapper)
    }

    /**
     * シークバーの進行値変更通知用の関数型インターフェースです。
     */
    fun interface OnSeekBarProgressChangeListener {
        fun onChange(progress: Int)
    }
}