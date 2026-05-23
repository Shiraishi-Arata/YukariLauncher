package com.arata.yukarilauncher.ui.subassembly.view

import android.app.Activity
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.getkeepsafe.taptargetview.TapTargetView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.utils.NewbieGuideUtils
import com.arata.yukarilauncher.utils.file.FileTools.Companion.formatFileSize
import com.arata.yukarilauncher.utils.platform.MemoryUtils
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.listener.control.IFxScopeControl
import com.petterp.floatingx.view.FxViewHolder
import org.lwjgl.glfw.CallbackBridge
import java.util.Timer
import java.util.TimerTask

/**
 * ゲームメニューのフローティングビューをラップするクラス。
 * @param activity アクティビティ
 * @param listener クリックリスナー
 * @param showInfo 情報表示を行うかどうか
 */
class GameMenuViewWrapper(
    private val activity: Activity,
    private val listener: View.OnClickListener,
    private val showInfo: Boolean
) {
    companion object {
        private const val TAG = "GameMenuViewWrapper"
    }

    private var timer: Timer? = null
    private val memoryText: String = AllSettings.gameMenuMemoryText.getValue()
    private var showMemory: Boolean = false
    private var showFPS: Boolean = false
    private var visible: Boolean = false

    private var scopeFx: IFxScopeControl? = null
    private val defaultTextColor: Int by lazy { ContextCompat.getColor(activity, R.color.primary_text) }

    init {
        refreshState()
    }

    /**
     * フローティングウィンドウの制御インスタンスを取得する
     */
    private fun getWindow(): IFxScopeControl {
        return FxScopeHelper.Builder().apply {
            setLayout(R.layout.view_game_menu_window)
            setOnClickListener(0L, listener)
            setEnableEdgeAdsorption(false)
            addViewLifecycle(object : IFxViewLifecycle {
                override fun initView(holder: FxViewHolder) {
                    holder.view.alpha = AllSettings.gameMenuAlpha.getValue().toFloat() / 100f

                    updateInfoText(holder.view)
                }

                override fun detached(view: View) {
                    cancelInfoTimer()
                }
            })
            setGravity(getCurrentGravity())
        }.build().toControl(activity)
    }

    /**
     * ニュービーガイドを開始する
     */
    private fun startNewbieGuide(mainView: View) {
        if (NewbieGuideUtils.showOnlyOne(TAG)) return
        TapTargetView.showFor(
            activity,
            NewbieGuideUtils.getSimpleTarget(activity, mainView,
                activity.getString(R.string.setting_category_game_menu),
                activity.getString(R.string.newbie_guide_game_menu)
            )
        )
    }

    /**
     * 表示状態を設定する
     */
    fun setVisibility(visible: Boolean) {
        this.visible = visible
        thinkForVisibility()
    }

    /**
     * 設定状態を更新する
     */
    fun refreshSettingsState() {
        refreshState()
        thinkForVisibility()
    }

    /**
     * 3つの条件（表示フラグ、メモリ情報表示、FPS表示）に基づいてフローティングウィンドウの表示を判断する
     */
    private fun thinkForVisibility() {
        val v1 = visible || showMemory || showFPS
        if (v1) {
            if (scopeFx != null) {
                updateInfoText()
            } else {
                scopeFx = getWindow().apply {
                    updateInfoText()
                    show()
                    getView()?.let { startNewbieGuide(it) }
                }
            }
        } else {
            scopeFx?.cancel()
            scopeFx = null
            cancelInfoTimer()
        }
    }

    /**
     * 設定からメモリ/FPS表示フラグを更新する
     */
    private fun refreshState() {
        showMemory = AllSettings.gameMenuShowMemory.getValue()
        showFPS = AllSettings.gameMenuShowFPS.getValue()
    }

    /**
     * 現在のビューの情報テキストを更新する
     */
    private fun updateInfoText() {
        scopeFx?.getView()?.apply {
            updateInfoText(this)
        }
    }

    /**
     * 指定されたビューのメモリ・FPS情報テキストを更新する
     */
    private fun updateInfoText(view: View) {
        cancelInfoTimer()

        val memoryText: TextView = view.findViewById(R.id.memory_text)
        val fpsText: TextView = view.findViewById(R.id.fps_text)

        fun updateInfoText() {
            if (showMemory) {
                val used = MemoryUtils.getUsedDeviceMemory(activity)
                val total = MemoryUtils.getTotalDeviceMemory(activity)
                val valueString = "${formatFileSize(used)}/${formatFileSize(total)}"
                val memoryString = "${this@GameMenuViewWrapper.memoryText} $valueString".let { string ->
                    if (string.length > 40) return@let string.take(40)
                    string
                }
                TaskExecutors.runInUIThread {
                    val spannable = SpannableStringBuilder(memoryString)
                    val valueStart = memoryString.indexOf(valueString)
                    val valueEnd = valueStart + valueString.length
                    spannable.setSpan(
                        ForegroundColorSpan(getColorForMemory(used, total)),
                        valueStart,
                        valueEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    memoryText.setTextColor(defaultTextColor)
                    memoryText.text = spannable
                }
            }
            if (showFPS) {
                val fpsValue = CallbackBridge.getCurrentFps()
                val fpsValueString = fpsValue.toString()
                val fpsString = "FPS: $fpsValueString"
                TaskExecutors.runInUIThread {
                    val spannable = SpannableStringBuilder(fpsString)
                    val valueStart = fpsString.indexOf(fpsValueString)
                    val valueEnd = valueStart + fpsValueString.length
                    spannable.setSpan(
                        ForegroundColorSpan(getColorForFps(fpsValue)),
                        valueStart,
                        valueEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    fpsText.setTextColor(defaultTextColor)
                    fpsText.text = spannable
                }
            }
        }

        updateInfoText()

        if (showInfo) {
            memoryText.visibility = if (showMemory) View.VISIBLE else View.GONE
            fpsText.visibility = if (showFPS) View.VISIBLE else View.GONE

            if (showMemory || showFPS) {
                timer = Timer().apply {
                    schedule(object : TimerTask() {
                        override fun run() {
                            updateInfoText()
                        }
                    }, 0, AllSettings.gameMenuInfoRefreshRate.getValue().toLong())
                }
            }
        }
    }

    /**
     * 使用中のデバイスメモリサイズを取得する
     */
    private fun getUsedDeviceMemory(): String = formatFileSize(MemoryUtils.getUsedDeviceMemory(activity))

    /**
     * 総デバイスメモリサイズを取得する
     */
    private fun getTotalDeviceMemory(): String = formatFileSize(MemoryUtils.getTotalDeviceMemory(activity))

    /**
     * FPS値に応じた表示色を取得する
     */
    private fun getColorForFps(fps: Int): Int {
        return when {
            fps >= 60 -> ContextCompat.getColor(activity, R.color.status_good)
            fps >= 30 -> ContextCompat.getColor(activity, R.color.status_warning)
            else -> ContextCompat.getColor(activity, R.color.status_error)
        }
    }

    /**
     * メモリ使用率に応じた表示色を取得する
     */
    private fun getColorForMemory(usedBytes: Long, totalBytes: Long): Int {
        val ratio = if (totalBytes > 0) usedBytes.toDouble() / totalBytes else 0.0
        return when {
            ratio <= 0.5 -> ContextCompat.getColor(activity, R.color.status_good)
            ratio <= 0.75 -> ContextCompat.getColor(activity, R.color.status_warning)
            else -> ContextCompat.getColor(activity, R.color.status_error)
        }
    }

    /**
     * 情報更新タイマーをキャンセルする
     */
    private fun cancelInfoTimer() {
        timer?.cancel()
        timer = null
    }

    /**
     * 現在の設定に基づくフローティングウィンドウの表示位置を取得する
     */
    private fun getCurrentGravity(): FxGravity {
        return when(AllSettings.gameMenuLocation.getValue()) {
            "left_or_top" -> FxGravity.LEFT_OR_TOP
            "left_or_bottom" -> FxGravity.LEFT_OR_BOTTOM
            "right_or_top" -> FxGravity.RIGHT_OR_TOP
            "right_or_bottom" -> FxGravity.RIGHT_OR_BOTTOM
            "top_or_center" -> FxGravity.TOP_OR_CENTER
            "bottom_or_center" -> FxGravity.BOTTOM_OR_CENTER
            "center" -> FxGravity.CENTER
            else -> FxGravity.CENTER
        }
    }
}
