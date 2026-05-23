package net.kdt.pojavlaunch.prefs;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.P;
import static net.kdt.pojavlaunch.Architecture.is32BitsDevice;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;

import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.feature.unpack.Jre;
import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.setting.AllStaticSettings;
import com.arata.yukarilauncher.setting.Settings;
import com.arata.yukarilauncher.ui.activity.BaseActivity;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.utils.JREUtils;

/**
 * ランチャーの設定を読み込み、管理するクラス。
 */
public class LauncherPreferences {

    /**
     * 設定を読み込み、Java引数から不要なLWJGL引数を除去し、ランタイムを再読み込みします。
     */
    public static void loadPreferences() {
        String argLwjglLibname = "-Dorg.lwjgl.opengl.libname=";
        String javaArgs = AllSettings.getJavaArgs().getValue();
        for (String arg : JREUtils.parseJavaArguments(javaArgs)) {
            if (arg.startsWith(argLwjglLibname)) {
                AllSettings.getJavaArgs().put(javaArgs.replace(arg, "")).save();
            }
        }
        reloadRuntime();
    }

    /**
     * デフォルトのランタイム設定が存在しない場合に設定します。
     */
    public static void reloadRuntime() {
        if (!Settings.Manager.contains("defaultRuntime") && !MultiRTUtils.getRuntimes().isEmpty()) {
            AllSettings.getDefaultRuntime().put(Jre.JRE_8.getJreName()).save();
        }
    }

    /**
     * デバイスの物理RAM量に基づいて最適なデフォルトRAM割り当て量を見つけます。
     * 少なすぎるとMinecraftがラグやクラッシュを起こし、多すぎるとGCが遅延しAndroidの動作に支障をきたします。
     * @param ctx デバイスの総メモリを取得するために必要なコンテキスト
     * @return 最適なデフォルトRAM値（MB）
     */
    public static int findBestRAMAllocation(Context ctx){
        int deviceRam = Tools.getTotalDeviceMemory(ctx);
        if (deviceRam < 1024) return 296;
        if (deviceRam < 1536) return 448;
        if (deviceRam < 2048) return 656;
        if (is32BitsDevice()) return 696;
        if (deviceRam < 3064) return 936;
        if (deviceRam < 4096) return 1144;
        if (deviceRam < 6144) return 1536;
        return 2048;
    }

    /**
     * ノッチサイズを計算し、画面の境界を超えないようにします。
     */
    public static void computeNotchSize(BaseActivity activity) {
        if (Build.VERSION.SDK_INT < P) return;
        try {
            final Rect cutout;
            if(SDK_INT >= Build.VERSION_CODES.S){
                cutout = activity.getWindowManager().getCurrentWindowMetrics().getWindowInsets().getDisplayCutout().getBoundingRects().get(0);
            } else {
                cutout = activity.getWindow().getDecorView().getRootWindowInsets().getDisplayCutout().getBoundingRects().get(0);
            }
            int orientation = activity.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) AllStaticSettings.notchSize = cutout.height();
            else if (orientation == Configuration.ORIENTATION_LANDSCAPE) AllStaticSettings.notchSize = cutout.width();
            else AllStaticSettings.notchSize = Math.min(cutout.width(), cutout.height());
        }catch (Exception e){
            Logging.i("NOTCH DETECTION", "No notch detected, or the device if in split screen mode");
            AllStaticSettings.notchSize = -1;
        }
        Tools.updateWindowSize(activity);
    }
}
