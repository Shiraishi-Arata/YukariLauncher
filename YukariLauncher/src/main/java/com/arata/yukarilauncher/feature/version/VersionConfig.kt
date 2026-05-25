package com.arata.yukarilauncher.feature.version

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.JsonParser
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.VersionsManager.getYukariVersionPath
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.utils.stringutils.StringUtils.getStringNotNull
import com.arata.yukarilauncher.Tools
import java.io.File
import java.io.FileWriter

class VersionConfig(private var versionPath: File) : Parcelable {
    private var isolationType: IsolationType = IsolationType.FOLLOW_GLOBAL
    private var javaDir: String = ""
    private var javaArgs: String = ""
    private var renderer: String = ""
    private var driver: String = ""
    private var control: String = ""
    private var customPath: String = ""
    private var customInfo: String = ""
    private var gameArgs: String = ""
    private var lwjglVersion: String = ""

    /**
     * 指定されたファイルパスと設定値でVersionConfigを作成する
     * @param filePath バージョンパス
     * @param isolationType 分離タイプ
     * @param javaDir Javaディレクトリ
     * @param javaArgs Java引数
     * @param renderer レンダラー
     * @param driver ドライバー
     * @param control コントロール
     * @param customPath カスタムパス
     * @param customInfo カスタム情報
     * @param gameArgs ゲーム引数
     */
    constructor(
        filePath: File,
        isolationType: IsolationType = IsolationType.FOLLOW_GLOBAL,
        javaDir: String = "",
        javaArgs: String = "",
        renderer: String = "",
        driver: String = "",
        control: String = "",
        customPath: String = "",
        customInfo: String = "",
        gameArgs: String = "",
        lwjglVersion: String = ""
    ) : this(filePath) {
        this.isolationType = isolationType
        this.javaDir = javaDir
        this.javaArgs = javaArgs
        this.renderer = renderer
        this.driver = driver
        this.control = control
        this.customPath = customPath
        this.customInfo = customInfo
        this.gameArgs = gameArgs
        this.lwjglVersion = lwjglVersion
    }

    /**
     * 現在の設定をコピーした新しいVersionConfigを生成する
     * @return コピーされたVersionConfig
     */
    fun copy(): VersionConfig = VersionConfig(versionPath,
        getIsolationTypeNotNull(isolationType),
        getStringNotNull(javaDir),
        getStringNotNull(javaArgs),
        getStringNotNull(renderer),
        getStringNotNull(driver),
        getStringNotNull(control),
        getStringNotNull(customPath),
        getStringNotNull(customInfo),
        getStringNotNull(gameArgs),
        getStringNotNull(lwjglVersion)
    )

    /**
     * 設定をファイルに保存する（エラーハンドリング付き）
     */
    fun save() {
        runCatching {
            saveWithThrowable()
        }.onFailure { e ->
            Logging.e("Save Version Config", "$this\n${Tools.printToString(e)}")
        }
    }

    /**
     * 設定をファイルに保存する（例外をスローする可能性あり）
     */
    @Throws(Throwable::class)
    fun saveWithThrowable() {
        Logging.i("Save Version Config", "Trying to save: $this")
        val yukariVersionPath = getYukariVersionPath(versionPath)
        val configFile = File(yukariVersionPath, "VersionConfig.json")
        if (!yukariVersionPath.exists()) yukariVersionPath.mkdirs()

        FileWriter(configFile, false).use {
            val json = Tools.GLOBAL_GSON.toJson(this)
            it.write(json)
        }
        Logging.i("Save Version Config", "Saved: $this")
    }

    /**
     * @return バージョンパスを取得する
     */
    fun getVersionPath() = versionPath

    /**
     * バージョンパスを設定する
     * @param versionPath バージョンパス
     */
    fun setVersionPath(versionPath: File) {
        this.versionPath = versionPath
    }

    /**
     * バージョン分離が有効かどうかを判定する
     * @return 分離が有効な場合はtrue
     */
    fun isIsolation(): Boolean = when(getIsolationTypeNotNull(isolationType)) {
        IsolationType.FOLLOW_GLOBAL -> AllSettings.versionIsolation.getValue()
        IsolationType.ENABLE -> true
        IsolationType.DISABLE -> false
    }

    /**
     * @return 分離タイプを取得する
     */
    fun getIsolationType() = getIsolationTypeNotNull(isolationType)

    /**
     * 分離タイプを設定する
     * @param isolationType 分離タイプ
     */
    fun setIsolationType(isolationType: IsolationType) { this.isolationType = isolationType }

    /**
     * @return Javaディレクトリを取得する
     */
    fun getJavaDir(): String = getStringNotNull(javaDir)

    /**
     * Javaディレクトリを設定する
     * @param dir Javaディレクトリ
     */
    fun setJavaDir(dir: String) { this.javaDir = dir }

    /**
     * @return Java引数を取得する
     */
    fun getJavaArgs(): String = getStringNotNull(javaArgs)

    /**
     * Java引数を設定する
     * @param args Java引数
     */
    fun setJavaArgs(args: String) { this.javaArgs = args }

    /**
     * @return レンダラーを取得する
     */
    fun getRenderer(): String = getStringNotNull(renderer)

    /**
     * レンダラーを設定する
     * @param renderer レンダラー
     */
    fun setRenderer(renderer: String) { this.renderer = renderer }

    /**
     * @return ドライバーを取得する
     */
    fun getDriver(): String = getStringNotNull(driver)

    /**
     * ドライバーを設定する
     * @param driver ドライバー
     */
    fun setDriver(driver: String) { this.driver = driver }

    /**
     * @return コントロールを取得する
     */
    fun getControl(): String = getStringNotNull(control)

    /**
     * コントロールを設定する
     * @param control コントロール
     */
    fun setControl(control: String) { this.control = control }

    /**
     * @return カスタムパスを取得する
     */
    fun getCustomPath(): String = getStringNotNull(customPath)

    /**
     * カスタムパスを設定する
     * @param customPath カスタムパス
     */
    fun setCustomPath(customPath: String) { this.customPath = customPath }

    /**
     * @return カスタム情報を取得する
     */
    fun getCustomInfo(): String = getStringNotNull(customInfo)

    /**
     * カスタム情報を設定する
     * @param customInfo カスタム情報
     */
    fun setCustomInfo(customInfo: String) { this.customInfo = customInfo }

    /**
     * @return ゲーム引数を取得する
     */
    fun getGameArgs(): String = getStringNotNull(gameArgs)

    /**
     * ゲーム引数を設定する
     * @param args ゲーム引数
     */
    fun setGameArgs(args: String) { this.gameArgs = args }

    /**
     * @return LWJGLバージョンを取得する
     */
    fun getLwjglVersion(): String = getStringNotNull(lwjglVersion)

    /**
     * LWJGLバージョンを設定する
     * @param version LWJGLバージョン
     */
    fun setLwjglVersion(version: String) { this.lwjglVersion = version }

    /**
     * 現在の設定と別の設定が異なるかどうかをチェックする
     * @param otherConfig 比較対象の設定
     * @return 異なる場合はtrue
     */
    fun checkDifferent(otherConfig: VersionConfig): Boolean {
        return !(this.getIsolationType() == otherConfig.getIsolationType() &&
                this.getJavaDir() == otherConfig.getJavaDir() &&
                this.getJavaArgs() == otherConfig.getJavaArgs() &&
                this.getRenderer() == otherConfig.getRenderer() &&
                this.getDriver() == otherConfig.getDriver() &&
                this.getControl() == otherConfig.getControl() &&
                this.getCustomPath() == otherConfig.getCustomPath() &&
                this.getCustomInfo() == otherConfig.getCustomInfo() &&
                this.getGameArgs() == otherConfig.getGameArgs() &&
                this.getLwjglVersion() == otherConfig.getLwjglVersion())
    }

    /**
     * 分離タイプがnullの場合はデフォルト値を返す
     * @param type 分離タイプ
     * @return nullでない分離タイプ
     */
    private fun getIsolationTypeNotNull(type: IsolationType?) = type ?: IsolationType.FOLLOW_GLOBAL

    /**
     * Parcelable: コンテンツの種類を記述する
     * @return 0（特別な種類はなし）
     */
    override fun describeContents(): Int = 0

    /**
     * Parcelable: オブジェクトをParcelに書き込む
     * @param dest 書き込み先のParcel
     * @param flags 追加のフラグ
     */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(versionPath.absolutePath)
        dest.writeInt(getIsolationTypeNotNull(isolationType).ordinal)
        dest.writeString(getStringNotNull(javaDir))
        dest.writeString(getStringNotNull(javaArgs))
        dest.writeString(getStringNotNull(renderer))
        dest.writeString(getStringNotNull(driver))
        dest.writeString(getStringNotNull(control))
        dest.writeString(getStringNotNull(customPath))
        dest.writeString(getStringNotNull(customInfo))
        dest.writeString(getStringNotNull(gameArgs))
        dest.writeString(getStringNotNull(lwjglVersion))
    }

    companion object CREATOR : Parcelable.Creator<VersionConfig> {
        /**
         * Parcelable: ParcelからVersionConfigを作成する
         * @param parcel 読み込み元のParcel
         * @return 作成されたVersionConfig
         */
        override fun createFromParcel(parcel: Parcel): VersionConfig {
            val versionPath = File(parcel.readString().orEmpty())
            val isolationType = IsolationType.entries.getOrNull(parcel.readInt()) ?: IsolationType.FOLLOW_GLOBAL
            val javaDir = parcel.readString().orEmpty()
            val javaArgs = parcel.readString().orEmpty()
            val renderer = parcel.readString().orEmpty()
            val driver = parcel.readString().orEmpty()
            val control = parcel.readString().orEmpty()
            val customPath = parcel.readString().orEmpty()
            val customInfo = parcel.readString().orEmpty()
            val gameArgs = parcel.readString().orEmpty()
            val lwjglVersion = parcel.readString().orEmpty()
            return VersionConfig(versionPath, isolationType, javaDir, javaArgs, renderer, driver, control, customPath, customInfo, gameArgs, lwjglVersion)
        }

        /**
         * Parcelable: 指定されたサイズのVersionConfig配列を作成する
         * @param size 配列のサイズ
         * @return 作成された配列
         */
        override fun newArray(size: Int): Array<VersionConfig?> {
            return arrayOfNulls(size)
        }

        /**
         * バージョンフォルダから設定を解析する
         * 旧バージョンの設定ファイルも自動的に認識して新形式で保存する
         * @param versionPath バージョンフォルダ
         * @return 解析されたVersionConfig
         */
        @JvmStatic
        fun parseConfig(versionPath: File): VersionConfig {
            // 旧バージョンのバージョン分離ファイルを認識して新形式で保存後、旧ファイルは削除
            val oldConfigFile = File(getYukariVersionPath(versionPath), "YukariVersion.cfg")
            val configFile = File(getYukariVersionPath(versionPath), "VersionConfig.json")

            return runCatching getConfig@{
                if (oldConfigFile.exists()) {
                    runCatching {
                        Tools.GLOBAL_GSON.fromJson(Tools.read(oldConfigFile), VersionConfig::class.java).apply {
                            setIsolationType(IsolationType.ENABLE)
                            setVersionPath(versionPath)
                            save()
                        }
                    }.getOrNull().let { config ->
                        // 古い設定ファイルを削除
                        oldConfigFile.delete()
                        config?.let { return@getConfig it }
                    }
                }
                // このファイルの内容を読み取り、VersionConfigとして解析
                val configString = Tools.read(configFile)
                val config = Tools.GLOBAL_GSON.fromJson(configString, VersionConfig::class.java)
                runCatching {
                    JsonParser.parseString(configString).asJsonObject.apply {
                        if (has("isolation")) {
                            config.setIsolationType(
                                if (get("isolation").asBoolean) IsolationType.ENABLE
                                else IsolationType.DISABLE
                            )
                        }
                    }
                }.onFailure { Logging.e("Refresh Versions", "Failed to parse the version isolation field of the old version.", it) }
                config.setVersionPath(versionPath)
                config
            }.getOrElse { e ->
                Logging.e("Refresh Versions", Tools.printToString(e))
                val config = VersionConfig(versionPath)
                config.save()
                config
            }
        }

        /**
         * バージョン分離を有効にして新しい設定を作成する
         * @param versionPath バージョンフォルダ
         * @return 作成されたVersionConfig
         */
        @JvmStatic
        fun createIsolation(versionPath: File): VersionConfig {
            val config = VersionConfig(versionPath)
            config.setIsolationType(IsolationType.ENABLE)
            return config
        }

        /**
         * 分離タイプの表示文字列を取得する
         * @param context コンテキスト
         * @param type 分離タイプ
         * @return 表示文字列
         */
        @JvmStatic
        fun getIsolationString(context: Context, type: IsolationType): String = when (type) {
            IsolationType.FOLLOW_GLOBAL -> context.getString(R.string.version_manager_isolation_type_follow_global)
            IsolationType.ENABLE -> context.getString(R.string.generic_open)
            IsolationType.DISABLE -> context.getString(R.string.generic_close)
        }
    }

    enum class IsolationType {
        FOLLOW_GLOBAL, ENABLE, DISABLE
    }
}
