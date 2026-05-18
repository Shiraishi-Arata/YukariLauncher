package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

class KopperZinkRenderer : RendererInterface {
    override fun getRendererId(): String = "kopper_zink"

    override fun getUniqueIdentifier(): String = "58604137-e1ad-49ae-81cd-10f82e25de94"

    override fun getRendererName(): String = "Kopper Zink"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { emptyMap() }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libglxshim.so"
}