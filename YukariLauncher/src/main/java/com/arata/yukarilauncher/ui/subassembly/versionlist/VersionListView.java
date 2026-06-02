package com.arata.yukarilauncher.ui.subassembly.versionlist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.Tools;
import com.arata.yukarilauncher.event.sticky.MinecraftVersionValueEvent;
import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.task.TaskExecutors;
import com.arata.yukarilauncher.ui.subassembly.filelist.FileItemBean;
import com.arata.yukarilauncher.ui.subassembly.filelist.FileRecyclerViewCreator;
import com.arata.yukarilauncher.utils.FilteredSubList;
import com.arata.yukarilauncher.value.JMinecraftVersionList;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.greenrobot.eventbus.EventBus;

import kotlin.Pair;

/**
 * Minecraftバージョン一覧表示用のカスタムLinearLayout
 */
public class VersionListView extends LinearLayout {
    private static final Set<String> APRIL_FOOLS_IDS = new HashSet<>(Arrays.asList(
            "2.0_purple",
            "2.0_red",
            "2.0_blue",
            "15w14a",
            "1.rv-pre1",
            "3d shareware v1.34",
            "20w14infinite",
            "22w13oneblockatatime",
            "23w13a_or_b",
            "24w14potato",
            "25w14craftmine",
            "26w14a"
    ));

    private Context context;
    private List<JMinecraftVersionList.Version> releaseList, snapshotList, betaList, alphaList, aprilFoolsList;
    private FileRecyclerViewCreator fileRecyclerViewCreator;
    private VersionSelectedListener versionSelectedListener;

    public VersionListView(Context context) {
        this(context, null);
    }

    public VersionListView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VersionListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * ビューを初期化する
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void init(Context context) {
        this.context = context;

        LayoutParams layParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        setOrientation(VERTICAL);

        RecyclerView mainListView = new RecyclerView(context);

        JMinecraftVersionList.Version[] versionArray;
        MinecraftVersionValueEvent event = EventBus.getDefault().getStickyEvent(MinecraftVersionValueEvent.class);

        if (event != null) {
            JMinecraftVersionList jMinecraftVersionList = event.getList();
            boolean isVersionsNotNull = jMinecraftVersionList != null && jMinecraftVersionList.versions != null;
            versionArray = isVersionsNotNull ? jMinecraftVersionList.versions : new JMinecraftVersionList.Version[0];
        } else {
            versionArray = new JMinecraftVersionList.Version[0];
        }

        releaseList = new FilteredSubList<>(versionArray, item -> item.type.equals("release"));
        snapshotList = new FilteredSubList<>(versionArray, item -> item.type.equals("snapshot"));
        betaList = new FilteredSubList<>(versionArray, item -> item.type.equals("old_beta"));
        alphaList = new FilteredSubList<>(versionArray, item -> item.type.equals("old_alpha"));
        aprilFoolsList = new FilteredSubList<>(versionArray, item -> isAprilFoolsVersion(item.id, item.type));

        fileRecyclerViewCreator = new FileRecyclerViewCreator(
                context,
                mainListView,
                (position, fileItemBean) -> versionSelectedListener.onVersionSelected(fileItemBean.name),
                null,
                showVersions(VersionType.RELEASE)
        );

        addView(mainListView, layParam);
    }

    /**
     * バージョンリストから日付付きのペア配列を生成する
     */
    private Pair<String, Date>[] getVersionPair(List<JMinecraftVersionList.Version> versions) {
        List<Pair<String, Date>> pairList = new ArrayList<>();
        for (int i = 0; i < versions.size(); i++) {
            JMinecraftVersionList.Version version = versions.get(i);
            Date date;
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(version.releaseTime, formatter);
                date = Date.from(zonedDateTime.toInstant());
            } catch (Exception e) {
                Logging.e("Version List", Tools.printToString(e));
                date = null;
            }
            pairList.add(new Pair<>(version.id, date));
        }
        return pairList.toArray(new Pair[0]);
    }

    /**
     * バージョン選択リスナーを設定する
     */
    public void setVersionSelectedListener(VersionSelectedListener versionSelectedListener) {
        this.versionSelectedListener = versionSelectedListener;
    }

    /**
     * 表示するバージョン種別を設定する
     */
    public void setVersionType(VersionType versionType) {
        showVersions(versionType);
    }

    /**
     * フィルター文字列を設定する
     */
    public void setFilterString(String filterString) {
        this.fileRecyclerViewCreator.setFilterString(filterString);
    }

    /**
     * 指定されたバージョン種別のリストを表示する
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private List<FileItemBean> showVersions(VersionType versionType) {
        switch (versionType) {
            case SNAPSHOT:
                return getVersion(context.getDrawable(R.drawable.ic_command_block), getVersionPair(snapshotList));
            case BETA:
                return getVersion(context.getDrawable(R.drawable.ic_old_cobblestone), getVersionPair(betaList));
            case ALPHA:
                return getVersion(context.getDrawable(R.drawable.ic_old_grass_block), getVersionPair(alphaList));
            case APRIL_FOOLS:
                return getVersion(context.getDrawable(R.drawable.ic_command_block), getVersionPair(aprilFoolsList));
            case RELEASE:
            default:
                return getVersion(context.getDrawable(R.drawable.ic_minecraft), getVersionPair(releaseList));
        }
    }

    /**
     * エイプリルフールバージョンかどうかを判定する
     */
    private boolean isAprilFoolsVersion(String id, String type) {
        if (type != null && type.toLowerCase(Locale.ROOT).contains("april")) return true;
        if (id == null) return false;
        return APRIL_FOOLS_IDS.contains(id.toLowerCase(Locale.ROOT));
    }

    /**
     * アイコン付きバージョン一覧を取得する
     */
    private List<FileItemBean> getVersion(Drawable icon, Pair<String, Date>[] namesPair) {
        List<FileItemBean> itemBeans = FileRecyclerViewCreator.loadItemBean(icon, namesPair);
        TaskExecutors.runInUIThread(() -> fileRecyclerViewCreator.loadData(itemBeans));
        return itemBeans;
    }
}
