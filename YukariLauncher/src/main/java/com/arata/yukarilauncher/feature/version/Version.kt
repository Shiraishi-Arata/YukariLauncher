package com.arata.yukarilauncher.feature.version

import android.os.Parcel
import android.os.Parcelable
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.feature.mod.parser.ModChecker
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.Tools
import java.io.File

/**
 * Minecraftのバージョン。バージョン名で区別される
 * @param versionsFolder バージョンが属するバージョンフォルダ
 * @param versionPath バージョンのパス
 * @param versionConfig 個別バージョンの設定
 * @param isValid バージョンの有効性
 */
class Version(
    private val versionsFolder: String,
    private val versionPath: String,
    private val versionConfig: VersionConfig,
    private val isValid: Boolean
) :Parcelable {
    /**
     * 現在のアカウントをオフラインアカウントとしてゲームを起動するかどうかを制御する
     */
    var offlineAccountLogin: Boolean = false

    /**
     * Modチェック結果
     */
    var modCheckResult: ModChecker.ModCheckResult? = null

    /**
     * @return バージョンが属するバージョンフォルダ
     */
    fun getVersionsFolder(): String = versionsFolder

    /**
     * @return バージョンフォルダ
     */
    fun getVersionPath(): File = File(versionPath)

    /**
     * @return バージョン名
     */
    fun getVersionName(): String = getVersionPath().name

    /**
     * @return バージョン分離設定
     */
    fun getVersionConfig() = versionConfig

    /**
     * @return バージョンの有効性：バージョンJSONファイルとバージョンフォルダの存在を確認
     */
    fun isValid() = isValid && getVersionPath().exists()

    /**
     * @return バージョン分離が有効かどうか
     */
    fun isIsolation() = versionConfig.isIsolation()

    /**
     * @return バージョンのゲームフォルダパス（バージョン分離が有効な場合はバージョンフォルダのパス）
     */
    fun getGameDir(): File {
        return if (versionConfig.isIsolation()) versionConfig.getVersionPath()
        // バージョン分離が無効な場合はカスタムパスを使用できる
        // カスタムパスが空の場合はデフォルトのゲームパス（.minecraft/）を返す
        else if (versionConfig.getCustomPath().isNotEmpty()) File(versionConfig.getCustomPath())
        else File(ProfilePathHome.getGameHome())
    }

    private fun String.getValueOrDefault(default: String): String = this.takeIf { it.isNotEmpty() } ?: default

    /**
     * レンダラー設定を取得する
     * @return レンダラー名
     */
    fun getRenderer(): String = versionConfig.getRenderer().getValueOrDefault(AllSettings.renderer.getValue())

    /**
     * ドライバー設定を取得する
     * @return ドライバー名
     */
    fun getDriver(): String = versionConfig.getDriver().getValueOrDefault(AllSettings.driver.getValue())

    /**
     * Javaディレクトリ設定を取得する
     * @return Javaのパス
     */
    fun getJavaDir(): String = versionConfig.getJavaDir().getValueOrDefault(AllSettings.defaultRuntime.getValue())

    /**
     * Java引数設定を取得する
     * @return Java引数文字列
     */
    fun getJavaArgs(): String = versionConfig.getJavaArgs().getValueOrDefault(AllSettings.javaArgs.getValue())

    /**
     * コントロール設定を取得する
     * @return コントロールマップのパス
     */
    fun getControl(): String {
        val configControl = versionConfig.getControl().removeSuffix("./")
        return if (configControl.isNotEmpty()) File(PathManager.DIR_CTRLMAP_PATH, configControl).absolutePath
        else File(AllSettings.defaultCtrl.getValue()).absolutePath
    }

    /**
     * カスタム情報を取得する
     * @return カスタム情報文字列
     */
    fun getCustomInfo(): String = versionConfig.getCustomInfo().getValueOrDefault(AllSettings.versionCustomInfo.getValue())
        .replace("[zl_version]", YLTools.getVersionName())

    /**
     * ゲーム引数を取得する
     * @return ゲーム引数文字列
     */
    fun getGameArgs(): String = versionConfig.getGameArgs()

    fun getLWJGLVersion(): String = versionConfig.getLwjglVersion().getValueOrDefault(AllSettings.lwjglVersion.getValue())

    /**
     * @return 保存されているバージョン情報
     */
    fun getVersionInfo(): VersionInfo? {
        return runCatching {
            val infoFile = File(VersionsManager.getYukariVersionPath(this), "VersionInfo.json")
            Tools.GLOBAL_GSON.fromJson(Tools.read(infoFile), VersionInfo::class.java)
        }.getOrElse { null }
    }

    /**
     * Boolean値をIntに変換する
     * @return trueの場合は1、falseの場合は0
     */
    private fun Boolean.getInt(): Int = if (this) 1 else 0

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
        dest.writeStringList(listOf(versionsFolder, versionPath))
        dest.writeParcelable(versionConfig, flags)
        dest.writeInt(isValid.getInt())
        dest.writeInt(offlineAccountLogin.getInt())
        dest.writeParcelable(modCheckResult, flags)
    }

    companion object CREATOR : Parcelable.Creator<Version> {
        /**
         * Int値をBooleanに変換する
         * @return 0以外の場合はtrue
         */
        private fun Int.toBoolean(): Boolean = this != 0

        /**
         * Parcelable: ParcelからVersionを作成する
         * @param parcel 読み込み元のParcel
         * @return 作成されたVersion
         */
        override fun createFromParcel(parcel: Parcel): Version {
            val stringList = ArrayList<String>()
            parcel.readStringList(stringList)
            val versionConfig = parcel.readParcelable<VersionConfig>(VersionConfig::class.java.classLoader)!!
            val isValid = parcel.readInt().toBoolean()
            val offlineAccount = parcel.readInt().toBoolean()
            val modCheckResult = parcel.readParcelable<ModChecker.ModCheckResult>(ModChecker.ModCheckResult::class.java.classLoader)

            return Version(stringList[0], stringList[1], versionConfig, isValid).apply {
                offlineAccountLogin = offlineAccount
                this.modCheckResult = modCheckResult
            }
        }

        /**
         * Parcelable: 指定されたサイズのVersion配列を作成する
         * @param size 配列のサイズ
         * @return 作成された配列
         */
        override fun newArray(size: Int): Array<Version?> {
            return arrayOfNulls(size)
        }
    }
}