package com.arata.yukarilauncher.ui.subassembly.customcontrols;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.task.Task;
import com.arata.yukarilauncher.task.TaskExecutors;
import com.arata.yukarilauncher.ui.dialog.DeleteDialog;
import com.arata.yukarilauncher.ui.subassembly.filelist.RefreshListener;
import com.arata.yukarilauncher.utils.path.PathManager;
import com.arata.yukarilauncher.utils.file.FileTools;
import com.arata.yukarilauncher.utils.stringutils.StringFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * コントロール設定一覧の表示を管理するクラス
 */
public class ControlsListViewCreator {
    private final Context context;
    private final RecyclerView mainListView;
    private final AtomicInteger searchCount = new AtomicInteger(0);

    private ControlListAdapter controlListAdapter;
    private ControlSelectedListener selectedListener;
    private RefreshListener refreshListener;
    private File fullPath = new File(PathManager.DIR_CTRLMAP_PATH);
    private String filterString = "";
    private boolean showSearchResultsOnly = false;
    private boolean caseSensitive = false;
    private TextView searchCountText;

    /**
     * ビュークリエーターを構築する
     */
    public ControlsListViewCreator(Context context, RecyclerView recyclerView) {
        this.context = context;
        this.mainListView = recyclerView;
        init();
    }

    /**
     * アダプターとレイアウトを初期化する
     */
    public void init() {
        controlListAdapter = new ControlListAdapter();
        controlListAdapter.setOnItemClickListener(new ControlListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String name) {
                File file = new File(fullPath, name);
                if (selectedListener != null) selectedListener.onItemSelected(file);
            }

            @Override
            public void onLongClick(String name) {
                File file = new File(fullPath, name);
                if (selectedListener != null) selectedListener.onItemLongClick(file);
            }

            @Override
            public void onInvalidItemClick(String name) {
                File file = new File(fullPath, name);
                List<File> files = new ArrayList<>();
                files.add(file);
                new DeleteDialog(
                        context,
                        Task.runTask(TaskExecutors.getAndroidUI(), () -> {
                            refresh();
                            return null;
                        }),
                        files
                ).show();
            }
        });

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
        mainListView.setLayoutManager(layoutManager);
        mainListView.setLayoutAnimation(new LayoutAnimationController(AnimationUtils.loadAnimation(context, R.anim.fade_downwards)));
        mainListView.setAdapter(controlListAdapter);
    }

    /**
     * 選択リスナーを設定する
     */
    public void setSelectedListener(ControlSelectedListener listener) {
        this.selectedListener = listener;
    }

    /**
     * 更新リスナーを設定する
     */
    public void setRefreshListener(RefreshListener listener) {
        this.refreshListener = listener;
    }

    /**
     * 検索結果のみ表示するかどうかを設定する
     */
    public void setShowSearchResultsOnly(boolean showSearchResultsOnly) {
        this.showSearchResultsOnly = showSearchResultsOnly;
    }

    /**
     * アイテム数を取得する
     */
    public int getItemCount() {
        return controlListAdapter.getItemCount();
    }

    /**
     * 指定パスからコントロールファイルのリストを読み込む
     */
    private List<ControlItemBean> loadInfoData(File path) {
        List<ControlItemBean> data = new ArrayList<>();

        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    ControlInfoData controlInfoData = null;
                    if (file.getName().endsWith(".json")) {
                        controlInfoData = EditControlData.loadFormFile(context, file);
                    }

                    ControlItemBean controlItemBean;
                    if (controlInfoData == null) {
                        ControlInfoData invalidInfoData = new ControlInfoData();
                        invalidInfoData.fileName = file.getName();
                        controlItemBean = new ControlItemBean(invalidInfoData);
                        controlItemBean.isInvalid = true;
                    } else {
                        controlItemBean = new ControlItemBean(controlInfoData);
                        if (shouldHighlight(controlInfoData, file)) {
                            controlItemBean.isHighlighted = true;
                            searchCount.addAndGet(1);
                        } else if (showSearchResultsOnly) {
                            continue;
                        }
                    }

                    data.add(controlItemBean);
                }
            }
        }

        return data;
    }

    /**
     * 検索条件に合致するか判定する
     */
    private boolean shouldHighlight(ControlInfoData controlInfoData, File file) {
        if (filterString == null || filterString.isEmpty()) return false;

        String name = controlInfoData.name;
        String searchString = !name.isEmpty() && !name.equals("null") ? name : file.getName();

        return StringFilter.containsSubstring(searchString, filterString, caseSensitive) ||
                StringFilter.containsSubstring(file.getName(), filterString, caseSensitive);
    }

    /**
     * コントロールファイルのパスで一覧を再表示する
     */
    public void listAtPath() {
        this.fullPath = controlPath();
        refresh();
    }

    /**
     * 現在のフルパスを取得する
     */
    public File getFullPath() {
        return this.fullPath;
    }

    /**
     * 検索を実行する
     */
    public void searchControls(TextView searchCountText, String filterString, boolean caseSensitive) {
        searchCount.set(0);
        this.filterString = filterString;
        this.caseSensitive = caseSensitive;
        this.searchCountText = searchCountText;
        refresh();
    }

    /**
     * コントロール設定のディレクトリパスを取得する
     */
    private File controlPath() {
        File ctrlPath = new File(PathManager.DIR_CTRLMAP_PATH);
        if (!ctrlPath.exists()) FileTools.mkdirs(ctrlPath);
        return ctrlPath;
    }

    /**
     * リストを更新する
     */
    @SuppressLint("NotifyDataSetChanged")
    public void refresh() {
        Task.runTask(() -> {
            List<ControlItemBean> itemBeans = loadInfoData(fullPath);
            filterString = "";
            return itemBeans;
        }).ended(TaskExecutors.getAndroidUI(), data -> {
            controlListAdapter.updateItems(data);
            mainListView.scheduleLayoutAnimation();

            if (searchCountText != null) {
                int count = searchCount.get();
                searchCountText.setText(searchCountText.getContext().getString(R.string.search_count, count));
                if (count != 0) searchCountText.setVisibility(View.VISIBLE);
            }

            if (refreshListener != null) refreshListener.onRefresh();
        }).execute();
    }
}
