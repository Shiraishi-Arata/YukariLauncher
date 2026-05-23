package com.arata.yukarilauncher.ui.subassembly.about;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * アバウト画面のアイテムデータを保持するBeanクラス
 */
public class AboutItemBean {
    private final Drawable icon;
    private final String title, desc;
    private final AboutItemButtonBean buttonBean;

    /**
     * アバウトアイテムを構築する
     */
    public AboutItemBean(@NonNull Drawable icon, @NonNull String title, @NonNull String desc, AboutItemButtonBean buttonBean) {
        this.icon = icon;
        this.title = title;
        this.desc = desc;
        this.buttonBean = buttonBean;
    }

    /** アイコンを取得する */
    public Drawable getIcon() {
        return icon;
    }

    /** タイトルを取得する */
    public String getTitle() {
        return title;
    }

    /** 説明を取得する */
    public String getDesc() {
        return desc;
    }

    /** ボタンデータを取得する */
    public AboutItemButtonBean getButtonBean() {
        return buttonBean;
    }

    /**
     * アバウトアイテムのボタンデータを保持する内部クラス
     */
    public static class AboutItemButtonBean {
        private final Activity activity;
        private final String name, url;

        /**
         * ボタンデータを構築する
         */
        public AboutItemButtonBean(@NonNull Activity activity, @NonNull String name, @NonNull String url) {
            this.activity = activity;
            this.name = name;
            this.url = url;
        }

        /** アクティビティを取得する */
        public Activity getActivity() {
            return activity;
        }

        /** ボタン名を取得する */
        public String getName() {
            return name;
        }

        /** URLを取得する */
        public String getUrl() {
            return url;
        }
    }
}
