package com.kdt;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * スクロールビュー内のEditTextなどによるフォーカス移動を無視できるスクロールビュー
 * フォーカスを無視することで、ビューが自動的に再フォーカスされるのを防ぐ
 */
public class DefocusableScrollView extends ScrollView {

    private boolean mKeepFocusing = false;

    /**
     * コンストラクタ
     */
    public DefocusableScrollView(Context context) {
        super(context);
    }

    /**
     * コンストラクタ（属性指定）
     */
    public DefocusableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * コンストラクタ（属性・デフォルトスタイル指定）
     */
    public DefocusableScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * コンストラクタ（属性・デフォルトスタイル・リソーススタイル指定）
     */
    public DefocusableScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * フォーカス維持モードを設定する
     */
    public void setKeepFocusing(boolean shouldKeepFocusing){
        mKeepFocusing = shouldKeepFocusing;
    }

    /**
     * 現在フォーカス維持モードかどうかを返す
     */
    public boolean isKeepFocusing(){
        return mKeepFocusing;
    }

    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if(!mKeepFocusing) return 0;
        return super.computeScrollDeltaToGetChildRectOnScreen(rect);
    }

}
