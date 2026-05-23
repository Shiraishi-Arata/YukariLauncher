package com.arata.yukarilauncher.utils.file;

import android.app.Activity;
import android.widget.Toast;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.task.Task;
import com.arata.yukarilauncher.task.TaskExecutors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ファイルの貼り付け操作を管理するクラス
 * コピー・移動操作の状態を保持し、貼り付け処理を実行する
 */
public class PasteFile {
    private static final PasteFile instance = new PasteFile();
    private final List<File> copyFiles = new ArrayList<>();
    private File mRoot = null;
    private PasteType pasteType = null;

    private PasteFile() {
    }

    /**
     * シングルトンインスタンスを取得する
     */
    public static PasteFile getInstance() {
        return instance;
    }

    /**
     * コピーするファイルを設定する
     */
    public void setCopyFiles(List<File> files) {
        this.copyFiles.clear();
        this.copyFiles.addAll(files);
    }

    /**
     * 貼り付け操作のルート・ファイル・種類を設定する
     */
    public void setPaste(File root, List<File> files, PasteType type) {
        setCopyFiles(files);
        this.mRoot = root;
        this.pasteType = type;
    }

    /**
     * 現在の貼り付け操作の種類を取得する
     */
    public PasteType getPasteType() {
        return pasteType;
    }

    /**
     * ファイルの貼り付け処理を実行する
     * コピー元ファイルが存在しない場合はトーストを表示する
     */
    public void pasteFiles(Activity activity, File target, FileCopyHandler.FileExtensionGetter fileExtensionGetter, Task<?> endTask) {
        if (copyFiles.isEmpty()) {
            TaskExecutors.runInUIThread(() -> Toast.makeText(activity, activity.getString(R.string.file_does_not_exist), Toast.LENGTH_SHORT).show());
            return;
        }
        new FileCopyHandler(activity, pasteType, copyFiles, mRoot, target, fileExtensionGetter, endTask.beforeStart(this::resetState)).start();
    }

    /**
     * 状態をリセットする
     */
    private void resetState() {
        pasteType = null;
        copyFiles.clear();
    }

    /**
     * 貼り付け操作の種類を表す列挙型
     */
    public enum PasteType {
        /** コピー */
        COPY,
        /** 移動 */
        MOVE
    }
}
