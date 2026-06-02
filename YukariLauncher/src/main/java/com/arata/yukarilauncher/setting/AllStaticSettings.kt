package com.arata.yukarilauncher.setting

/**
 * 静的設定項目の値。一時的に有効な設定項目に使用する
 * ここでの値は設定構成に保存されず、ソフトウェアの再起動で消去される
 */
class AllStaticSettings {
    companion object {
        /**
         * ノッチ（画面の切り欠き）の幅
         */
        @JvmField var notchSize = 0

        /**
         * スケール係数
         */
        @JvmField var scaleFactor = AllSettings.resolutionRatio.getValue() / 100f

        /**
         * ダブルタップによるアイテム入れ替えの無効化
         */
        @JvmField var disableDoubleTap = AllSettings.disableDoubleTap.getValue()
        @JvmField var forceGuiInput = AllSettings.forceGuiInput.getValue()

        /**
         * 長押しトリガーの遅延時間（ミリ秒）
         */
        @JvmField var timeLongPressTrigger = AllSettings.timeLongPressTrigger.getValue()

        /**
         * ジャイロスコープ制御の有効/無効
         */
        @JvmField var enableGyro = AllSettings.enableGyro.getValue()

        /**
         * ジャイロスコープの感度
         */
        @JvmField var gyroSensitivity = AllSettings.gyroSensitivity.getValue()

        /**
         * ジャイロスコープX軸の反転
         */
        @JvmField var gyroInvertX = AllSettings.gyroInvertX.getValue()

        /**
         * ジャイロスコープY軸の反転
         */
        @JvmField var gyroInvertY = AllSettings.gyroInvertY.getValue()

        /**
         * コントロールプロキシの使用
         */
        @JvmField var useControllerProxy = false
    }
}