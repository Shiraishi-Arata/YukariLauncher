package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

class LTWRenderer : RendererInterface {
    override fun getRendererId(): String = "ltw"
    override fun getUniqueIdentifier(): String = "d24c0cad-2ec9-478b-9e19-84e6f4c9f0d1"
    override fun getRendererName(): String = "LTW (OpenGL 3.3, 1.13+)"
    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { emptyMap() }
    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }
    override fun getRendererLibrary(): String = "libltw.so"
}