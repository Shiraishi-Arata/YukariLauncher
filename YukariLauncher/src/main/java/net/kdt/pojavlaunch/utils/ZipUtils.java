package net.kdt.pojavlaunch.utils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtils {
    /**
     * ZIPエントリのInputStreamを取得します。エントリが存在しない場合はIOExceptionをスローします。
     * @param zipFile エントリを取得するZipFile
     * @param entryPath ZipFile内のフルパス
     * @return ZipFileによって提供されるInputStream
     * @throws IOException エントリが見つからなかった場合
     */
    public static InputStream getEntryStream(ZipFile zipFile, String entryPath) throws IOException{
        ZipEntry entry = zipFile.getEntry(entryPath);
        if(entry == null) throw new IOException("No entry in ZIP file: "+entryPath);
        return zipFile.getInputStream(entry);
    }

    /**
     * ZipFile内の指定されたディレクトリにあるすべてのファイルを、指定された宛先ディレクトリに抽出します。
     * dirNameの指定方法:
     * ZipFile内のすべてのファイルを抽出する場合は""を指定
     * 単一のディレクトリを抽出する場合は、そのフルパスに末尾の/を付けて指定
     * @param zipFile ファイルを抽出するZipFile
     * @param dirName ファイルを抽出するディレクトリ
     * @param destination ファイルの抽出先ディレクトリ
     * @throws IOException ディレクトリの作成またはファイルの抽出に失敗した場合
     */
    public static void zipExtract(ZipFile zipFile, String dirName, File destination) throws IOException {
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

        int dirNameLen = dirName.length();
        while(zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = zipEntries.nextElement();
            String entryName = zipEntry.getName();
            if(!entryName.startsWith(dirName) || zipEntry.isDirectory()) continue;
            File zipDestination = new File(destination, entryName.substring(dirNameLen));
            FileUtils.ensureParentDirectory(zipDestination);
            try (InputStream inputStream = zipFile.getInputStream(zipEntry);
                 OutputStream outputStream = new FileOutputStream(zipDestination)) {
                IOUtils.copy(inputStream, outputStream);
            }
        }
    }
}
