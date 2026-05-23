package com.kdt.mcgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;

import com.arata.yukarilauncher.R;

/**
 * テキストを表示可能な進捗バー
 * 進捗状況に応じて現在値をテキストとして画面上に描画する
 */
public class TextProgressBar extends ProgressBar {

    private int mTextPadding = 0;

    /**
     * コンストラクタ
     */
    public TextProgressBar(Context context) {super(context, null, android.R.attr.progressBarStyleHorizontal); init();}

    /**
     * コンストラクタ（属性指定）
     */
    public TextProgressBar(Context context, AttributeSet attrs) {super(context, attrs, android.R.attr.progressBarStyleHorizontal); init();}

    /**
     * コンストラクタ（属性・デフォルトスタイル指定）
     */
    public TextProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, android.R.attr.progressBarStyleHorizontal);
        init();
    }

    /**
     * コンストラクタ（属性・デフォルトスタイル・リソーススタイル指定）
     */
    public TextProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, android.R.attr.progressBarStyleHorizontal, defStyleRes);
        init();
    }

    private Paint mTextPaint;
    private String mText = "";

    /**
     * プログレスバーの初期化処理
     * 描画用ドロウアブルの設定とテキスト描画用ペイントの初期化を行う
     */
    private void init(){
        setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.view_text_progressbar, null));
        setProgress(0);
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setFlags(Paint.FAKE_BOLD_TEXT_FLAG);
        mTextPaint.setAntiAlias(true);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mTextPaint.setTextSize((float) ((getHeight()- getPaddingBottom() - getPaddingTop()) * 0.55));
        mTextPaint.setColor(getResources().getColor(R.color.primary_text, getContext().getTheme()));
        int xPos = (int) Math.max(Math.min((getProgress() * getWidth() / getMax()) + mTextPadding, getWidth() - mTextPaint.measureText(mText) - mTextPadding) , mTextPadding);
        int yPos = (int) ((getHeight() / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)) ;

        canvas.drawText(mText, xPos, yPos, mTextPaint);
    }


    /**
     * 表示テキストをリソースIDから設定する
     */
    public final void setText(@StringRes int resid) {
        setText(getContext().getResources().getText(resid).toString());
    }

    /**
     * 表示テキストを設定する
     */
    public final void setText(String text){
        mText = text;
        invalidate();
    }

    /**
     * テキストのパディング値を設定する
     */
    public final void setTextPadding(int padding){
        mTextPadding = padding;
    }
}
