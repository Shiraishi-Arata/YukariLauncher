package com.arata.yukarilauncher.feature.download.platform.update

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object ModDownloader {
    private val client = OkHttpClient()

    /**
     * Download a mod into the given version's mods directory.
     */
    fun download(url: String, modName: String, versionDir: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return
            val bytes = response.body?.bytes() ?: return

            val modsDir = File(versionDir, "mods")
            if (!modsDir.exists()) modsDir.mkdirs()

            val outFile = File(modsDir, modName)
            FileOutputStream(outFile).use { fos -> fos.write(bytes) }
        }
    }
}
