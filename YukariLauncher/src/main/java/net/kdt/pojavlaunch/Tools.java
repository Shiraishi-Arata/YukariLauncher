package net.kdt.pojavlaunch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.*;
import android.util.*;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.arata.yukarilauncher.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.kdt.pojavlaunch.showError.RemoteErrorTask;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.io.IOUtils;

/**
 * アプリケーション全体で使用されるユーティリティメソッドを提供するクラス。
 */
public class Tools {
    public static final String APP_NAME = "YukariLauncher";
    public static final String APP_NAME_FULL = "YukariLauncher for Pojav";

    /** アプリケーションのローカルデータが保存されるディレクトリ名。 */
    public static final String DIRDATA_GAME_CTL_DIR = "game_control";
    public static final String DIRDATA_ACCOUNT_FILE = "account_control.json";
    public static final String DIRDATA_ACCOUNT_TABLE = "account_table.json";
    public static final String DIRDATA_ARCHIVE_CACHE = "archive_cache";
    public static final String DIRDATA_BOOTSTRAP_DIR = "bootstrap";
    public static final String DIRDATA_CACHE_DIR = "cache";
    public static final String DIRDATA_CRASH_FILE = "latestCrash.txt";
    public static final String DIRDATA_CRASH_DIR = "crash-reports";
    public static final String DIRDATA_ETC_DIR = "etc";
    public static final String DIRDATA_LOG_DIR = "logs";
    public static final String DIRDATA_MOD_DIR = "mods";
    public static final String DIRDATA_RESOURCE_PACK_DIR = "resourcepacks";
    public static final String DIRDATA_SCRIPTS_DIR = "scripts";
    public static final String DIRDATA_SHADERPACK_DIR = "shaderpacks";
    public static final String DIRDATA_TMP_DIR = "tmp";
    public static final String DIRDATA_WORKSPACE_DIR = "workspace";

    public static final String LIBCORE_DIR = "libcore";
    public static final String LIBLIB_DIR = "liblib";
    public static final String LIBRENDERER_DIR = "librenderer";

    public static final String URL_TOSTRING_RESULT = BuildConfig.URL_TOSTRING_RESULT;

    /**
     * JSONエンジン（日付はLongとしてパースされるように設定）。
     */
    public static final Gson GLOBAL_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            .registerTypeAdapter(Date.class, (com.google.gson.JsonDeserializer<Date>) (json, typeOfT, context) -> {
                if (json == null) return null;
                return new Date(json.getAsLong());
            })
            .create();

    private static final Runtime mRuntime = Runtime.getRuntime();
    private static final String androidTV = "ATV";
    private static final String androidAuto = "AA";

    /**
     * アクティビティと名前を基に新しいアラートダイアログを構築します。
     * @param activity 親アクティビティ
     * @param title ダイアログのタイトル
     * @param message ダイアログのメッセージ
     * @param emptyText 空のキャンセルボタンのテキスト（現在は未使用）
     * @param onCancel キャンセル時の動作
     * @return AlertDialog.Builder
     */
    public static AlertDialog.Builder newDialog(Activity activity, String title, String message, String emptyText, DialogInterface.OnCancelListener onCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        if (title != null) builder.setTitle(title);
        if (message != null) builder.setMessage(message);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.setOnCancelListener(onCancel);

        return builder;
    }

    /**
     * 単純なOKダイアログを表示します。
     * @param activity 親アクティビティ
     * @param title タイトル
     * @param message メッセージ
     */
    public static void dialog(Activity activity, String title, String message) {
        newDialog(activity, title, message, null, null).show();
    }

    /**
     * 単純なOKダイアログを表示します。
     * @param context コンテキスト
     * @param title タイトル
     * @param message メッセージ
     */
    public static void dialog(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * アプリケーションのローカルデータディレクトリのパスを取得します。
     * @param ctx コンテキスト
     * @return データディレクトリのパス
     */
    public static String getDataDirPath(Context ctx) {
        if (ctx == null) throw new IllegalArgumentException("コンテキストがnullです！");
        return ctx.getFilesDir().getAbsolutePath().replace(ctx.getPackageCodePath(), "");
    }

    /**
     * アプリケーションのデータディレクトリが存在しない場合は作成します。
     */
    public static void setupDataDir(Context ctx) throws IOException {
        File dataDir = ctx.getFilesDir();
        if (!dataDir.exists()) dataDir.mkdirs();
        // ディレクトリの存在確認の別の方法として、単にファイルの存在をチェック
        File testFile = new File(dataDir, ".test");
        testFile.createNewFile();
    }

    /**
     * ディレクトリを作成し、親ディレクトリも必要に応じて作成します。
     * @param file 作成するディレクトリ
     * @throws IOException 作成に失敗した場合
     */
    public static void createDirectoryIfNeeded(File file) throws IOException {
        if (file.exists()) return;
        if (!file.mkdirs()) throw new IOException("ディレクトリの作成に失敗しました: " + file.getAbsolutePath());
    }

    // ---- ディレクトリ構造管理 ----

    /**
     * コントローラレイアウトのパスを取得します。
     */
    public static String genControllerPath(String name) {
        return DIRDATA_GAME_CTL_DIR + "/" + name + ".json";
    }

    /**
     * ローカルカスタムコントローラのパスを取得します。
     */
    public static File getLocalControllerFile(Context ctx, String name) {
        return new File(ctx.getFilesDir(), genControllerPath(name));
    }

    /**
     * カスタムコントローラが変更されたかどうかを返します。
     */
    public static boolean isControllerModified(Context ctx, String name) {
        String cName = name.contains("/") ? name.substring(name.indexOf("/") + 1) : name;
        return getLocalControllerFile(ctx, cName).exists();
    }

    /**
     * srcからdstにInputStreamをコピーします。呼び出し側でストリームを閉じる必要があります。
     */
    public static void copy(InputStream src, OutputStream dst) throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        while ((len = src.read(buffer)) != -1) {
            dst.write(buffer, 0, len);
        }
    }

    /**
     * InputStream全体をバイト配列として読み取ります。
     */
    public static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(is.available(), 8192));
        copy(is, baos);
        return baos.toByteArray();
    }

    /**
     * ファイルをバイト配列として読み取ります。
     */
    public static byte[] read(String path) throws IOException {
        return read(new File(path));
    }

    /**
     * ファイルをバイト配列として読み取ります。
     */
    public static byte[] read(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            copy(fis, baos);
            return baos.toByteArray();
        }
    }

    /**
     * アセットから文字列を読み取ります。
     */
    public static String read(AssetManager assets, String name) throws IOException {
        try (InputStream is = assets.open(name)) {
            return new String(readAll(is), StandardCharsets.UTF_8);
        }
    }

    /**
     * アセットからバイナリデータを読み取ります。
     */
    public static byte[] readAsset(AssetManager assets, String name) throws IOException {
        try (InputStream is = assets.open(name)) {
            return readAll(is);
        }
    }

    /**
     * 指定されたファイルパスの文字列内容を読み取ります。
     */
    public static String readString(String path) throws IOException {
        return new String(read(new File(path)), StandardCharsets.UTF_8);
    }

    /**
     * InputStreamからすべてのテキストを読み取り、UTF-8文字列として返します。
     */
    public static String read(InputStream is) throws IOException {
        return new String(readAll(is), StandardCharsets.UTF_8);
    }

    /**
     * テキストをファイルに書き込みます。親ディレクトリが存在しない場合は作成します。
     */
    public static void write(String path, byte[] content) throws IOException {
        write(new File(path), content);
    }

    /**
     * テキストをファイルに書き込みます。親ディレクトリが存在しない場合は作成します。
     */
    public static void write(File file, byte[] content) throws IOException {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        }
    }

    /**
     * テキスト文字列をファイルに書き込みます。
     */
    public static void writeString(String path, String content) throws IOException {
        writeString(new File(path), content);
    }

    /**
     * テキスト文字列をファイルに書き込みます。
     */
    public static void writeString(File file, String content) throws IOException {
        write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * テキストファイルの内容のSHA256ハッシュを計算します。
     * テキストコンテンツからハッシュを計算するには、content.getBytes()を使用します。
     * @param content ハッシュ化するバイト配列
     * @return 16進数のハッシュ文字列
     */
    public static String calcHash(byte[] content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(content);
            byte[] hash = messageDigest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 2つのディレクトリ/ファイルを比較します。
     */
    public static boolean compare(File file1, File file2) throws IOException {
        if (file1.isDirectory() && file2.isDirectory()) {
            File[] files1 = file1.listFiles();
            File[] files2 = file2.listFiles();
            if (files1 == null || files2 == null) return files1 == files2;
            if (files1.length != files2.length) return false;
            for (int i = 0; i < files1.length; i++) {
                if (!compare(files1[i], files2[i])) return false;
            }
            return true;
        } else {
            return Arrays.equals(read(file1), read(file2));
        }
    }

    /**
     * 指定されたパッケージ名がインストールされているかどうかを確認します。
     */
    public static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * アクティビティを特定のオプションで再起動します（画面回転時など）。
     */
    public static void restartActivity(Activity activity) {
        Configuration config = activity.getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE || config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            activity.recreate();
        }
    }

    /**
     * 指定されたURLからファイル名を推測します。
     */
    public static String guessFileName(String url) {
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        return fileName.contains("?") ? fileName.substring(0, fileName.indexOf('?')) : fileName;
    }

    /**
     * URLから拡張子を推測します。
     */
    public static String guessMimeType(String url) {
        return URLConnection.guessContentTypeFromName(url);
    }

    /**
     * 文字列を比較して一致するかどうかを返します。
     */
    public static boolean compare(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    /**
     * assetsからアプリケーションデータディレクトリにファイルをコピーします。
     */
    public static void copyAssets(AssetManager assetManager, String assetsDir, String outputDir) throws IOException {
        String[] list = assetManager.list(assetsDir);
        if (list == null) return;
        File outputDirFile = new File(outputDir);
        for (String item : list) {
            File outputFile = new File(outputDirFile, item);
            String assetPath = assetsDir.isEmpty() ? item : assetsDir + "/" + item;
            try {
                if (assetManager.open(assetPath) != null) {
                    copy(assetManager.open(assetPath), outputFile);
                }
            } catch (IOException e) {
                copyAssets(assetManager, assetPath, outputFile.getAbsolutePath());
            }
        }
    }

    /**
     * アセットからファイルを直接コピーします。
     */
    public static void copyAsset(AssetManager assetManager, String assetPath, File outputFile) throws IOException {
        try (InputStream is = assetManager.open(assetPath);
             FileOutputStream os = new FileOutputStream(outputFile)) {
            copy(is, os);
        }
    }

    /**
     * InputStreamをファイルに直接コピーします。
     */
    public static void copy(InputStream is, File file) throws IOException {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        try (FileOutputStream os = new FileOutputStream(file)) {
            copy(is, os);
        }
    }

    /**
     * ファイルをコピーします。
     */
    public static void copyFile(File src, File dst) throws IOException {
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            copy(fis, fos);
        }
    }

    /**
     * エラーダイアログを表示します。ShowErrorActivityから呼び出された場合、アクティビティ終了処理を行います。
     */
    public static void showError(Activity activity, String message, Throwable e, boolean ended) {
        if (e instanceof ContextExecutorTask) {
            ((ContextExecutorTask) e).executeWithActivity(activity);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        if (message != null) {
            builder.setTitle(message);
        } else {
            builder.setTitle(activity.getString(R.string.global_error));
        }

        if (e != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            if (e.getMessage() != null) pw.println(e.getMessage());
            e.printStackTrace(pw);
            pw.flush();
            builder.setMessage(sw.toString());
        }

        if (ended) {
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> activity.finish());
        } else {
            builder.setPositiveButton(android.R.string.ok, null);
        }

        builder.setNeutralButton(R.string.global_error_copy, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("error", e != null ? Log.getStackTraceString(e) : message);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(activity, R.string.global_error_copied, Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    /**
     * エラーをダイアログとして表示します。
     */
    public static void showError(final Context context, final Throwable e) {
        showError(context, e, false);
    }

    /**
     * エラーダイアログを表示します。
     */
    public static void showError(final Context context, final Throwable e, final Boolean ended) {
        if (context instanceof Activity) {
            showError((Activity) context, context.getString(R.string.global_error), e, ended);
        } else {
            final RemoteErrorTask task = new RemoteErrorTask(e, context.getString(R.string.global_error));
            ContextExecutor executor = new ContextExecutor();
        }
    }

    /**
     * エラーをダイアログとして表示します。
     */
    public static void showError(final Context context, final String message, final Throwable e) {
        if (context instanceof Activity) {
            showError((Activity) context, message, e, false);
        } else {
            final RemoteErrorTask task = new RemoteErrorTask(e, message);
            ContextExecutor executor = new ContextExecutor();
        }
    }

    /**
     * エラーをリモートで表示します。
     */
    public static void showErrorRemote(String message, Throwable e) {
        RemoteErrorTask task = new RemoteErrorTask(e, message);
        ContextExecutor.executeTask(task);
    }

    /**
     * エラーをリモートで表示します。
     */
    public static void showErrorRemote(Throwable e) {
        showErrorRemote(null, e);
    }

    /**
     * エラーをトーストで表示します。
     */
    public static void showError(final Context context, final Throwable e, final int length) {
        Toast.makeText(context, e.getMessage(), length).show();
    }

    /**
     * CRC32ビルドバリアントのセレクタ。
     */
    private static String sBuildVariant;
    private static boolean sIsTV;
    private static boolean sIsCar;
    private static boolean sIsHuawei;

    /**
     * ビルドバリアントを取得します。
     */
    public static String getBuildVariant() {
        return sBuildVariant;
    }

    /**
     * デバイスがAndroid TVかどうかを返します。
     */
    public static boolean isTV() {
        return sIsTV;
    }

    /**
     * デバイスがAndroid Autoかどうかを返します。
     */
    public static boolean isCar() {
        return sIsCar;
    }

    /**
     * デバイスがHuaweiかどうかを返します。
     */
    public static boolean isHuawei() {
        return sIsHuawei;
    }

    /**
     * ビルドバリアントを設定します。（起動時に1回だけ呼び出す必要があります）
     */
    public static void setBuildVariant(PackageManager pm) {
        if (sBuildVariant != null) return;
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_ACTIVITIES);
            sBuildVariant = pkgInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            sBuildVariant = BuildConfig.VERSION_NAME;
        }

        sIsTV = pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION);
        sIsCar = pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);

        try {
            Class<?> huaweiApi = Class.forName("com.huawei.android.os.BuildEx");
            Method method = huaweiApi.getMethod("getOsBrand");
            sIsHuawei = method.invoke(null) != null;
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            sIsHuawei = false;
        }
    }

    /**
     * 指定されたディレクトリを再帰的に削除します。
     */
    public static void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) deleteDir(file);
                else file.delete();
            }
        }
        dir.delete();
    }

    /**
     * ディレクトリの合計サイズをバイト単位で計算します。
     */
    public static long calculateDirSize(File dir) {
        long size = 0;
        if (dir == null || !dir.exists()) return 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) size += calculateDirSize(file);
                else size += file.length();
            }
        }
        return size;
    }

    /**
     * ディレクトリを別の場所にコピーします。
     */
    public static void copyDir(File src, File dst) throws IOException {
        if (!src.exists()) return;
        if (src.isDirectory()) {
            if (!dst.exists()) dst.mkdirs();
            File[] files = src.listFiles();
            if (files != null) {
                for (File file : files) {
                    copyDir(file, new File(dst, file.getName()));
                }
            }
        } else {
            copyFile(src, dst);
        }
    }

    /**
     * assetsから特定のパスにファイルをコピーします。
     */
    public static String copyFromAssets(AssetManager assets, String assetsPath, String targetPath) throws IOException {
        File f = new File(targetPath, assetsPath);
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        try (InputStream is = assets.open(assetsPath);
             FileOutputStream fos = new FileOutputStream(f)) {
            copy(is, fos);
        }
        return f.getAbsolutePath();
    }

    /**
     * MIMEタイプに関連付けられたデフォルトのファイル名を返します。
     */
    public static String defaultMimeType(String mimeType) {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
    }

    /**
     * 最小ランチャーバージョンを検証します。
     */
    public static boolean isLauncherVersionValid(int minimumLauncherVersion) {
        return minimumLauncherVersion <= BuildConfig.LAUNCHER_VERSION_CODE;
    }

    /**
     * ファイルを安全に削除しようとします。
     */
    public static void tryDelete(File file) {
        try {
            if (file.exists()) file.delete();
        } catch (Exception e) {
            // 無視
        }
    }
}
