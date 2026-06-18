package com.arata.yukarilauncher.renderer

/**
 * ランチャーレンダラーの実装インターフェース
 * 各レンダラーはこのインターフェースを実装する
 */
interface RendererInterface {
    /**
     * レンダラーIDを取得する
     */
    fun getRendererId(): String

    /**
     * レンダラーの一意識別子を取得する
     */
    fun getUniqueIdentifier(): String

    /**
     * レンダラーの表示名を取得する
     */
    fun getRendererName(): String

    /**
     * レンダラーの環境変数を取得する
     */
    fun getRendererEnv(): Lazy<Map<String, String>>

    /**
     * dlopenが必要なライブラリリストを取得する
     */
    fun getDlopenLibrary(): Lazy<List<String>>

    /**
     * レンダラーライブラリ名を取得する
     */
    fun getRendererLibrary(): String

    /**
     * EGLライブラリ名を取得する（既定値はnull）
     */
    fun getRendererEGL(): String? = null
}