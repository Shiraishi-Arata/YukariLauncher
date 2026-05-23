package net.kdt.pojavlaunch.customcontrols.buttons;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlPopup;

import java.util.ArrayList;



@SuppressLint("ViewConstructor")
public class ControlDrawer extends ControlButton {


    public final ArrayList<ControlSubButton> buttons;
    public final ControlDrawerData drawerData;
    public final ControlLayout parentLayout;
    public boolean areButtonsVisible;
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlDrawer(ControlLayout layout, ControlDrawerData drawerData) {
        super(layout, drawerData.properties);

        buttons = new ArrayList<>(drawerData.buttonProperties.size());
        this.parentLayout = layout;
        this.drawerData = drawerData;
        areButtonsVisible = layout.getModifiable();
    }
/**
 * 「add Button」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void addButton(ControlData properties){
        addButton(new ControlSubButton(parentLayout, properties, this));
    }
/**
 * 「add Button」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void addButton(ControlSubButton button){
        buttons.add(button);
        syncButtons();
        setControlButtonVisibility(button, areButtonsVisible);
    }
/**
 * 「ControlButtonVisibility」の値を設定します。
 */
    private void setControlButtonVisibility(ControlButton button, boolean isVisible){
        button.getControlView().setVisibility(isVisible ? VISIBLE : GONE);
    }
/**
 * 「switch Button Visibility」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void switchButtonVisibility(){
        areButtonsVisible = !areButtonsVisible;
        int visibility = areButtonsVisible ? VISIBLE : GONE;
        for(ControlButton button : buttons){
            button.getControlView().setVisibility(visibility);
        }
    }
/**
 * 「align Buttons」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

    //同期処理
    private void alignButtons(){
        if(buttons == null) return;
        if(drawerData.orientation == ControlDrawerData.Orientation.FREE) return;
        int margin = (int) ControlInterface.getMarginDistance();

        for(int i = 0; i < buttons.size(); ++i){
            switch (drawerData.orientation){
                case RIGHT:
                    buttons.get(i).setDynamicX(generateDynamicX(getX() + (drawerData.properties.getWidth() + margin)*(i+1) ));
                    buttons.get(i).setDynamicY(generateDynamicY(getY()));
                    break;

                case LEFT:
                    buttons.get(i).setDynamicX(generateDynamicX(getX() - (drawerData.properties.getWidth() + margin)*(i+1)));
                    buttons.get(i).setDynamicY(generateDynamicY(getY()));
                    break;

                case UP:
                    buttons.get(i).setDynamicY(generateDynamicY(getY() - (drawerData.properties.getHeight() + margin)*(i+1)));
                    buttons.get(i).setDynamicX(generateDynamicX(getX()));
                    break;

                case DOWN:
                    buttons.get(i).setDynamicY(generateDynamicY(getY() + (drawerData.properties.getHeight() + margin)*(i+1)));
                    buttons.get(i).setDynamicX(generateDynamicX(getX()));
                    break;
            }
            buttons.get(i).updateProperties();
        }
    }
/**
 * 「resize Buttons」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

    private void resizeButtons(){
        if (buttons == null || drawerData.orientation == ControlDrawerData.Orientation.FREE) return;
        for(ControlSubButton subButton : buttons){
            subButton.mProperties.setWidth(mProperties.getWidth());
            subButton.mProperties.setHeight(mProperties.getHeight());

            subButton.updateProperties();
        }
    }
/**
 * 「sync Buttons」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void syncButtons(){
        alignButtons();
        resizeButtons();
    }

    /**
     * パラメータとして渡されたボタンがこのドロワーに属するかどうかを確認します。
     *
     * @param button 検索するボタン
     * @return ボタンがドロワーのボタンリスト内にあるかどうか
     */
    public boolean containsChild(ControlInterface button){
        for(ControlButton childButton : buttons){
            if (childButton == button) return true;
        }
        return false;
    }
/**
 * 「pre Process Properties」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public ControlData preProcessProperties(ControlData properties, ControlLayout layout) {
        ControlData data = super.preProcessProperties(properties, layout);
        data.isHideable = true;
        return data;
    }
/**
 * 「Visible」の値を設定します。
 */
    @Override
    public void setVisible(boolean isVisible) {
        int visibility = isVisible ? VISIBLE : GONE;
        setVisibility(visibility);
        if(visibility == GONE || areButtonsVisible) {
            for(ControlSubButton button : buttons){
                button.getControlView().setVisibility(isVisible ? VISIBLE : (!mProperties.isHideable && getVisibility() == GONE) ? VISIBLE : View.GONE);
            }
        }
    }
/**
 * タッチイベントを処理します。
 * ユーザーからのタッチ入力を検出し、適切なアクションを実行します。
 */

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!getControlLayoutParent().getModifiable()){
            switch (event.getActionMasked()){
                case MotionEvent.ACTION_UP: // 1
                case MotionEvent.ACTION_POINTER_UP: // 6
                    switchButtonVisibility();
                    break;
            }
            return true;
        }

        return super.onTouchEvent(event);
    }
/**
 * 「X」の値を設定します。
 */

    @Override
    public void setX(float x) {
        super.setX(x);
        alignButtons();
    }
/**
 * 「Y」の値を設定します。
 */
    @Override
    public void setY(float y) {
        super.setY(y);
        alignButtons();
    }
/**
 * 「LayoutParams」の値を設定します。
 */
    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        syncButtons();
    }
/**
 * 「can Snap」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public boolean canSnap(ControlInterface button) {
        boolean result = super.canSnap(button);
        return result && !containsChild(button);
    }
/**
 * 「DrawerData」の値を取得します。
 */

    //ゲッター
    public ControlDrawerData getDrawerData() {
        return drawerData;
    }
/**
 * 「load Edit Values」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void loadEditValues(EditControlPopup editControlPopup) {
        editControlPopup.loadValues(drawerData);
    }
/**
 * 「clone Button」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void cloneButton() {
        ControlDrawerData cloneData = new ControlDrawerData(getDrawerData());
        cloneData.properties.dynamicX = "0.5 * ${screen_width}";
        cloneData.properties.dynamicY = "0.5 * ${screen_height}";
        ((ControlLayout) getParent()).addDrawer(cloneData);
    }
/**
 * 「remove Button」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void removeButton() {
        ControlLayout layout = getControlLayoutParent();
        for(ControlSubButton subButton : buttons){
            layout.removeView(subButton);
        }

        layout.getLayout().mDrawerDataList.remove(getDrawerData());
        layout.removeView(this);
    }

}
