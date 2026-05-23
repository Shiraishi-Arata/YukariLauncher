package net.kdt.pojavlaunch.mirrors;

import androidx.annotation.Nullable;

import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.setting.AllSettings;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Objects;

/**
 * ダウンロードミラーリング機能を提供するクラス。
 * BMCLAPIなどのミラーを使用してファイルをダウンロードし、失敗した場合は公式ソースにフォールバックします。
 */
public class DownloadMirror {
    /** ライブラリダウンロードクラス */
    public static final int DOWNLOAD_CLASS_LIBRARIES = 0;
    /** メタデータダウンロードクラス */
    public static final int DOWNLOAD_CLASS_METADATA = 1;
    /** アセットダウンロードクラス */
    public static final int DOWNLOAD_CLASS_ASSETS = 2;

    private static final String URL_PROTOCOL_TAIL = "://";
    private static final String[] MIRROR_BMCLAPI = {
            "https://bmclapi2.bangbang93.com/maven",
            "https://bmclapi2.bangbang93.com",
            "https://bmclapi2.bangbang93.com/assets"
    };

    /**
     * 現在のミラーを使用してファイルをダウンロードします。ミラーにファイルがない場合は公式ソースにフォールバックします。
     * @param downloadClass ダウンロードクラス（DOWNLOAD_CLASS_LIBRARIES / DOWNLOAD_CLASS_METADATA / DOWNLOAD_CLASS_ASSETS）
     * @param urlInput 元の（Mojang）URL
     * @param outputFile 出力ファイル
     * @param buffer 共有バッファ
     * @param monitor ダウンロードモニター
     */
    public static void downloadFileMirrored(int downloadClass, String urlInput, File outputFile,
                                            @Nullable byte[] buffer, Tools.DownloaderFeedback monitor) throws IOException {
        try {
            DownloadUtils.downloadFileMonitored(getMirrorMapping(downloadClass, urlInput),
                    outputFile, buffer, monitor);
            return;
        }catch (Exception e) {
            Logging.w("DownloadMirror", "Cannot find the file on the mirror", e);
            Logging.i("DownloadMirror", "Falling back to default source");
        }
        DownloadUtils.downloadFileMonitored(urlInput, outputFile, buffer, monitor);
    }

    /**
     * ミラーを使用してファイルをダウンロードします（モニターなし）。
     */
    public static void downloadFileMirrored(int downloadClass, String urlInput, File outputFile) throws IOException {
        try {
            DownloadUtils.downloadFile(getMirrorMapping(downloadClass, urlInput), outputFile);
            return;
        }catch (Exception e) {
            Logging.w("DownloadMirror", "Cannot find the file on the mirror", e);
            Logging.i("DownloadMirror", "Falling back to default source");
        }
        DownloadUtils.downloadFile(urlInput, outputFile);
    }

    /**
     * ミラー上のファイルのコンテンツ長を取得します。利用できない場合は公式ソースから取得します。
     * @return ファイルの長さ（バイト）。利用できない場合は-1。
     */
    public static long getContentLengthMirrored(int downloadClass, String urlInput) throws IOException {
        long length = DownloadUtils.getContentLength(getMirrorMapping(downloadClass, urlInput));
        if(length < 1) {
            Logging.w("DownloadMirror", "Unable to get content length from mirror");
            Logging.i("DownloadMirror", "Falling back to default source");
            return DownloadUtils.getContentLength(urlInput);
        }else {
            return length;
        }
    }

    /**
     * ミラーからファイルを文字列としてダウンロードします。存在しない場合や無効な場合は公式ソースを使用します。
     */
    public static String downloadStringMirrored(int downloadClass, String urlInput) throws IOException{
        String resultString = null;
        try {
            resultString = DownloadUtils.downloadString(getMirrorMapping(downloadClass,urlInput));
        }catch (FileNotFoundException e) {
            Logging.w("DownloadMirror", "Failed to download string from mirror", e);
        }
        if(Tools.isValidString(resultString)) {
            return resultString;
        }else {
            Logging.w("DownloadMirror", "Downloaded string is invalid, falling back to default");
        }
        return DownloadUtils.downloadString(urlInput);
    }

    /**
     * @return 現在のダウンロードソースがミラーである場合はtrue
     */
    public static boolean isMirrored() {
        return !Objects.equals(AllSettings.getDownloadSource().getValue(), "default");
    }

    /**
     * 現在のミラー設定を取得します。
     */
    private static String[] getMirrorSettings() {
        switch (Objects.requireNonNull(AllSettings.getDownloadSource().getValue())) {
            case "bmclapi": return MIRROR_BMCLAPI;
            case "default":
            default:
                return null;
        }
    }

    /**
     * MojangのURLをミラーURLにマッピングします。
     */
    private static String getMirrorMapping(int downloadClass, String mojangUrl) throws MalformedURLException{
        String[] mirrorSettings = getMirrorSettings();
        if(mirrorSettings == null) return mojangUrl;
        int urlTail = getBaseUrlTail(mojangUrl);
        String baseUrl = mojangUrl.substring(0, urlTail);
        String path = mojangUrl.substring(urlTail);
        switch(downloadClass) {
            case DOWNLOAD_CLASS_ASSETS:
            case DOWNLOAD_CLASS_METADATA:
                baseUrl = mirrorSettings[downloadClass];
                break;
            case DOWNLOAD_CLASS_LIBRARIES:
                if(!baseUrl.endsWith("libraries.minecraft.net")) break;
                baseUrl = mirrorSettings[downloadClass];
                break;
        }
        return baseUrl + path;
    }

    /**
     * URLからベースURLの末尾位置を取得します。
     */
    private static int getBaseUrlTail(String wholeUrl) throws MalformedURLException{
        int protocolNameEnd = wholeUrl.indexOf(URL_PROTOCOL_TAIL);
        if(protocolNameEnd == -1)
            throw new MalformedURLException("No protocol, or non path-based URL");
        protocolNameEnd += URL_PROTOCOL_TAIL.length();
        int hostnameEnd = wholeUrl.indexOf('/', protocolNameEnd);
        if(protocolNameEnd >= wholeUrl.length() || hostnameEnd == protocolNameEnd)
            throw new MalformedURLException("No hostname");
        if(hostnameEnd == -1) hostnameEnd = wholeUrl.length();
        return hostnameEnd;
    }
}
