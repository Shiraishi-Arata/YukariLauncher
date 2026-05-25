package com.arata.yukarilauncher.renderer

import android.content.Context
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.renderer.renderers.FreedrenoRenderer
import com.arata.yukarilauncher.renderer.renderers.GL4ESRenderer
import com.arata.yukarilauncher.renderer.renderers.NGGL4ESRenderer
import com.arata.yukarilauncher.renderer.renderers.PanfrostRenderer
import com.arata.yukarilauncher.renderer.renderers.VirGLRenderer
import com.arata.yukarilauncher.renderer.renderers.KopperZinkRenderer
import com.arata.yukarilauncher.renderer.renderers.VulkanZinkRenderer
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.Tools

/**
 * ランチャーの全レンダラー管理
 * 内蔵レンダラーとプラグインで読み込まれたレンダラーがここに集約される
 */
object Renderers {
    private val renderers: MutableList<RendererInterface> = mutableListOf()
    private var compatibleRenderers: Pair<RenderersList, MutableList<RendererInterface>>? = null
    private var currentRenderer: RendererInterface? = null
    private var isInitialized: Boolean = false

    /**
     * レンダラーを初期化する
     * 内蔵レンダラーを登録する
     * @param reset 初期化するかどうか
     */
    fun init(reset: Boolean = false) {
        if (isInitialized && !reset) return
        isInitialized = true

        if (reset) {
            renderers.clear()
            compatibleRenderers = null
            currentRenderer = null
        }

        addRenderers(
            GL4ESRenderer(),
            NGGL4ESRenderer(),
            KopperZinkRenderer(),
            VulkanZinkRenderer(),
            VirGLRenderer(),
            FreedrenoRenderer(),
            PanfrostRenderer()
        )
    }

    /**
     * 現在のデバイスと互換性のあるレンダラー一覧を取得する
     * Vulkan対応やZinkバイナリの有無などを考慮する
     * @param context コンテキスト
     * @return レンダラーリストとレンダラー情報のペア
     */
    fun getCompatibleRenderers(context: Context): Pair<RenderersList, List<RendererInterface>> = compatibleRenderers ?: run {
        val deviceHasVulkan = Tools.checkVulkanSupport(context.packageManager)
        // 現在、32ビットx86のみZinkバイナリがない
        val deviceHasZinkBinary = !(Architecture.is32BitsDevice() && Architecture.isx86Device())

        val compatibleRenderers1: MutableList<RendererInterface> = mutableListOf()
        renderers.forEach { renderer ->
            if (renderer.getRendererId().contains("vulkan") && !deviceHasVulkan) return@forEach
            if (renderer.getRendererId().contains("zink") && !deviceHasZinkBinary) return@forEach
            compatibleRenderers1.add(renderer)
        }

        val rendererIdentifiers: MutableList<String> = mutableListOf()
        val rendererNames: MutableList<String> = mutableListOf()
        compatibleRenderers1.forEach { renderer ->
            rendererIdentifiers.add(renderer.getUniqueIdentifier())
            rendererNames.add(renderer.getRendererName())
        }

        val rendererPair = Pair(RenderersList(rendererIdentifiers, rendererNames), compatibleRenderers1)
        compatibleRenderers = rendererPair
        rendererPair
    }

    /**
     * 複数のレンダラーを追加する
     */
    @JvmStatic
    fun addRenderers(vararg renderers: RendererInterface) {
        renderers.forEach { renderer ->
            addRenderer(renderer)
        }
    }

    /**
     * 単一のレンダラーを追加する
     * @return 追加成功時はtrue（一意識別子が重複している場合はfalse）
     */
    @JvmStatic
    fun addRenderer(renderer: RendererInterface): Boolean {
        return if (this.renderers.any { it.getUniqueIdentifier() == renderer.getUniqueIdentifier() }) {
            Logging.w("Renderers", "The unique identifier of this renderer (${renderer.getRendererName()} - ${renderer.getUniqueIdentifier()}) conflicts with an already loaded renderer. " +
                    "Normally, this shouldn't happen. You deliberately caused this conflict, didn't you, user?")
            false
        } else {
            this.renderers.add(renderer)
            Logging.i("Renderers", "Renderer loaded: ${renderer.getRendererName()} (${renderer.getRendererId()} - ${renderer.getUniqueIdentifier()})")
            true
        }
    }

    /**
     * 現在のレンダラーを設定する
     * @param context デバイス互換性の初期化に使用
     * @param uniqueIdentifier 設定したいレンダラーの一意識別子
     * @param retryToFirstOnFailure 一致するレンダラーがない場合、先頭のレンダラーにフォールバックするかどうか
     */
    fun setCurrentRenderer(context: Context, uniqueIdentifier: String, retryToFirstOnFailure: Boolean = true) {
        if (!isInitialized) throw IllegalStateException("Uninitialized renderer!")
        val compatibleRenderers = getCompatibleRenderers(context).second
        currentRenderer = compatibleRenderers.find { it.getUniqueIdentifier() == uniqueIdentifier } ?: run {
            if (retryToFirstOnFailure) {
                val renderer = compatibleRenderers[0]
                Logging.w("Renderers", "Incompatible renderer $uniqueIdentifier will be replaced with ${renderer.getUniqueIdentifier()} (${renderer.getRendererName()})")
                renderer
            } else null
        }
    }

    /**
     * 現在のレンダラーを取得する
     * @throws IllegalStateException 初期化されていない場合
     */
    fun getCurrentRenderer(): RendererInterface {
        if (!isInitialized) throw IllegalStateException("Uninitialized renderer!")
        return currentRenderer ?: throw IllegalStateException("Current renderer not set")
    }

    /**
     * 現在有効なレンダラーが設定されているかどうかを返す
     */
    fun isCurrentRendererValid(): Boolean = isInitialized && this.currentRenderer != null
}
