package com.arata.yukarilauncher.value

import androidx.annotation.Keep
import com.google.gson.JsonSyntaxException
import com.arata.yukarilauncher.feature.accounts.AccountsManager
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.skin.SkinFileDownloader
import com.arata.yukarilauncher.utils.stringutils.StringUtilsKt
import com.arata.yukarilauncher.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID

@Keep
class MinecraftAccount {
    var accessToken: String = "0"
    var clientToken: String = "0"
    var profileId: String = "00000000-0000-0000-0000-000000000000"
    var username: String = "Steve"
    var msaRefreshToken: String = "0"
    var xuid: String? = null
    var otherBaseUrl: String? = null
    var otherAccount: String? = null
    var otherPassword: String? = null
    var accountType: String? = null
    private val uniqueUUID = UUID.randomUUID().toString().lowercase(Locale.ROOT)

    fun updateMicrosoftSkin() {
        updateSkin("https://sessionserver.mojang.com")
    }

    fun updateOtherSkin() {
        updateSkin(StringUtilsKt.removeSuffix(otherBaseUrl ?: "", "/") + "/sessionserver/")
    }

    private fun updateSkin(url: String) {
        val skinFile = File(PathManager.DIR_USER_SKIN, "$uniqueUUID.png")
        if (skinFile.exists()) FileUtils.deleteQuietly(skinFile)
        try {
            SkinFileDownloader().yggdrasil(url, skinFile, profileId)
            Logging.i("SkinLoader", "Update skin success")
        } catch (e: Exception) {
            Logging.i("SkinLoader", "Could not update skin\n" + Tools.printToString(e))
        }
    }

    fun save() {
        Tools.write(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID, Tools.GLOBAL_GSON.toJson(this))
    }

    companion object {
        fun parse(content: String): MinecraftAccount {
            return Tools.GLOBAL_GSON.fromJson(content, MinecraftAccount::class.java)
        }

        fun loadFromProfileID(profileID: String): MinecraftAccount? {
            for (account in AccountsManager.allAccounts) {
                if (account.profileId == profileID) return account
            }
            return null
        }

        fun loadFromUniqueUUID(uniqueUUID: String): MinecraftAccount? {
            if (!accountExists(uniqueUUID)) return null
            try {
                val acc = parse(Tools.read(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID))
                if (acc.accessToken == null) acc.accessToken = "0"
                if (acc.clientToken == null) acc.clientToken = "0"
                if (acc.profileId == null) acc.profileId = "00000000-0000-0000-0000-000000000000"
                if (acc.username == null) acc.username = "0"
                if (acc.msaRefreshToken == null) acc.msaRefreshToken = "0"
                return acc
            } catch (e: IOException) {
                Logging.e(MinecraftAccount::class.java.name, "Caught an exception while loading the profile", e)
                return null
            } catch (e: JsonSyntaxException) {
                Logging.e(MinecraftAccount::class.java.name, "Caught an exception while loading the profile", e)
                return null
            }
        }

        private fun accountExists(uniqueUUID: String): Boolean {
            return uniqueUUID.isNotEmpty() && File(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID).exists()
        }
    }

    fun getUniqueUUID(): String {
        return uniqueUUID
    }

    override fun toString(): String {
        return "MinecraftAccount{" +
                "username='" + username + '\'' +
                ", accountType=" + accountType +
                '}'
    }
}