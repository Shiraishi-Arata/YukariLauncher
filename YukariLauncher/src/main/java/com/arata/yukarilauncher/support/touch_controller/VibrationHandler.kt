package com.arata.yukarilauncher.support.touch_controller

import android.os.VibrationEffect
import android.os.Vibrator
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.setting.AllSettings
import top.fifthlight.touchcontroller.proxy.client.LauncherProxyClient
import top.fifthlight.touchcontroller.proxy.message.VibrateMessage

/**
 * バイブレーション処理ハンドラー
 * TouchControllerのバイブレーション要求を処理する
 */
class VibrationHandler(private val vibrator: Vibrator) : LauncherProxyClient.VibrationHandler {
    /**
     * 指定された種類のバイブレーションを実行する
     * 設定された持続時間でバイブレーション効果を生成する
     * @param kind バイブレーションの種類
     */
    override fun viberate(kind: VibrateMessage.Kind) {
        runCatching {
            val effect = VibrationEffect.createOneShot(
                AllSettings.tcVibrateDuration.getValue().coerceAtMost(500).coerceAtLeast(80).toLong(),
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibrator.vibrate(effect)
        }.getOrElse {
            Logging.e("TouchController_VibrationHandler", "Failed to attempt vibrating the device!", it)
        }
    }
}