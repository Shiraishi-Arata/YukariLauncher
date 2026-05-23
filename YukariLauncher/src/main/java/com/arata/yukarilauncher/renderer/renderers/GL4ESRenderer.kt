package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

/**
 * GL4ESレンダラー実装
 */
class GL4ESRenderer : RendererInterface {
    /**
     * レンダラーIDを取得する
     */
    override fun getRendererId(): String = "opengles2"

    /**
     * 一意識別子を取得する
     */
    override fun getUniqueIdentifier(): String = "8b52d82d-8f6d-4d3a-a767-dc93f8b72fc7"

    /**
     * 表示名を取得する
     */
    override fun getRendererName(): String = "GL4ES"

    /**
     * 環境変数を取得する
     */
    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { emptyMap() }

    /**
     * dlopenが必要なライブラリリストを取得する
     */
    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    /**
     * レンダラーライブラリ名を取得する
     */
    override fun getRendererLibrary(): String = "libgl4es_114.so"
}
