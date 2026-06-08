package com.arata.yukarilauncher.ui.fragment.settings

import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.SettingsFragmentVideoBinding
import com.arata.yukarilauncher.event.single.LauncherIgnoreNotchEvent
import com.arata.yukarilauncher.feature.graphics.GameGraphicsApiHelper
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.plugins.driver.DriverPluginManager
import com.arata.yukarilauncher.plugins.renderer.RendererPluginManager
import com.arata.yukarilauncher.renderer.Renderers
import com.arata.yukarilauncher.renderer.renderers.MobileGluesRenderer
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.LocalRendererPluginDialog
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.ui.fragment.settings.wrapper.BaseSettingsWrapper
import com.arata.yukarilauncher.ui.fragment.settings.wrapper.EditTextSettingsWrapper
import com.arata.yukarilauncher.ui.fragment.settings.wrapper.ListSettingsWrapper
import com.arata.yukarilauncher.ui.fragment.settings.wrapper.SeekBarSettingsWrapper
import com.arata.yukarilauncher.ui.fragment.settings.wrapper.SwitchSettingsWrapper
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.path.UrlManager

import com.google.android.material.materialswitch.MaterialSwitch

import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.ui.activity.OpenDocumentWithExtension
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 * 映像設定フラグメント
 */
class VideoSettingsFragment : AbstractSettingsFragment(R.layout.settings_fragment_video, SettingCategory.VIDEO) {
    private lateinit var binding: SettingsFragmentVideoBinding
    private lateinit var openDocumentLauncher: ActivityResultLauncher<Any>

    /**
     * フラグメント作成時にファイル選択ランチャーを初期化します。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension("zip", true)) { uris: List<Uri>? ->
            uris?.let { uriList ->
                val dialog = YLTools.showTaskRunningDialog(requireActivity())
                Task.runTask {
                    val pluginFiles = mutableListOf<File>()
                    uriList.forEach { uri ->
                        val file = FileTools.copyFileInBackground(requireActivity(), uri, PathManager.DIR_CACHE.absolutePath)
                        pluginFiles.add(file)
                    }
                    pluginFiles.takeIf { it.isNotEmpty() }
                }.beforeStart(TaskExecutors.getAndroidUI()) {
                    dialog.show()
                }.ended { pluginFiles ->
                    pluginFiles?.let { files ->
                        var requiresRestart = false
                        files.forEach { pluginFile ->
                            val info = if (RendererPluginManager.importLocalRendererPlugin(pluginFile)) {
                                requiresRestart = true
                                "The renderer plugin has been successfully imported!"
                            } else {
                                "The renderer plugin import failed!"
                            }
                            Logging.i("VideoSettings", info)
                            FileUtils.deleteQuietly(pluginFile)
                        }
                        TaskExecutors.runInUIThread {
                            if (requiresRestart) {
                                TipDialog.Builder(requireActivity())
                                    .setTitle(R.string.generic_warning)
                                    .setMessage(R.string.setting_renderer_local_import_restart)
                                    .setWarning()
                                    .setConfirmClickListener { YLTools.killProcess() }
                                    .showDialog()
                            } else {
                                TipDialog.Builder(requireActivity())
                                    .setTitle(R.string.generic_tip)
                                    .setMessage(R.string.setting_renderer_local_import_failed)
                                    .showDialog()
                            }
                        }
                    }
                }.onThrowable { e ->
                    Tools.showErrorRemote(e)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    dialog.dismiss()
                }.execute()
            }
        }
    }

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsFragmentVideoBinding.inflate(layoutInflater)
        return binding.root
    }

    /**
     * ビュー作成後の初期化処理を行います。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireActivity()

        val renderers = Renderers.getCompatibleRenderers(context).first
        ListSettingsWrapper(
            context,
            AllSettings.renderer,
            binding.rendererLayout,
            binding.rendererTitle,
            binding.rendererValue,
            renderers.rendererNames.toTypedArray(),
            renderers.rendererIdentifier.toTypedArray()
        )

        ListSettingsWrapper(
            context,
            AllSettings.gameGraphicsApi,
            binding.gameGraphicsApiLayout,
            binding.gameGraphicsApiTitle,
            binding.gameGraphicsApiValue,
            context.resources.getStringArray(R.array.setting_game_graphics_api_entries),
            context.resources.getStringArray(R.array.setting_game_graphics_api_values),
            onValueSelected = { selectedValue ->
                if (selectedValue == GameGraphicsApiHelper.SETTING_VULKAN &&
                    !GameGraphicsApiHelper.isVulkan12Supported(context)
                ) {
                    TipDialog.Builder(requireActivity())
                        .setTitle(R.string.generic_warning)
                        .setMessage(buildVulkanUnsupportedMessage(context))
                        .setWarning()
                        .showDialog()
                    false
                } else {
                    true
                }
            }
        )

        binding.rendererDownload.setOnClickListener { YLTools.openLink(context, UrlManager.URL_FCL_RENDERER_PLUGIN) }

        BaseSettingsWrapper(
            context,
            binding.rendererLocalImportLayout
        ) {
            openDocumentLauncher.launch("zip")
        }

        binding.rendererLocalImportManage.setOnClickListener {
            if (RendererPluginManager.getAllLocalRendererList().isNotEmpty()) {
                LocalRendererPluginDialog(requireActivity()).show()
            }
        }

        val driverNames = DriverPluginManager.getDriverNameList().toTypedArray()
        ListSettingsWrapper(
            context,
            AllSettings.driver,
            binding.driverLayout,
            binding.driverTitle,
            binding.driverValue,
            driverNames,
            driverNames
        )

        binding.driverDownload.setOnClickListener { YLTools.openLink(context, UrlManager.URL_FCL_DRIVER_PLUGIN) }

        val ignoreNotch = SwitchSettingsWrapper(
            context,
            AllSettings.ignoreNotch,
            binding.ignoreNotchLayout,
            binding.ignoreNotch
        )

        val ignoreNotchLauncher = SwitchSettingsWrapper(
            context,
            AllSettings.ignoreNotchLauncher,
            binding.ignoreNotchLauncherLayout,
            binding.ignoreNotchLauncher
        ).setOnCheckedChangeListener { _, _, listener ->
            listener.onSave()
            EventBus.getDefault().post(LauncherIgnoreNotchEvent())
        }

        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && AllStaticSettings.notchSize > 0)) {
            ignoreNotch.setGone()
            ignoreNotchLauncher.setGone()
        }

        SeekBarSettingsWrapper(
            context,
            AllSettings.resolutionRatio,
            binding.resolutionRatioLayout,
            binding.resolutionRatioTitle,
            binding.resolutionRatioSummary,
            binding.resolutionRatioValue,
            binding.resolutionRatio,
            "%"
        ).setOnSeekBarProgressChangeListener { progress ->
            changeResolutionRatioPreview(progress)
        }

        SwitchSettingsWrapper(
            context,
            AllSettings.sustainedPerformance,
            binding.sustainedPerformanceLayout,
            binding.sustainedPerformance
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.alternateSurface,
            binding.alternateSurfaceLayout,
            binding.alternateSurface
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.forceVsync,
            binding.forceVsyncLayout,
            binding.forceVsync
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.vsyncInZink,
            binding.vsyncInZinkLayout,
            binding.vsyncInZink
        )

        val zinkPreferSystemDriver = SwitchSettingsWrapper(
            context,
            AllSettings.zinkPreferSystemDriver,
            binding.zinkPreferSystemDriverLayout,
            binding.zinkPreferSystemDriver
        )
        if (!Tools.checkVulkanSupport(context.packageManager)) {
            zinkPreferSystemDriver.setGone()
        } else {
            zinkPreferSystemDriver.setOnCheckedChangeListener { buttonView, isChecked, listener ->
                if (isChecked and YLTools.isAdrenoGPU()) {
                    TipDialog.Builder(requireActivity())
                        .setTitle(R.string.generic_warning)
                        .setMessage(R.string.setting_zink_driver_adreno)
                        .setWarning()
                        .setCancelable(false)
                        .setConfirmClickListener { listener.onSave() }
                        .setCancelClickListener { buttonView.isChecked = false }
                        .showDialog()
                } else {
                    listener.onSave()
                }
            }
        }

        // MobileGlues 設定の初期化
        initMobileGluesSettings(context)

        changeResolutionRatioPreview(AllSettings.resolutionRatio.getValue())
        computeVisibility()
    }

    /**
     * MobileGlues レンダラー設定のUIを初期化する
     */
    private fun initMobileGluesSettings(context: android.content.Context) {
        // GLSLキャッシュサイズ
        EditTextSettingsWrapper(
            AllSettings.mgGlslCacheSize,
            binding.mgGlslCacheLayout,
            binding.mgGlslCacheInput
        )

        // ANGLEドライバー
        ListSettingsWrapper(
            context,
            AllSettings.mgAngle,
            binding.mgAngleLayout,
            binding.mgAngleTitle,
            binding.mgAngleValue,
            R.array.setting_mg_angle_entries,
            R.array.setting_mg_angle_values
        )

        // OpenGLエラー設定
        ListSettingsWrapper(
            context,
            AllSettings.mgNoError,
            binding.mgNoErrorLayout,
            binding.mgNoErrorTitle,
            binding.mgNoErrorValue,
            R.array.setting_mg_no_error_entries,
            R.array.setting_mg_no_error_values
        )

        // マルチドローエミュレーション
        ListSettingsWrapper(
            context,
            AllSettings.mgMultidrawMode,
            binding.mgMultidrawLayout,
            binding.mgMultidrawTitle,
            binding.mgMultidrawValue,
            R.array.setting_mg_multidraw_entries,
            R.array.setting_mg_multidraw_values
        )

        // ANGLE深度クリア回避策
        ListSettingsWrapper(
            context,
            AllSettings.mgAngleDepthClearFixMode,
            binding.mgAngleClearLayout,
            binding.mgAngleClearTitle,
            binding.mgAngleClearValue,
            R.array.setting_mg_angle_clear_entries,
            R.array.setting_mg_angle_clear_values
        )

        // カスタムOpenGLバージョン
        ListSettingsWrapper(
            context,
            AllSettings.mgCustomGLVersion,
            binding.mgCustomGlLayout,
            binding.mgCustomGlTitle,
            binding.mgCustomGlValue,
            R.array.setting_mg_custom_gl_version_entries,
            R.array.setting_mg_custom_gl_version_values
        )

        // ARB_compute_shader拡張
        SwitchSettingsWrapper(
            context,
            AllSettings.mgExtComputeShader,
            binding.mgExtComputeShaderLayout,
            binding.mgExtComputeShader
        )

        // timer_query拡張（UIは反転: ON=推奨設定=有効）
        SwitchSettingsWrapper(
            context,
            AllSettings.mgExtTimerQuery,
            binding.mgExtTimerQueryLayout,
            binding.mgExtTimerQuery
        )

        // direct_state_access拡張
        SwitchSettingsWrapper(
            context,
            AllSettings.mgExtDirectStateAccess,
            binding.mgExtDirectStateAccessLayout,
            binding.mgExtDirectStateAccess
        )

        // フレーム生成（FG）
        val fgWarning = SwitchSettingsWrapper(
            context,
            AllSettings.mgFrameGeneration,
            binding.mgFrameGenerationLayout,
            binding.mgFrameGeneration
        )
        fgWarning.setOnCheckedChangeListener { buttonView, isChecked, listener ->
            if (isChecked) {
                TipDialog.Builder(requireActivity())
                    .setTitle(R.string.generic_warning)
                    .setMessage(R.string.setting_mg_frame_generation_warning)
                    .setWarning()
                    .setCancelable(false)
                    .setConfirmClickListener { listener.onSave() }
                    .setCancelClickListener { buttonView.isChecked = false }
                    .showDialog()
            } else {
                listener.onSave()
            }
        }

        // FSR1
        ListSettingsWrapper(
            context,
            AllSettings.mgFsr1,
            binding.mgFsr1Layout,
            binding.mgFsr1Title,
            binding.mgFsr1Value,
            R.array.setting_mg_fsr1_entries,
            R.array.setting_mg_fsr1_values
        )

        // F3画面からMGを隠す
        SwitchSettingsWrapper(
            context,
            AllSettings.mgHideMG,
            binding.mgHideF3Layout,
            binding.mgHideF3
        )
    }

    /**
     * 解像度比率のプレビューを更新する
     */
    /**
     * 解像度比率のプレビューを更新します。
     */
    private fun changeResolutionRatioPreview(progress: Int) {
        binding.resolutionRatioPreview.text = getResolutionRatioPreview(resources, progress)
    }

    /**
     * 設定変更時に表示/非表示を再計算します。
     */
    override fun onChange() {
        super.onChange()
        computeVisibility()
    }

    /**
     * 設定変更に応じて表示/非表示を切り替えます。
     */
    private fun computeVisibility() {
        binding.apply {
            binding.forceVsyncLayout.visibility = if (AllSettings.alternateSurface.getValue()) View.VISIBLE else View.GONE

            // MobileGlues 詳細設定の表示/非表示（レンダラー選択に応じて展開）
            val isMobileGlues = AllSettings.renderer.getValue() == MobileGluesRenderer.UNIQUE_IDENTIFIER
            binding.mgContainer.visibility = if (isMobileGlues) View.VISIBLE else View.GONE
        }
    }

    /**
     * スライドインアニメーションを実行します。
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.root, Animations.BounceInDown))
    }

    /**
     * Vulkan非対応メッセージを構築します。
     */
    private fun buildVulkanUnsupportedMessage(context: android.content.Context): String {
        val status = GameGraphicsApiHelper.getVulkanStatus(context)
        val supportText = { supported: Boolean -> if (supported) "Supported" else "Unsupported" }
        return buildString {
            append(getString(R.string.setting_game_graphics_api_vulkan_unsupported))
            append("\n\n")
            append("Vulkan version ${status.version} ")
            append(supportText(status.vulkan12Supported))
            append("\n")
            append("VK_KHR_dynamic_rendering ")
            append(supportText(status.dynamicRenderingSupported))
            append("\n")
            append("VK_KHR_push_descriptor ")
            append(supportText(status.pushDescriptorSupported))
        }
    }

    companion object {
        @JvmStatic
        fun getResolutionRatioPreview(resources: Resources, progress: Int): String {
            val metrics = Tools.currentDisplayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || width > height

            val progressFloat = progress.toFloat() / 100F
            val previewWidth = Tools.getDisplayFriendlyRes((if (isLandscape) width else height), progressFloat)
            val previewHeight = Tools.getDisplayFriendlyRes((if (isLandscape) height else width), progressFloat)

            return "$previewWidth x $previewHeight"
        }
    }
}