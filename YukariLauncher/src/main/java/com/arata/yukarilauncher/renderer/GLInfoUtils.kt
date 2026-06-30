package com.arata.yukarilauncher.renderer

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.GLES20

object GLInfoUtils {
    data class GLInfo(
        val isAdreno: Boolean = false,
        val glesMajorVersion: Int = 2,
        val vendor: String = "",
        val renderer: String = ""
    )

    fun getGlInfo(): GLInfo {
        return try {
            val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) return GLInfo()
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return GLInfo()
            try {
                val configAttribs = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_NONE
                )
                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
                    return GLInfo()
                }
                val contextAttribs = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
                )
                val context = EGL14.eglCreateContext(eglDisplay, configs[0]!!, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
                if (context == EGL14.EGL_NO_CONTEXT) return GLInfo()
                try {
                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, context)
                    val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: ""
                    val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: ""
                    val isAdreno = vendor.equals("Qualcomm", ignoreCase = true) &&
                        renderer.lowercase().contains("adreno")
                    GLInfo(isAdreno, 2, vendor, renderer)
                } finally {
                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    EGL14.eglDestroyContext(eglDisplay, context)
                }
            } finally {
                EGL14.eglTerminate(eglDisplay)
            }
        } catch (e: Exception) {
            GLInfo()
        }
    }
}