package com.arata.yukarilauncher.ui.subassembly.filelist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.task.TaskExecutors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ファイル一覧表示用のカスタムLinearLayout
 */
@SuppressLint("ViewConstructor")
public class FileRecyclerView extends LinearLayout {
    private final AtomicInteger searchCount = new AtomicInteger(0);
    private Context context;
    private FileRecyclerViewCreator fileRecyclerViewCreator;
    private FileIcon fileIcon = FileIcon.FILE;
    private SetTitleListener mSetTitleListener;
    private FileSelectedListener fileSelectedListener;
    private RefreshListener mRefreshListener;
    private File fullPath;
    private File lockPath = new File("/");
    private boolean showFiles = true;
    private boolean showFolders = true;
    private String filterString = "";
    private boolean showSearchResultsOnly = false;
    private boolean caseSensitive = false;

    public FileRecyclerView(Context context) {
        this(context, null);
    }

    public FileRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FileRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * ビューを初期化する
     */
    public void init(final Context context) {
        this.context = context;

        LayoutParams layParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        setOrientation(VERTICAL);

        RecyclerView mainLv = new RecyclerView(context);

        fileRecyclerViewCreator = new FileRecyclerViewCreator(
                context,
                mainLv,
                (position, fileItemBean) -> {
                    if (position == 0 && !lockPath.equals(fullPath)) {
                        parentDir();
                    } else {
                        listFileAt(fileItemBean.file);
                    }
                },
                (position, fileItemBean) -> {
                    File file = fileItemBean.file;
                    if (file != null) {
                        if (position == 0 && !lockPath.equals(fullPath)) {
                            parentDir();
                        } else {
                            fileSelectedListener.onItemLongClick(file, file.getAbsolutePath());
                        }
                    }
                },
                new ArrayList<>());

        addView(mainLv, layParam);
    }

    /**
     * ファイル選択リスナーを設定する
     */
    public void setFileSelectedListener(FileSelectedListener listener) {
        this.fileSelectedListener = listener;
    }

    /**
     * マルチ選択リスナーを設定する
     */
    public void setOnMultiSelectListener(FileRecyclerAdapter.OnMultiSelectListener listener) {
        this.fileRecyclerViewCreator.setOnMultiSelectListener(listener);
    }

    /**
     * タイトルリスナーを設定する
     */
    public void setTitleListener(SetTitleListener setTitleListener) {
        this.mSetTitleListener = setTitleListener;
    }

    /**
     * 更新リスナーを設定する
     */
    public void setRefreshListener(RefreshListener listener) {
        this.mRefreshListener = listener;
    }

    /**
     * ファイル表示の有無を設定する
     */
    public void setShowFiles(boolean showFiles) {
        this.showFiles = showFiles;
    }

    /**
     * フォルダ表示の有無を設定する
     */
    public void setShowFolders(boolean showFolders) {
        this.showFolders = showFolders;
    }

    /**
     * ファイルアイコン種別を設定する
     */
    public void setFileIcon(FileIcon fileIcon) {
        this.fileIcon = fileIcon;
    }

    /**
     * ファイルが存在しないかどうかを返す
     */
    public boolean isNoFile() {
        return fileRecyclerViewCreator.isNoFile();
    }

    /**
     * ファイルを検索する
     */
    public int searchFiles(String filterString, boolean caseSensitive) {
        searchCount.set(0);
        this.filterString = filterString;
        this.caseSensitive = caseSensitive;
        refreshPath();
        return searchCount.get();
    }

    /**
     * 検索結果のみ表示するかどうかを設定する
     */
    public void setShowSearchResultsOnly(boolean showSearchResultsOnly) {
        this.showSearchResultsOnly = showSearchResultsOnly;
    }

    /**
     * アダプターを取得する
     */
    public FileRecyclerAdapter getAdapter() {
        return fileRecyclerViewCreator.fileRecyclerAdapter;
    }

    /**
     * アイテム数を取得する
     */
    public int getItemCount() {
        return fileRecyclerViewCreator.fileRecyclerAdapter.getItemCount();
    }

    /**
     * ロックパスを設定して指定パスのファイル一覧を表示する
     */
    public void lockAndListAt(File lockPath, File listPath) {
        this.lockPath = lockPath;
        listFileAt(listPath);
    }

    /**
     * 指定パスのファイル一覧を表示する
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    public void listFileAt(final File path) {
        if (path != null && path.exists()) {
            if (path.isDirectory()) {
                fullPath = path;

                List<FileItemBean> itemBeans = FileRecyclerViewCreator.loadItemBeansFromPath(context, this.filterString, showSearchResultsOnly, caseSensitive, searchCount, path, this.fileIcon, this.showFiles, this.showFolders);
                Collections.sort(itemBeans);
                filterString = "";

                if (!path.equals(lockPath)) {
                    FileItemBean itemBean = new FileItemBean(
                            "..",
                            context.getResources().getDrawable(R.drawable.ic_folder, context.getTheme())
                    );
                    itemBean.isCanCheck = false;
                    itemBeans.add(0, itemBean);
                }

                if (mSetTitleListener != null) {
                    mSetTitleListener.setTitle(path.getAbsolutePath());
                }

                TaskExecutors.runInUIThread(() -> {
                    fileRecyclerViewCreator.loadData(itemBeans);
                    if (mRefreshListener != null) mRefreshListener.onRefresh();
                });
            } else {
                fileSelectedListener.onFileSelected(path, path.getAbsolutePath());
            }
        } else {
            listFileAt(Environment.getExternalStorageDirectory());
            Toast.makeText(context, R.string.file_does_not_exist, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 現在のフルパスを取得する
     */
    public File getFullPath() {
        return fullPath;
    }

    /**
     * 現在のパスを再表示する
     */
    public void refreshPath() {
        listFileAt(getFullPath());
    }

    /**
     * 親ディレクトリに移動する
     */
    public void parentDir() {
        if (!fullPath.getAbsolutePath().equals("/")) {
            listFileAt(fullPath.getParentFile());
        }
    }
}
