package net.kdt.pojavlaunch.utils;

import java.io.File;
import java.io.IOException;

public class FileUtils {
    /**
     * 文字列パスで指定されたファイルが存在するか確認する。
     * @param filePath 確認するファイルパス
     * @return ファイルが存在する場合はtrue（File.exists()と同じ）
     */
    public static boolean exists(String filePath){
        return new File(filePath).exists();
    }

    /**
     * パスまたはURL文字列からファイル名を取得する。
     * @param pathOrUrl ファイルのパスまたはURL
     * @return ファイル名。スラッシュがない場合はnull
     */
    public static String getFileName(String pathOrUrl) {
        int lastSlashIndex = pathOrUrl.lastIndexOf('/');
        if(lastSlashIndex == -1) return null;
        return pathOrUrl.substring(lastSlashIndex);
    }

    /**
     * パスまたはURL文字列から拡張子（最後のドット以降）を削除する。
     * @param pathOrUrl ファイルのパスまたはURL
     * @return 拡張子が削除された文字列
     */
    public static String removeExtension(String pathOrUrl) {
        int lastDotIndex = pathOrUrl.lastIndexOf('.');
        if(lastDotIndex == -1) return pathOrUrl;
        return pathOrUrl.substring(0, lastDotIndex);
    }

    /**
     * ディレクトリが存在し、ディレクトリであり、書き込み可能であることを確認する。
     * @param targetFile 確認するディレクトリ
     * @return 確認に成功した場合はtrue
     */
    public static boolean ensureDirectorySilently(File targetFile) {
        if(targetFile.isFile()) return false;
        if(targetFile.exists()) return targetFile.canWrite();
        else return targetFile.mkdirs();

    }

    /**
     * ファイルの親ディレクトリが存在し、書き込み可能であることを確認する。
     * @param targetFile 親ディレクトリを確認するファイル
     * @return 確認に成功した場合はtrue
     */
    public static boolean ensureParentDirectorySilently(File targetFile) {
        File parentFile = targetFile.getParentFile();
        if(parentFile == null) return false;
        return ensureDirectorySilently(parentFile);
    }

    /**
     * ensureDirectorySilently()と同じだが、失敗時にIOExceptionをスローする。
     * @param targetFile 確認するディレクトリ
     * @throws IOException 確認に失敗した場合
     */
    public static void ensureDirectory(File targetFile) throws IOException{
        if(targetFile.isFile()) throw new IOException("Target directory is a file");
        if(targetFile.exists()) {
            if(!targetFile.canWrite()) throw new IOException("Target directory is not writable");
        }else if(!targetFile.mkdirs()) throw new IOException("Unable to create target directory");
    }

    /**
     * ensureParentDirectorySilently()と同じだが、失敗時にIOExceptionをスローする。
     * @param targetFile 親ディレクトリを確認するファイル
     * @throws IOException 確認に失敗した場合
     */
    public static void ensureParentDirectory(File targetFile) throws IOException{
        File parentFile = targetFile.getParentFile();
        if(parentFile == null) throw new IOException("targetFile does not have a parent");
        ensureDirectory(parentFile);
    }
}