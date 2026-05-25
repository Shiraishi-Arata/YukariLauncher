package com.arata.yukarilauncher.support.touch_controller;

import android.content.Context;
import android.os.Vibrator;
import android.system.Os;

import com.arata.yukarilauncher.InfoDistributor;
import com.arata.yukarilauncher.feature.log.Logging;

import com.arata.yukarilauncher.feature.log.Logger;

import top.fifthlight.touchcontroller.proxy.client.LauncherProxyClient;
import top.fifthlight.touchcontroller.proxy.client.MessageTransport;
import top.fifthlight.touchcontroller.proxy.client.android.transport.UnixSocketTransportKt;

/**
 * TouchController Modとの連携を担当する
 * <a href="">https://modrinth.com/mod/touchcontroller</a>
 */
public final class ControllerProxy {
    private static LauncherProxyClient proxyClient;

    private ControllerProxy() {}

    /**
     * 制御プロキシクライアントを起動する
     * TouchController Modとの通信を確立する
     * @param context アプリケーションコンテキスト
     */
    public static void startProxy(Context context) {
        if (proxyClient == null) {
            try {
                MessageTransport transport = UnixSocketTransportKt.UnixSocketTransport(InfoDistributor.LAUNCHER_NAME);
                Os.setenv("TOUCH_CONTROLLER_PROXY_SOCKET", InfoDistributor.LAUNCHER_NAME, true);
                LauncherProxyClient client = new LauncherProxyClient(transport);
                Vibrator vibrator = context.getSystemService(Vibrator.class);
                VibrationHandler handler = new VibrationHandler(vibrator);
                client.setVibrationHandler(handler);
                client.run();
                Logging.i("TouchController", "TouchController Proxy Client has been created!");
                //Logger.appendToLog("TouchController: TouchController Proxy Client has been created!");
                proxyClient = client;
            } catch (Throwable ex) {
                Logging.w("TouchController", "TouchController proxy client create failed", ex);
                proxyClient = null;
            }
        }
    }

    /**
     * プロキシクライアントのインスタンスを取得する
     * @return プロキシクライアント（未初期化の場合はnull）
     */
    static LauncherProxyClient getProxyClient() {
        return proxyClient;
    }
}
