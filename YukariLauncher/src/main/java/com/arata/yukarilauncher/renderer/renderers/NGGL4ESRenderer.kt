package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

class NGGL4ESRenderer : RendererInterface {
    override fun getRendererId(): String = "nggl4es"

    override fun getUniqueIdentifier(): String = "303942ff-feb1-446f-923f-ba7d72726192"

    override fun getRendererName(): String = "Krypton Wrapper"

override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { emptyMap() }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libng_gl4es.so"
}