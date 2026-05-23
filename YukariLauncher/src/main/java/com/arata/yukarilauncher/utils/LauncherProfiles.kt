package com.arata.yukarilauncher.utils

import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.feature.log.Logging
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

class LauncherProfiles {
    companion object {
        /**
         * デフォルトの launcher_profiles.json ファイルを生成する
         * このファイルが存在しない場合、ForgeやNeoForgeなどが正常にインストールできない
         */
        @JvmStatic
        fun generateLauncherProfiles() {
            runCatching {
                File(ProfilePathHome.getGameHome(), "launcher_profiles.json").apply {
                    if (!exists()) {
                        if (parentFile?.exists() == false) parentFile?.mkdirs()
                        if (!createNewFile()) throw IOException("Failed to create launcher_profiles.json file!")
                        // ファイル内容を書き込む
                        val profilesJsonString = """{"profiles":{"default":{"lastVersionId":"latest-release"}},"selectedProfile":"default"}""".trimIndent()
                        FileUtils.write(this, profilesJsonString)
                        Logging.i(
                            "Write launcher_profiles.json",
                            "The content has already been written! \r\nFile Location: $absolutePath\r\nContents: $profilesJsonString"
                        )
                    }
                }
            }.getOrElse { e ->
                Logging.e("Write launcher_profiles.json", "Unable to generate launcher_profiles.json file!", e)
            }
        }
    }
}
