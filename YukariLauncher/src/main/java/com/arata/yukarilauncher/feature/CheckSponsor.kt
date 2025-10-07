package com.arata.yukarilauncher.feature

import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.ui.subassembly.about.SponsorMeta
import com.arata.yukarilauncher.utils.http.CallUtils
import com.arata.yukarilauncher.utils.http.CallUtils.CallbackListener
import com.arata.yukarilauncher.utils.path.UrlManager
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import net.kdt.pojavlaunch.Tools
import okhttp3.Call
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Objects

class CheckSponsor {
    companion object {
        private var sponsorMeta: SponsorMeta? = null
        private var isChecking = false

        @JvmStatic
        fun getSponsorData(): SponsorMeta? {
            return sponsorMeta
        }

        @JvmStatic
        fun check(listener: CheckListener) {
            if (isChecking) {
                listener.onFailure()
                return
            }
            isChecking = true

            sponsorMeta?.let {
                listener.onSuccessful(sponsorMeta)
                isChecking = false
                return
            }

            CallUtils(object : CallbackListener {
                override fun onFailure(call: Call?) {
                    listener.onFailure()
                    isChecking = false
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call?, response: Response?) {
                    if (!response!!.isSuccessful) {
                        Logging.e("CheckSponsor", "Unexpected code ${response.code}")
                    } else {
                        runCatching {
                            Objects.requireNonNull(response.body)
                            val responseBody = response.body!!.string()

                            val originJson = JSONObject(responseBody)
                            val rawBase64 = originJson.getString("content")
                            //base64解码，因为这里读取的是一个经过Base64加密后的文本
                            val rawJson = StringUtils.decodeBase64(rawBase64)

                            sponsorMeta = Tools.GLOBAL_GSON.fromJson(rawJson, SponsorMeta::class.java).takeIf { it.sponsors.isNotEmpty() } ?: run {
                                listener.onFailure()
                                return
                            }
                            listener.onSuccessful(sponsorMeta)
                        }.getOrElse { e ->
                            Logging.e("Load Sponsor Data", "Failed to resolve sponsor list.", e)
                            listener.onFailure()
                        }
                    }
                    isChecking = false
                }
            }, "${UrlManager.URL_GITHUB_HOME}launcher_sponsor.json", null).enqueue()
        }
    }

    interface CheckListener {
        fun onFailure()

        fun onSuccessful(data: SponsorMeta?)
    }
}
