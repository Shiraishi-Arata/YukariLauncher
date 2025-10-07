package com.arata.yukarilauncher.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.arata.yukarilauncher.context.ContextExecutor;
import com.arata.yukarilauncher.context.LocaleHelper;
import com.arata.yukarilauncher.event.single.LauncherIgnoreNotchEvent;
import com.arata.yukarilauncher.feature.accounts.AccountsManager;
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathManager;
import com.arata.yukarilauncher.plugins.PluginLoader;
import com.arata.yukarilauncher.renderer.Renderers;
import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.utils.StoragePermissionsUtils;

import net.kdt.pojavlaunch.MissingStorageActivity;
import net.kdt.pojavlaunch.Tools;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.Companion.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleHelper.Companion.setLocale(this);
        Tools.setFullscreen(this);
        Tools.updateWindowSize(this);

        checkStoragePermissions();
        //加载渲染器
        Renderers.INSTANCE.init(false);
        //加载插件
        PluginLoader.loadAllPlugins(this, false);
        //刷新游戏路径
        ProfilePathManager.INSTANCE.refreshPath();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        if (!Tools.checkStorageRoot()) {
            startActivity(new Intent(this, MissingStorageActivity.class));
            finish();
        }

        checkStoragePermissions();

        AccountsManager.INSTANCE.reload();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Tools.setFullscreen(this);
        Tools.ignoreNotch(shouldIgnoreNotch(),this);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Tools.getDisplayMetrics(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void event(LauncherIgnoreNotchEvent event) {
        Tools.ignoreNotch(shouldIgnoreNotch(),this);
    }

    /** @return Whether or not the notch should be ignored */
    public boolean shouldIgnoreNotch() {
        return AllSettings.getIgnoreNotchLauncher().getValue();
    }

    private void checkStoragePermissions() {
        //检查所有文件管理权限
        StoragePermissionsUtils.checkPermissions(this);
    }
}
