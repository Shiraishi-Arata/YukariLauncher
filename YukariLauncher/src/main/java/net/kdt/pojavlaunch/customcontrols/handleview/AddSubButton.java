package net.kdt.pojavlaunch.customcontrols.handleview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.ui.view.AnimButton;

import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;

@SuppressLint("AppCompatCustomView")
public class AddSubButton extends AnimButton implements ActionButtonInterface {
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public AddSubButton(Context context) {super(context); init();}
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public AddSubButton(Context context, @Nullable AttributeSet attrs) {super(context, attrs); init();}
/**
 * 「init」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    public void init() {
        setOnClickListener(this);
        setText(R.string.customctrl_addsubbutton);
    }

    private ControlInterface mCurrentlySelectedButton = null;
/**
 * 「should Be Visible」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public boolean shouldBeVisible() {
        return mCurrentlySelectedButton != null && mCurrentlySelectedButton instanceof ControlDrawer;
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
        if(mCurrentlySelectedButton instanceof ControlDrawer){
            ((ControlDrawer)mCurrentlySelectedButton).getControlLayoutParent().addSubButton(
                    (ControlDrawer)mCurrentlySelectedButton,
                    new ControlData(getContext().getString(R.string.controls_add_control_button))
            );
        }
    }


}
