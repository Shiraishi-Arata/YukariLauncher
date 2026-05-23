package net.kdt.pojavlaunch.customcontrols.buttons;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.ViewGroup;

import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlPopup;

@SuppressLint("ViewConstructor")
public class ControlSubButton extends ControlButton {

    public final ControlDrawer parentDrawer;
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlSubButton(ControlLayout layout, ControlData properties, ControlDrawer parentDrawer) {
        super(layout, properties);
        this.parentDrawer = parentDrawer;
        filterProperties();
    }
/**
 * 「filter Properties」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void filterProperties(){
        if (parentDrawer != null && parentDrawer.drawerData.orientation != ControlDrawerData.Orientation.FREE) {
            mProperties.setHeight(parentDrawer.getProperties().getHeight());
            mProperties.setWidth(parentDrawer.getProperties().getWidth());
        }

        setProperties(mProperties, false);
    }
/**
 * 「Visible」の値を設定します。
 */
    @Override
    public void setVisible(boolean isVisible) {
        // STUB, visibility handled by the ControlDrawer
        //setVisibility(isVisible ? VISIBLE : (!mProperties.isHideable && parentDrawer.getVisibility() == GONE) ? VISIBLE : View.GONE);
    }
/**
 * 「on Grab State」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void onGrabState(boolean isGrabbing) {
        // STUB, visibility lifecycle handled by the ControlDrawer
    }
/**
 * 「LayoutParams」の値を設定します。
 */
    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if(parentDrawer != null && parentDrawer.drawerData.orientation != ControlDrawerData.Orientation.FREE){
            params.width = (int)parentDrawer.mProperties.getWidth();
            params.height = (int)parentDrawer.mProperties.getHeight();
        }
        super.setLayoutParams(params);
    }
/**
 * タッチイベントを処理します。
 * ユーザーからのタッチ入力を検出し、適切なアクションを実行します。
 */

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!getControlLayoutParent().getModifiable() || parentDrawer.drawerData.orientation == ControlDrawerData.Orientation.FREE){
            return super.onTouchEvent(event);
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            //mCanTriggerLongClick = true;
            onLongClick(this);
        }
        return true;
    }
/**
 * 「clone Button」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void cloneButton() {
        ControlData cloneData = new ControlData(getProperties());
        cloneData.dynamicX = "0.5 * ${screen_width}";
        cloneData.dynamicY = "0.5 * ${screen_height}";
        ((ControlLayout) getParent()).addSubButton(parentDrawer, cloneData);
    }
/**
 * 「remove Button」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void removeButton() {
        parentDrawer.drawerData.buttonProperties.remove(getProperties());
        parentDrawer.drawerData.buttonProperties.remove(getProperties());
        parentDrawer.buttons.remove(this);

        parentDrawer.syncButtons();

        super.removeButton();
    }
/**
 * 「snap And Align」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void snapAndAlign(float x, float y) {
        if(parentDrawer.drawerData.orientation == ControlDrawerData.Orientation.FREE)
            super.snapAndAlign(x, y);
        // Else the button is forced into place
    }
/**
 * 「load Edit Values」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void loadEditValues(EditControlPopup editControlPopup) {
        editControlPopup.loadSubButtonValues(getProperties(), parentDrawer.drawerData.orientation);
    }
}
