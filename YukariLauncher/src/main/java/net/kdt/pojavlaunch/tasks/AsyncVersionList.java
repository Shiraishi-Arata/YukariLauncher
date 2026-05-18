package net.kdt.pojavlaunch.tasks;

import static net.kdt.pojavlaunch.utils.DownloadUtils.downloadString;

import androidx.annotation.Nullable;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.task.Task;
import com.arata.yukarilauncher.utils.path.PathManager;
import com.arata.yukarilauncher.utils.YLTools;
import com.arata.yukarilauncher.utils.path.UrlManager;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class AsyncVersionList {

    private static final long CACHE_TIME = 5 * 60 * 1000;

    public void getVersionList(@Nullable VersionDoneListener listener, boolean forceRefresh) {
        Task.runTask((java.util.concurrent.Callable<Void>) () -> {

            File versionFile = new File(PathManager.FILE_VERSION_LIST);
            JMinecraftVersionList versionList = null;

            try {

                boolean shouldDownload =
                        forceRefresh ||
                        !versionFile.exists() ||
                        (YLTools.getCurrentTimeMillis() > versionFile.lastModified() + CACHE_TIME);

                if (shouldDownload) {
                    versionList = downloadVersionList(UrlManager.URL_MINECRAFT_VERSION_REPOS);
                }

            } catch (Exception e) {
                Logging.e("AsyncVersionList", "Refreshing version list failed :" + e);
                Logging.e("GetVersionList", Tools.printToString(e));
            }

            if (versionList == null) {
                try {
                    versionList = Tools.GLOBAL_GSON.fromJson(
                            new JsonReader(new FileReader(versionFile)),
                            JMinecraftVersionList.class
                    );
                } catch (FileNotFoundException e) {
                    Logging.e("File Not Found", Tools.printToString(e));
                } catch (JsonIOException | JsonSyntaxException e) {
                    Logging.e("AsyncVersionList", Tools.printToString(e));
                    versionFile.delete();

                    if (!forceRefresh) {
                        getVersionList(listener, true);
                        return null;
                    }
                }
            }

            if (listener != null && versionList != null) {
                listener.onVersionDone(versionList);
            }

            return null;
        }).execute();
    }

    public void refresh(@Nullable VersionDoneListener listener) {
        getVersionList(listener, true);
    }

    public void clearCache() {
        File versionFile = new File(PathManager.FILE_VERSION_LIST);
        if (versionFile.exists()) versionFile.delete();
    }

    private JMinecraftVersionList downloadVersionList(String mirror) {
        JMinecraftVersionList list = null;

        try {
            Logging.i("ExtVL", "Syncing to external: " + mirror);

            String jsonString = downloadString(mirror);

            list = Tools.GLOBAL_GSON.fromJson(jsonString, JMinecraftVersionList.class);

            Logging.i("ExtVL", "Downloaded version list, len=" + list.versions.length);

            FileOutputStream fos = new FileOutputStream(PathManager.FILE_VERSION_LIST);
            fos.write(jsonString.getBytes());
            fos.close();

        } catch (IOException e) {
            Logging.e("AsyncVersionList", Tools.printToString(e));
        }

        return list;
    }

    public interface VersionDoneListener {
        void onVersionDone(JMinecraftVersionList versions);
    }
}