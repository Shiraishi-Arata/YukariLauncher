package net.kdt.pojavlaunch.colorselector;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.listener.SimpleTextWatcher;

/**
 * カラーセレクターのメインクラス。色相、彩度・明度、アルファの各ビューを統合し、色選択を管理します。
 */
public class ColorSelector implements HueSelectionListener, RectangleSelectionListener, AlphaSelectionListener, SimpleTextWatcher {
    private static final int ALPHA_MASK = ~(0xFF << 24);
    private final View mRootView;
    private final HueView mHueView;
    private final SVRectangleView mLuminosityIntensityView;
    private final AlphaView mAlphaView;
    private final ColorSideBySideView mColorView;
    private final EditText mTextView;

    private ColorSelectionListener mColorSelectionListener;
    private final float[] mHueTemplate = new float[] {0,1,1};
    private final float[] mHsvSelected = new float[] {360,1,1};
    private int mAlphaSelected = 0xff;
    private final ColorStateList mTextColors;
    private boolean mWatch = true;

    private boolean mAlphaEnabled = true;

    /**
     * カラーセレクターを作成します。
     * @param context コンテキスト
     * @param parent 親ビューグループ
     * @param colorSelectionListener 色選択リスナー（null可）
     */
    public ColorSelector(Context context, ViewGroup parent, @Nullable ColorSelectionListener colorSelectionListener) {
        mRootView = LayoutInflater.from(context).inflate(R.layout.dialog_color_selector,parent, false);
        mHueView = mRootView.findViewById(R.id.color_selector_hue_view);
        mLuminosityIntensityView = mRootView.findViewById(R.id.color_selector_rectangle_view);
        mAlphaView = mRootView.findViewById(R.id.color_selector_alpha_view);
        mColorView = mRootView.findViewById(R.id.color_selector_color_view);
        mTextView = mRootView.findViewById(R.id.color_selector_hex_edit);
        runColor(Color.RED);
        mHueView.setHueSelectionListener(this);
        mLuminosityIntensityView.setRectSelectionListener(this);
        mAlphaView.setAlphaSelectionListener(this);
        mTextView.addTextChangedListener(this);
        mTextColors = mTextView.getTextColors();
        mColorSelectionListener = colorSelectionListener;
        parent.addView(mRootView);
    }

    /**
     * @return ルートビュー。主に位置操作のため。
     */
    public View getRootView(){
        return mRootView;
    }

    /**
     * カラーセレクターをデフォルト（赤）色で表示します。
     */
    public void show() {
        show(Color.RED);
    }

    /**
     * カラーセレクターを指定されたARGB色で表示します。
     * @param previousColor 初期表示するARGB色
     */
    public void show(int previousColor) {
        runColor(previousColor);
        dispatchColorChange();
    }

    /**
     * 色相が選択されたときの処理。
     */
    @Override
    public void onHueSelected(float hue) {
        mHsvSelected[0] = mHueTemplate[0] = hue;
        mLuminosityIntensityView.setColor(Color.HSVToColor(mHueTemplate), true);
        dispatchColorChange();
    }

    /**
     * 彩度・明度が変更されたときの処理。
     */
    @Override
    public void onLuminosityIntensityChanged(float luminosity, float intensity) {
        mHsvSelected[1] = intensity;
        mHsvSelected[2] = luminosity;
        dispatchColorChange();
    }

    /**
     * アルファ値が選択されたときの処理。
     */
    @Override
    public void onAlphaSelected(int alpha) {
        mAlphaSelected = alpha;
        dispatchColorChange();
    }

    /**
     * 色のアルファ値を置き換えて結果を返します。
     * @param color アルファを置き換える色
     * @param alpha 使用するアルファ値
     * @return 新しい色
     */
    public static int setAlpha(int color, int alpha) {
        return color & ALPHA_MASK | ((alpha & 0xFF) << 24);
    }

    /**
     * 色変更を全ビューに通知します。
     */
    protected void dispatchColorChange() {
        int color = Color.HSVToColor(mAlphaSelected, mHsvSelected);
        mColorView.setColor(color);
        mWatch = false;
        mTextView.setText(String.format("%08X",color));
        notifyColorSelector(color);
    }

    /**
     * すべてのビューを指定された色で描画します。初期化とHEX入力に使用します。
     */
    protected void runColor(int color) {
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), mHsvSelected);
        mHueTemplate[0] = mHsvSelected[0];
        mHueView.setHue(mHsvSelected[0]);
        mLuminosityIntensityView.setColor(Color.HSVToColor(mHueTemplate), false);
        mLuminosityIntensityView.setLuminosityIntensity(mHsvSelected[2], mHsvSelected[1]);
        mAlphaSelected = Color.alpha(color);
        mAlphaView.setAlpha(mAlphaEnabled ? mAlphaSelected : 255);
        mColorView.setColor(color);
    }

    /**
     * HEXテキストが変更されたときの処理。
     */
    @Override
    public void afterTextChanged(Editable s) {
        if(mWatch) {
            try {
                int color = Integer.parseInt(s.toString(), 16);
                mTextView.setTextColor(mTextColors);
                runColor(color);
            }catch (NumberFormatException exception) {
                mTextView.setTextColor(Color.RED);
            }
        }else{
            mWatch = true;
        }
    }

    /**
     * 色選択リスナーを設定します。
     */
    public void setColorSelectionListener(ColorSelectionListener listener){
        mColorSelectionListener = listener;
    }

    /**
     * アルファ選択の有効/無効を設定します。
     */
    public void setAlphaEnabled(boolean alphaEnabled){
        mAlphaEnabled = alphaEnabled;
        mAlphaView.setVisibility(alphaEnabled ? View.VISIBLE : View.GONE);
        mAlphaView.setAlpha(255);
    }

    /**
     * 色選択リスナーに色を通知します。
     */
    private void notifyColorSelector(int color){
        if(mColorSelectionListener != null)
            mColorSelectionListener.onColorSelected(color);
    }
}
