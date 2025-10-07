package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface
import com.arata.yukarilauncher.utils.path.PathManager
import java.io.File

class VirGLRenderer2205 : RendererInterface {
    override fun getRendererId(): String = "gallium_virgl"

    override fun getUniqueIdentifier(): String = "b9f9e2c7-1234-4a5b-9f6a-abcdef123456"

    override fun getRendererName(): String = "VirGLRenderer (22.0.5)"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "VTEST_SOCKET_NAME" to File(PathManager.DIR_CACHE, ".virgl_test").absolutePath
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libOSMesa_2205.so"
}