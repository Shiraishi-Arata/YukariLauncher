package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

/**
 * Kopper Zinkレンダラー実装
 */
class KopperZinkRenderer : RendererInterface {
    /**
     * レンダラーIDを取得する
     */
    override fun getRendererId(): String = "kopper_zink"

    /**
     * 一意識別子を取得する
     */
    override fun getUniqueIdentifier(): String = "58604137-e1ad-49ae-81cd-10f82e25de94"

    /**
     * 表示名を取得する
     */
    override fun getRendererName(): String = "Kopper Zink"

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
    override fun getRendererLibrary(): String = "libglxshim.so"
}