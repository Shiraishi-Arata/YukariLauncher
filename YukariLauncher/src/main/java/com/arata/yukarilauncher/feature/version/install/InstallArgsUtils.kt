package com.arata.yukarilauncher.feature.version.install

import android.content.Intent
import com.google.gson.JsonParser
import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.utils.path.LibPath
import net.kdt.pojavlaunch.JavaGUILauncherActivity
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * ModLoaderインストーラーの起動引数を管理するユーティリティクラス
 * @param mcVersion Minecraftのバージョン
 * @param loaderVersion ModLoaderのバージョン
 */
class InstallArgsUtils(private val mcVersion: String, private val loaderVersion: String) {
    /**
     * Fabricインストーラーの起動引数をIntentに設定する
     * @param intent 対象のIntent
     * @param jarFile FabricインストーラーのJARファイル
     * @param customName カスタムバージョン名
     */
    fun setFabric(intent: Intent, jarFile: File, customName: String) {
        val args = "-DprofileName=\"$customName\" -javaagent:${LibPath.MIO_FABRIC_AGENT.absolutePath}" +
                " -jar ${jarFile.absolutePath} client -mcversion \"$mcVersion\" -loader \"$loaderVersion\" -dir \"${ProfilePathHome.getGameHome()}\""
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
    }

    /**
     * Quiltインストーラーの起動引数をIntentに設定する（非推奨）
     * JRE 8でのインストールはサポートされていない。
     * より新しいJRE環境では自動終了しないため、この関数は一時的に使用しない
     * @param intent 対象のIntent
     * @param jarFile QuiltインストーラーのJARファイル
     */
    @Deprecated("JRE 8でのインストールはサポート外。より高いJRE環境では自動終了しないため、この関数は一時的に使用しない")
    fun setQuilt(intent: Intent, jarFile: File) {
        val args = "-jar ${jarFile.absolutePath} install client \"$mcVersion\" \"$loaderVersion\" --install-dir=\"${ProfilePathHome.getGameHome()}\""
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
    }

    /**
     * Forgeインストーラーの起動引数をIntentに設定する
     * @param intent 対象のIntent
     * @param jarFile ForgeインストーラーのJARファイル
     * @param customName カスタムバージョン名
     */
    @Throws(Throwable::class)
    fun setForge(intent: Intent, jarFile: File, customName: String) {
        forgeLikeCustomVersionName(jarFile, customName)

        val args = "-javaagent:${LibPath.FORGE_INSTALLER.absolutePath}=\"$loaderVersion\" -jar ${jarFile.absolutePath}"
        intent.putExtra("javaArgs", args)
    }

    /**
     * NeoForgeインストーラーの起動引数をIntentに設定する
     * @param intent 対象のIntent
     * @param jarFile NeoForgeインストーラーのJARファイル
     * @param customName カスタムバージョン名
     */
    @Throws(Throwable::class)
    fun setNeoForge(intent: Intent, jarFile: File, customName: String) {
        forgeLikeCustomVersionName(jarFile, customName)

        val args = "-jar ${jarFile.absolutePath} --installClient \"${ProfilePathHome.getGameHome()}\""
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
        // NeoForgeインストーラーのセキュリティマネージャーを無効化するフラグを追加
        intent.putExtra("disableSecurityManager", true)
    }

    /**
     * OptiFineインストーラーの起動引数をIntentに設定する
     * @param intent 対象のIntent
     * @param jarFile OptiFineインストーラーのJARファイル
     * @param customName カスタムバージョン名
     */
    fun setOptiFine(intent: Intent, jarFile: File, customName: String) {
        val args = "-javaagent:${LibPath.FORGE_INSTALLER.absolutePath}=OFNPS " +
                "-javaagent:${LibPath.OPTIFINE_RENAMER.absolutePath}=\"$customName\" " +
                "-jar ${jarFile.absolutePath}"
        intent.putExtra("javaArgs", args)
    }

    /**
     * Forge/NeoForgeインストーラー内のinstall_profile.jsonのversionキーをcustomNameに書き換える
     * Forgeインストーラーはversionの値を使用してバージョンフォルダを生成する
     * これにより、カスタムバージョンjsonのインストール位置を制御できる
     * @param jarFile インストーラーのJARファイル
     * @param customName カスタムバージョン名
     */
    @Throws(Throwable::class)
    private fun forgeLikeCustomVersionName(jarFile: File, customName: String) {
        val tempJarFile = File(jarFile.parentFile, "${jarFile.nameWithoutExtension}_temp.jar")
        val profileJson = File(jarFile.parentFile, "install_profile.json")
        try {
            updateProgress(0)

            if (tempJarFile.exists()) tempJarFile.delete()
            extractInstallProfile(jarFile, profileJson)
            updateProgress(50)

            modifyJsonFile(profileJson, customName)
            writeTempJarFile(jarFile, tempJarFile, profileJson)
            updateProgress(100)

            if (!jarFile.delete()) throw IOException("Failed to delete original Installer file!")
            if (!tempJarFile.renameTo(jarFile)) throw IOException("Failed to rename temp Installer file to original!")
            profileJson.delete()
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
        }
    }

    /**
     * 進捗状況を更新する
     * @param progress 進捗値（0〜100）
     */
    private fun updateProgress(progress: Int) {
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, progress, R.string.mod_forge_custom_version)
    }

    /**
     * JARファイルからinstall_profile.jsonを抽出する
     * @param jarFile インストーラーJARファイル
     * @param profileJson 出力先のJSONファイル
     */
    @Throws(Throwable::class)
    private fun extractInstallProfile(jarFile: File, profileJson: File) {
        val zipFile = ZipFile(jarFile)
        val entry = zipFile.getEntry("install_profile.json")
            ?: throw IOException("File \"install_profile.json\" not found in the Installer")
        profileJson.outputStream().use { outputStream ->
            zipFile.getInputStream(entry).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    /**
     * install_profile.jsonファイル内の値を変更して、カスタムバージョン名を適用する
     * @param profileJson プロファイルJSONファイル
     * @param customName カスタムバージョン名
     */
    @Throws(Throwable::class)
    private fun modifyJsonFile(profileJson: File, customName: String) {
        val jsonObject = JsonParser.parseString(profileJson.readText()).asJsonObject
        // specキーの有無で新/旧インストーラーを判別
        if (jsonObject.has("spec")) { // 新バージョンインストーラー
            if (!jsonObject.has("version")) throw IOException("Unable to find version key!")
            // install_profile.jsonのversion値をcustomNameに変更することで、カスタムバージョン名を実現
            jsonObject.addProperty("version", customName)
        } else { // 旧バージョンインストーラー
            if (!jsonObject.has("install")) throw IOException("Unable to find install key!")
            val install = jsonObject.get("install").asJsonObject
            if (!install.has("target")) throw IOException("Unable to find install-target key!")
            // target値をcustomNameに変更することで、旧バージョンのカスタムバージョン名を実現
            install.addProperty("target", customName)
            jsonObject.add("install", install)
        }
        profileJson.writeText(jsonObject.toString())
    }

    /**
     * 変更を加えた一時JARファイルを書き出す
     * META-INF内の.SF/.RSAファイルをスキップして、署名検証の問題を回避する
     * @param jarFile 元のJARファイル
     * @param tempJarFile 出力先の一時JARファイル
     * @param profileJson 変更済みのinstall_profile.jsonファイル
     */
    @Throws(Throwable::class)
    private fun writeTempJarFile(jarFile: File, tempJarFile: File, profileJson: File) {
        // META-INF内の.SFまたは.RSAファイルをスキップし、install_profile.jsonが変更されても検証に失敗しないようにする
        fun needSkip(entryName: String) = entryName.startsWith("META-INF/") && (entryName.endsWith(".SF") || entryName.endsWith(".RSA"))

        ZipFile(jarFile).use { zipFile ->
            ZipOutputStream(tempJarFile.outputStream()).use { zos ->
                zipFile.entries().asSequence().forEach { originalEntry ->
                    zos.putNextEntry(ZipEntry(originalEntry.name))
                    if (originalEntry.name == "install_profile.json") {
                        profileJson.inputStream().use { fis -> fis.copyTo(zos) }
                    } else {
                        if (!originalEntry.isDirectory && !needSkip(originalEntry.name)) {
                            // 元のファイルを書き込む
                            zipFile.getInputStream(originalEntry).use { it.copyTo(zos) }
                        }
                    }
                    zos.closeEntry()
                }
            }
        }
    }
}
