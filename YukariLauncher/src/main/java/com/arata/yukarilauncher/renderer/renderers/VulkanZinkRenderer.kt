package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

/**
 * Vulkan Zinkレンダラー実装
 * Vulkan上でOpenGLをエミュレートするZinkドライバーを使用する
 */
class VulkanZinkRenderer : RendererInterface {
    /**
     * レンダラーIDを取得する
     */
    override fun getRendererId(): String = "vulkan_zink"

    /**
     * 一意識別子を取得する
     */
    override fun getUniqueIdentifier(): String = "0fa435e2-46df-45c9-906c-b29606aaef00"

    /**
     * 表示名を取得する
     */
    override fun getRendererName(): String = "Vulkan Zink"

    /**
     * 環境変数を取得する（OpenGLバージョンのオーバーライド設定）
     */
    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460"
        )
    }

    /**
     * dlopenが必要なライブラリリストを取得する
     */
    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    /**
     * レンダラーライブラリ名を取得する
     */
    override fun getRendererLibrary(): String = "libOSMesa_8.so"
}
