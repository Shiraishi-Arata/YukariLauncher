package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

class VulkanZinkRenderer : RendererInterface {
    override fun getRendererId(): String = "vulkan_zink"

    override fun getUniqueIdentifier(): String = "0fa435e2-46df-45c9-906c-b29606aaef00"

    override fun getRendererName(): String = "Vulkan Zink"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460",
            "MESA_GLES_VERSION_OVERRIDE" to "3.3",
            "MESA_GLSL_CACHE_DIR" to "/data/data/com.arata.yukarilauncher/cache", // writable cache
            "MESA_GLSL_FORCE_COMPILER" to "1",    // use LLVM
            "MESA_VK_THREAD_SUBMIT" to "1",       // Vulkan multithreaded submit
            "ZINK_USE_THREADED_CMDBUF" to "1",    // optional, improves multithreaded GL
            "MESA_SHADER_CACHE_DISABLE" to "0"    // ensure caching is enabled
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libOSMesa_8.so"
}