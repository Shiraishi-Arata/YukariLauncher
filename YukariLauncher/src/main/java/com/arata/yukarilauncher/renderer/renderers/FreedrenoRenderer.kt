package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

/**
 * Freedreno（Adreno GPU用オープンソースドライバー）レンダラー実装
 */
class FreedrenoRenderer : RendererInterface {
    /**
     * レンダラーIDを取得する
     */
    override fun getRendererId(): String = "gallium_freedreno"

    /**
     * 一意識別子を取得する
     */
    override fun getUniqueIdentifier(): String = "1ad7249f-5784-4f00-bc72-174b3578ee46"

    /**
     * 表示名を取得する
     */
    override fun getRendererName(): String = "Freedreno (Adreno)"

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
    override fun getRendererLibrary(): String = "libOSMesa_8.so"
}
