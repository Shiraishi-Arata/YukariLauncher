package com.arata.yukarilauncher.renderer.renderers

import com.arata.yukarilauncher.renderer.RendererInterface

/**
 * Panfrost（Mali GPU）レンダラー実装
 */
class PanfrostRenderer : RendererInterface {
    /**
     * レンダラーIDを取得する
     */
    override fun getRendererId(): String = "gallium_panfrost"

    /**
     * 一意識別子を取得する
     */
    override fun getUniqueIdentifier(): String = "9b2808c4-11af-4c72-a9c6-94c940396475"

    /**
     * 表示名を取得する
     */
    override fun getRendererName(): String = "Panfrost (Mali)"

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
    override fun getRendererLibrary(): String = "libOSMesa_2300d.so"
}