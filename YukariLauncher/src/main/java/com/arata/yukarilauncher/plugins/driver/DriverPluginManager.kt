package com.arata.yukarilauncher.plugins.driver

import android.content.Context
import android.content.pm.ApplicationInfo
import com.arata.yukarilauncher.setting.AllSettings

/**
 * FCL ドライバープラグイン管理
 * [FCL DriverPlugin.kt](https://github.com/FCL-Team/FoldCraftLauncher/blob/main/FCLauncher/src/main/java/com/tungsten/fclauncher/plugins/DriverPlugin.kt)
 */
object DriverPluginManager {
    private val driverList: MutableList<Driver> = mutableListOf()

    /**
     * ドライバー名の一覧を取得する
     */
    @JvmStatic
    fun getDriverNameList(): List<String> = driverList.map { it.driver }

    private lateinit var currentDriver: Driver

    /**
     * 名前からドライバーを設定する
     * @param driverName ドライバー名
     */
    @JvmStatic
    fun setDriverByName(driverName: String) {
        currentDriver = driverList.find { it.driver == driverName } ?: driverList[0]
    }

    /**
     * 現在のドライバーを取得する
     */
    @JvmStatic
    fun getDriver(): Driver = currentDriver

    /**
     * ドライバーを初期化する
     * @param reset 既存のプラグインをクリアするかどうか
     */
    fun initDriver(context: Context, reset: Boolean) {
        if (reset) driverList.clear()
        driverList.add(Driver("Turnip", context.applicationInfo.nativeLibraryDir))
        setDriverByName(AllSettings.driver.getValue())
    }

    /**
     * FCLドライバープラグインを解析する
     */
    fun parsePlugin(info: ApplicationInfo) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (metaData.getBoolean("fclPlugin", false)) {
                val driver = metaData.getString("driver") ?: return
                val nativeLibraryDir = info.nativeLibraryDir
                driverList.add(
                    Driver(
                        driver,
                        nativeLibraryDir
                    )
                )
            }
        }
    }
}