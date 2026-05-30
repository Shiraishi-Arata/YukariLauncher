package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

/**
 * Krypton Wrapper（NGGL4ES）レンダラー実装
 */
class NGGL4ESRenderer : RendererInterface {
    /**
     * レンダラーIDを取得する
     */
    override fun getRendererId(): String = "nggl4es"

    /**
     * 一意識別子を取得する
     */
    override fun getUniqueIdentifier(): String = "303942ff-feb1-446f-923f-ba7d72726192"

    /**
     * 表示名を取得する
     */
    override fun getRendererName(): String = "Krypton Wrapper 0.4.5"

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
    override fun getRendererLibrary(): String = "libng_gl4es.so"
}
