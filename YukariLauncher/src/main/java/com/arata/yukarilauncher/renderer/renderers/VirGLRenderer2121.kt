package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface
import com.arata.yukarilauncher.utils.path.PathManager
import java.io.File

class VirGLRenderer2121 : RendererInterface {
    override fun getRendererId(): String = "gallium_virgl"

    override fun getUniqueIdentifier(): String = "a3ccc1fe-de3f-4a81-8c45-2485181b63b3"

    override fun getRendererName(): String = "VirGLRenderer (21.2.1)"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "VTEST_SOCKET_NAME" to File(PathManager.DIR_CACHE, ".virgl_test").absolutePath
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libOSMesa_2121.so"
}