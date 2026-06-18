package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface
import com.arata.yukarilauncher.utils.path.PathManager
import java.io.File

/**
 * VirGLレンダラー実装（仮想GPU）
 */
class VirGLRenderer : RendererInterface {
    /**
     * レンダラーIDを取得する
     */
    override fun getRendererId(): String = "gallium_virgl"

    /**
     * 一意識別子を取得する
     */
    override fun getUniqueIdentifier(): String = "a3ccc1fe-de3f-4a81-8c45-2485181b63b3"

    /**
     * 表示名を取得する
     */
    override fun getRendererName(): String = "VirGLRenderer"

    /**
     * 環境変数を取得する（VirGLテストソケットパスの設定）
     */
    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "VTEST_SOCKET_NAME" to File(PathManager.DIR_CACHE, ".virgl_test").absolutePath
        )
    }

    /**
     * dlopenが必要なライブラリリストを取得する
     */
    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    /**
     * レンダラーライブラリ名を取得する
     */
    override fun getRendererLibrary(): String = "libOSMesa_2121.so"
}