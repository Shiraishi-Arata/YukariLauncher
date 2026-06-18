package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

class MobileGluesRenderer : RendererInterface {
    override fun getRendererId(): String = "opengles3"

    override fun getUniqueIdentifier(): String = UNIQUE_IDENTIFIER

    companion object {
        const val UNIQUE_IDENTIFIER = "47a02902-c0ac-4d60-9f66-aee5c37c43ad"
    }

    override fun getRendererName(): String = "MobileGlues (OpenGL 4.0, 1.17+)"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf("LIBGL_EGL" to "libmobileglues.so")
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libmobileglues.so"

    override fun getRendererEGL(): String? = "libmobileglues.so"
}