package com.arata.yukarilauncher.feature.background

import android.content.Context
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.arata.yukarilauncher.InfoDistributor
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.file.FileTools.Companion.mkdirs
import com.arata.yukarilauncher.utils.image.ImageUtils.Companion.isImage
import jp.wasabeef.glide.transformations.BlurTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Properties

object BackgroundManager {
    private val FILE_BACKGROUND_PROPERTIES: File = File(PathManager.DIR_DATA, "background.properties")
    const val NULL: String = "null"
    private val videoExtensions = setOf("mp4", "webm", "mkv", "3gp")

    private val defaultProperties: Properties
        get() {
            val properties = Properties()
            properties.setProperty(BackgroundType.MAIN_MENU.name, NULL)
            properties.setProperty(BackgroundType.CUSTOM_CONTROLS.name, NULL)
            properties.setProperty(BackgroundType.IN_GAME.name, NULL)
            return properties
        }

    val properties: Properties
        get() {
            FILE_BACKGROUND_PROPERTIES.apply {
                if (!exists()) {
                    return@apply
                }

                val properties = Properties()
                runCatching {
                    FileReader(this).use { fileReader ->
                        properties.load(fileReader)
                    }
                }.getOrElse { e ->
                    Logging.e("BackgroundManager", Tools.printToString(e))
                    return@apply
                }

                return properties
            }

            return defaultProperties
        }

    @JvmStatic
    fun setBackgroundImage(
        context: Context,
        backgroundType: BackgroundType,
        backgroundView: ImageView,
        callback: CallbackDrawableImageViewTarget.Callback? = null
    ) {
        backgroundView.setImageDrawable(
            ContextCompat.getDrawable(context, R.color.background_app)
        )

        val backgroundImage = getBackgroundImage(backgroundType) ?: run {
            callback?.callback(false)
            return
        }

        val blurRadius = AllSettings.customBackgroundBlur.getValue().coerceIn(0, 25)
        val request = Glide.with(context).load(backgroundImage)
            .override(backgroundView.width, backgroundView.height)

        if (blurRadius == 0) {
            request.transform(CenterCrop())
        } else {
            request.transform(CenterCrop(), BlurTransformation(blurRadius, 1))
        }

        request.into(CallbackDrawableImageViewTarget(backgroundView, callback))
    }

    @JvmStatic
    fun clearBackgroundImage(
        backgroundView: ImageView
    ) {
        backgroundView.background = null
        backgroundView.setImageDrawable(null)
    }

    @JvmStatic
    fun hasBackgroundImage(backgroundType: BackgroundType): Boolean {
        val pngName = properties[backgroundType.name] as String?
        return pngName != null && pngName != NULL
    }

    @JvmStatic
    fun getBackgroundImage(backgroundType: BackgroundType): File? {
        if (!hasBackgroundImage(backgroundType)) return null

        val pngName = properties[backgroundType.name] as String

        val backgroundImage = File(PathManager.DIR_BACKGROUND, pngName)
        if (!backgroundImage.exists()) return null
        if (!isImage(backgroundImage) && !isVideo(backgroundImage)) return null
        return backgroundImage
    }

    @JvmStatic
    fun isVideo(file: File): Boolean {
        return file.extension.lowercase() in videoExtensions
    }

    private fun saveProperties(properties: Properties) {
        PathManager.DIR_BACKGROUND.apply {
            if (!exists()) mkdirs(this)
        }

        runCatching {
            properties.store(
                FileWriter(FILE_BACKGROUND_PROPERTIES),
                "${InfoDistributor.APP_NAME} Background Properties File"
            )
        }.getOrElse { e -> Logging.e("saveProperties", Tools.printToString(e)) }
    }

    fun saveProperties(map: Map<BackgroundType, String>) {
        val properties = Properties()
        properties.setProperty(
            BackgroundType.MAIN_MENU.name,
            map[BackgroundType.MAIN_MENU] ?: NULL
        )
        properties.setProperty(
            BackgroundType.CUSTOM_CONTROLS.name,
            map[BackgroundType.CUSTOM_CONTROLS] ?: NULL
        )
        properties.setProperty(
            BackgroundType.IN_GAME.name,
            map[BackgroundType.IN_GAME] ?: NULL
        )

        saveProperties(properties)
    }
}
