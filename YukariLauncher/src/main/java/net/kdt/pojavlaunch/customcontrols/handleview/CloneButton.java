package net.kdt.pojavlaunch.customcontrols.handleview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.ui.view.AnimButton;

import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;

@SuppressLint("AppCompatCustomView")
public class CloneButton extends AnimButton implements ActionButtonInterface {
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public CloneButton(Context context) {super(context); init();}
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public CloneButton(Context context, @Nullable AttributeSet attrs) {super(context, attrs); init();}
/**
 * 「init」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    public void init() {
        setOnClickListener(this);
        setText(R.string.generic_clone);
    }

    private ControlInterface mCurrentlySelectedButton = null;
/**
 * 「should Be Visible」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public boolean shouldBeVisible() {
        return mCurrentlySelectedButton != null;
    }
/**
 * 「FollowedView」の値を設定します。
 */
    @Override
    public void setFollowedView(ControlInterface view) {
        mCurrentlySelectedButton = view;
    }
/**
 * 「on Click」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void onClick() {
        if(mCurrentlySelectedButton == null) return;

        mCurrentlySelectedButton.cloneButton();
        mCurrentlySelectedButton.getControlLayoutParent().removeEditWindow();
    }
}
