package com.arata.yukarilauncher.feature.mod.modloader;

import androidx.annotation.NonNull;

import com.kdt.mcgui.ProgressLayout;
import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome;
import com.arata.yukarilauncher.feature.version.install.InstallTask;
import com.arata.yukarilauncher.utils.path.PathManager;

import com.arata.yukarilauncher.Tools;
import com.arata.yukarilauncher.task.ProgressKeeper;
import com.arata.yukarilauncher.utils.http.DownloadUtils;
import com.arata.yukarilauncher.utils.file.FileUtils;

import java.io.File;

public class FabricLikeDownloadTask implements InstallTask, Tools.DownloaderFeedback {
    private final FabricLikeUtils mUtils;
    private String mGameVersion = null;
    private String mLoaderVersion = null;

/**
 * FabricLikeDownloadTaskする
 */
    public FabricLikeDownloadTask(FabricLikeUtils utils) {
        this.mUtils = utils;
    }

/**
 * FabricLikeDownloadTaskする
 */
    public FabricLikeDownloadTask(FabricLikeUtils utils, String gameVersion, String loaderVersion) {
        this(utils);
        this.mGameVersion = gameVersion;
        this.mLoaderVersion = loaderVersion;
    }

    /**
     * インストールタスクを実行する
     * @param customName カスタムインスタンス名
     * @return インストーラーファイル、またはnull
     * @throws Exception 実行エラー時
     */
    @Override
    public File run(@NonNull String customName) throws Exception {
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.mod_download_progress, mUtils.getName());
        File outputFile;
        if (mGameVersion == null && mLoaderVersion == null) {
            outputFile = downloadInstaller();
        }
        else {
            legacyInstall(customName);
            outputFile = null;
        }
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE);
        return outputFile;
    }

/**
 * Fabricインストーラーをダウンロードする
 * @return ダウンロードファイル
 */
    private File downloadInstaller() throws Exception {
        File outputFile = new File(PathManager.DIR_CACHE, "fabric-installer.jar");

        String installerDownloadUrl = mUtils.getInstallerDownloadUrl();
        byte[] buffer = new byte[8192];
        DownloadUtils.downloadFileMonitored(installerDownloadUrl, outputFile, buffer, this);

        return outputFile;
    }

    /**
     * 従来の方式でFabricインストールを実行する
     * バージョンJSONをダウンロードして保存する
     * @param customName カスタムインスタンス名
     * @throws Exception インストールエラー時
     */
    private void legacyInstall(String customName) throws Exception {
        String jsonString = DownloadUtils.downloadString(mUtils.createJsonDownloadUrl(mGameVersion, mLoaderVersion));

        File versionJsonDir = new File(ProfilePathHome.getVersionsHome(), customName);
        File versionJsonFile = new File(versionJsonDir, customName + ".json");
        FileUtils.ensureDirectory(versionJsonDir);
        Tools.write(versionJsonFile.getAbsolutePath(), jsonString);
    }

    /**
     * ダウンロード進捗を更新する
     * @param curr 現在の進捗
     * @param max 最大進捗
     */
    @Override
    public void updateProgress(long curr, long max) {
        int progress100 = (int)(((float)curr / (float)max)*100f);
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, progress100, R.string.mod_download_progress, mUtils.getName());
    }
}
