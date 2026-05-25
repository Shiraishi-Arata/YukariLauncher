package com.arata.yukarilauncher.renderer

object GLInfoUtils {
    data class GLInfo(
        val isAdreno: Boolean = false,
        val glesMajorVersion: Int = 2,
        val vendor: String = "",
        val renderer: String = ""
    )

    fun getGlInfo(): GLInfo = GLInfo()
}
