package net.kdt.pojavlaunch.customcontrols.buttons;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.core.math.MathUtils;

import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.setting.AllSettings;

import net.kdt.pojavlaunch.GrabListener;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlPopup;

import org.lwjgl.glfw.CallbackBridge;

/**
 * カスタム動作をViewに注入するインターフェース。
 * 注入される動作のほとんどは編集動作であり、
 * キー送信はサブクラスで実装する必要があります。
 */
public interface ControlInterface extends View.OnLongClickListener, GrabListener {
/**
 * 「ControlView」の値を取得します。
 */
    View getControlView();
/**
 * 「Properties」の値を取得します。
 */

    ControlData getProperties();
/**
 * 「Properties」の値を設定します。
 */

    default void setProperties(ControlData properties) {
        setProperties(properties, true);
    }

    /**
     * CustomControlオブジェクトからボタンの存在を削除します
     * これには{getControlParent()}を使用する必要があります。
     */
    void removeButton();

    /**
     * ボタンのデータを複製し、複製したデータでビューを追加します
     * 実装はControlLayoutに依存します。
     */
    void cloneButton();
/**
 * 「Visible」の値を設定します。
 */

    default void setVisible(boolean isVisible) {
        if(getProperties().isHideable)
            getControlView().setVisibility(isVisible ? VISIBLE : GONE);
    }
/**
 * 「send Key Presses」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

    void sendKeyPresses(boolean isDown);

    /**
     * 値を読み込み、不要なフォームを非表示にします
     */
    void loadEditValues(EditControlPopup editControlPopup);
/**
 * 「on Grab State」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    default void onGrabState(boolean isGrabbing) {
        if (getControlLayoutParent() != null && getControlLayoutParent().getModifiable()) return; // Disable when edited
        setVisible(((getProperties().displayInGame && isGrabbing) || (getProperties().displayInMenu && !isGrabbing)) && getControlLayoutParent().areControlVisible());
    }
/**
 * 「ControlLayoutParent」の値を取得します。
 */

    default ControlLayout getControlLayoutParent() {
        return (ControlLayout) getControlView().getParent();
    }

    /**
     * ビュー作成時の変換手順を適用します
     */
    default ControlData preProcessProperties(ControlData properties, ControlLayout layout) {
        //サイズ
        properties.setWidth(properties.getWidth() / layout.getLayoutScale() * AllSettings.getButtonScale().getValue());
        properties.setHeight(properties.getHeight() / layout.getLayoutScale() * AllSettings.getButtonScale().getValue());

        //表示設定
        properties.isHideable = !properties.containsKeycode(ControlData.SPECIALBTN_TOGGLECTRL) && !properties.containsKeycode(ControlData.SPECIALBTN_VIRTUALMOUSE);

        return properties;
    }
/**
 * 「update Properties」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

    default void updateProperties() {
        setProperties(getProperties());
    }
/**
 * 「Properties」の値を設定します。
 */

    /* この関数はプロパティを保存するためにオーバーライドされるべきです */
    @CallSuper
    default void setProperties(ControlData properties, boolean changePos) {
        if (changePos) {
            getControlView().setX(properties.insertDynamicPos(getProperties().dynamicX));
            getControlView().setY(properties.insertDynamicPos(getProperties().dynamicY));
        }

        // レイアウトパラメータを再利用
        ViewGroup.LayoutParams params = getControlView().getLayoutParams();
        if (params == null)
            params = new FrameLayout.LayoutParams((int) properties.getWidth(), (int) properties.getHeight());
        params.width = (int) properties.getWidth();
        params.height = (int) properties.getHeight();
        getControlView().setLayoutParams(params);
    }

    /**
     * プロパティに従って背景を適用します
     */
    default void setBackground() {
        GradientDrawable gd = getControlView().getBackground() instanceof GradientDrawable
                ? (GradientDrawable) getControlView().getBackground()
                : new GradientDrawable();
        gd.setColor(getProperties().bgColor);
        gd.setStroke((int) Tools.dpToPx(getProperties().strokeWidth * (getControlLayoutParent().getLayoutScale()/100f)), getProperties().strokeColor);
        gd.setCornerRadius(computeCornerRadius(getProperties().cornerRadius));

        getControlView().setBackground(gd);
    }

    /**
     * X軸に動的方程式を適用します。
     *
     * @param dynamicX 位置計算のための方程式
     */
    default void setDynamicX(String dynamicX) {
        getProperties().dynamicX = dynamicX;
        getControlView().setX(getProperties().insertDynamicPos(dynamicX));
    }

    /**
     * Y軸に動的方程式を適用します。
     *
     * @param dynamicY 位置計算のための方程式
     */
    default void setDynamicY(String dynamicY) {
        getProperties().dynamicY = dynamicY;
        getControlView().setY(getProperties().insertDynamicPos(dynamicY));
    }

    /**
     * 絶対位置から動的方程式を生成します。デバイス間で適切にスケーリングするために使用されます
     *
     * @param x 水平軸上の絶対位置
     * @return 文字列としての方程式
     */
    default String generateDynamicX(float x) {
        if (x + (getProperties().getWidth() / 2f) > CallbackBridge.physicalWidth / 2f) {
            return (x + getProperties().getWidth()) / CallbackBridge.physicalWidth + " * ${screen_width} - ${width}";
        } else {
            return x / CallbackBridge.physicalWidth + " * ${screen_width}";
        }
    }

    /**
     * 絶対位置から動的方程式を生成します。デバイス間で適切にスケーリングするために使用されます
     *
     * @param y 垂直軸上の絶対位置
     * @return 文字列としての方程式
     */
    default String generateDynamicY(float y) {
        if (y + (getProperties().getHeight() / 2f) > CallbackBridge.physicalHeight / 2f) {
            return (y + getProperties().getHeight()) / CallbackBridge.physicalHeight + " * ${screen_height} - ${height}";
        } else {
            return y / CallbackBridge.physicalHeight + " * ${screen_height}";
        }
    }

    /**
     * 変更されたプロパティで座標を再生成して適用します
     */
    default void regenerateDynamicCoordinates() {
        getProperties().dynamicX = generateDynamicX(getControlView().getX());
        getProperties().dynamicY = generateDynamicY(getControlView().getY());
        updateProperties();
    }

    /**
     * ボタンの値を使用して方程式の事前変換を行います。
     * 変数を別のボタンで使用できるようにします。
     * <p>
     * 内部使用のみ。
     *
     * @param equation 動的位置を示す文字列
     * @param button   値を取得するボタン
     * @return 前処理済みの方程式（文字列）
     */
    default String applySize(String equation, ControlInterface button) {
        return equation
                .replace("${right}", "(${screen_width} - ${width})")
                .replace("${bottom}", "(${screen_height} - ${height})")
                .replace("${height}", "(px(" + Tools.pxToDp(button.getProperties().getHeight()) + ") /" + AllSettings.getButtonScale().getValue() + " * ${preferred_scale})")
                .replace("${width}", "(px(" + Tools.pxToDp(button.getProperties().getWidth()) + ") / " + AllSettings.getButtonScale().getValue() + " * ${preferred_scale})");
    }


    /**
     * 角丸のパーセンテージをpxの角丸に変換します
     */
    default float computeCornerRadius(float radiusInPercent) {
        float minSize = Math.min(getProperties().getWidth(), getProperties().getHeight());
        return (minSize / 2) * (radiusInPercent / 100);
    }

    /**
     * 一連のチェックを実行し、ControlButtonがスナップ可能かどうかを判定します。
     *
     * @param button チェックするボタン
     * @return whether or not the button
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean canSnap(ControlInterface button) {
        if (!AllSettings.getButtonSnapping().getValue()) return false;

        float MIN_DISTANCE = getSnapDistance();

        if (button == this) return false;
        if (button.getControlView().getVisibility() == GONE) return false;
        return !(net.kdt.pojavlaunch.utils.MathUtils.dist(
                button.getControlView().getX() + button.getControlView().getWidth() / 2f,
                button.getControlView().getY() + button.getControlView().getHeight() / 2f,
                getControlView().getX() + getControlView().getWidth() / 2f,
                getControlView().getY() + getControlView().getHeight() / 2f)
                > Math.max(button.getControlView().getWidth() / 2f + getControlView().getWidth() / 2f,
                button.getControlView().getHeight() / 2f + getControlView().getHeight() / 2f) + MIN_DISTANCE);
    }

    /**
     * 指定された座標で、スナップしてから隣接ボタンに位置合わせします。
     * 新しい位置は、スナップの有無に関わらず自動的にViewに適用されます。
     * <p>
     * 新しい位置は常に動的であり、以前の動的位置を置き換えます
     *
     * @param x X軸上の座標
     * @param y Y軸上の座標
     */
    default void snapAndAlign(float x, float y) {
        final float MIN_DISTANCE = getSnapDistance();
        String dynamicX = generateDynamicX(x);
        String dynamicY = generateDynamicY(y);

        getControlView().setX(x);
        getControlView().setY(y);

        for (ControlInterface button : ((ControlLayout) getControlView().getParent()).getButtonChildren()) {
            //ステップ1: 不要なボタンをフィルタリング
            if (!canSnap(button)) continue;

            //ステップ2: 座標を取得
            float button_top = button.getControlView().getY();
            float button_bottom = button_top + button.getControlView().getHeight();
            float button_left = button.getControlView().getX();
            float button_right = button_left + button.getControlView().getWidth();

            float top = getControlView().getY();
            float bottom = getControlView().getY() + getControlView().getHeight();
            float left = getControlView().getX();
            float right = getControlView().getX() + getControlView().getWidth();

            //ステップ3: 各軸で最も近いボタンにスナップ
            if (Math.abs(top - button_bottom) < MIN_DISTANCE) { // Bottom snap
                dynamicY = applySize(button.getProperties().dynamicY, button) + applySize(" + ${height}", button) + " + ${margin}";
            } else if (Math.abs(button_top - bottom) < MIN_DISTANCE) { //Top snap
                dynamicY = applySize(button.getProperties().dynamicY, button) + " - ${height} - ${margin}";
            }
            if (!dynamicY.equals(generateDynamicY(getControlView().getY()))) { //If we snapped
                if (Math.abs(button_left - left) < MIN_DISTANCE) { //Left align snap
                    dynamicX = applySize(button.getProperties().dynamicX, button);
                } else if (Math.abs(button_right - right) < MIN_DISTANCE) { //Right align snap
                    dynamicX = applySize(button.getProperties().dynamicX, button) + applySize(" + ${width}", button) + " - ${width}";
                }
            }

            if (Math.abs(button_left - right) < MIN_DISTANCE) { //Left snap
                dynamicX = applySize(button.getProperties().dynamicX, button) + " - ${width} - ${margin}";
            } else if (Math.abs(left - button_right) < MIN_DISTANCE) { //Right snap
                dynamicX = applySize(button.getProperties().dynamicX, button) + applySize(" + ${width}", button) + " + ${margin}";
            }
            if (!dynamicX.equals(generateDynamicX(getControlView().getX()))) { //If we snapped
                if (Math.abs(button_top - top) < MIN_DISTANCE) { //Top align snap
                    dynamicY = applySize(button.getProperties().dynamicY, button);
                } else if (Math.abs(button_bottom - bottom) < MIN_DISTANCE) { //Bottom align snap
                    dynamicY = applySize(button.getProperties().dynamicY, button) + applySize(" + ${height}", button) + " - ${height}";
                }
            }

        }

        setDynamicX(dynamicX);
        setDynamicY(dynamicY);
    }

    /**
     * 複数の注入を一度に行うラッパー
     */
    default void injectBehaviors() {
        injectProperties();
        injectTouchEventBehavior();
        injectLayoutParamBehavior();
        injectGrabListenerBehavior();
    }

    /**
     * グラブリスナーを注入し、ビューが削除されたら削除します
     */
    default void injectGrabListenerBehavior() {
        if (getControlView() == null) {
            Logging.e(ControlInterface.class.toString(), "Failed to inject grab listener behavior !");
            return;
        }


        getControlView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
/**
 * 「on View Attached To Window」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                CallbackBridge.addGrabListener(ControlInterface.this);
            }
/**
 * 「on View Detached From Window」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                getControlView().removeOnAttachStateChangeListener(this);
                CallbackBridge.removeGrabListener(ControlInterface.this);
            }
        });


    }
/**
 * 「inject Properties」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

    default void injectProperties() {
        getControlView().post(() -> getControlView().setTranslationZ(10));
    }

    /**
     * 編集コントロールを簡単にするために、ビューにタッチリスナーを注入します
     */
    default void injectTouchEventBehavior() {
        getControlView().setOnTouchListener(new View.OnTouchListener() {
            private boolean mCanTriggerLongClick = true;
            private float downX, downY;
            private float downRawX, downRawY;
/**
 * 「on Touch」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!getControlLayoutParent().getModifiable()) {
                    // Basically, editing behavior is forced while in game behavior is specific
                    view.onTouchEvent(event);
                    return true;
                }

                /* If the button can be modified/moved */
                //必要なときだけジェスチャー検出器をインスタンス化

                if (event.getActionMasked() == MotionEvent.ACTION_UP && mCanTriggerLongClick) {
                    //TODO change this.
                    onLongClick(view);
                }

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mCanTriggerLongClick = true;
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        downX = downRawX - view.getX();
                        downY = downRawY - view.getY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - downRawX) > 8 || Math.abs(event.getRawY() - downRawY) > 8)
                            mCanTriggerLongClick = false;
                        getControlLayoutParent().adaptPanelPosition();
                        snapAndAlign(
                                MathUtils.clamp(event.getRawX() - downX, 0, CallbackBridge.physicalWidth - view.getWidth()),
                                MathUtils.clamp(event.getRawY() - downY, 0, CallbackBridge.physicalHeight - view.getHeight())
                        );
                        break;
                }

                return true;
            }
        });
    }
/**
 * 「inject Layout Param Behavior」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

    default void injectLayoutParamBehavior() {
        getControlView().addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            getProperties().setWidth(right - left);
            getProperties().setHeight(bottom - top);
            setBackground();

            // Re-calculate position
            getControlView().setX(getControlView().getX());
            getControlView().setY(getControlView().getY());
        });
    }
/**
 * 「on Long Click」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    default boolean onLongClick(View v) {
        if (getControlLayoutParent().getModifiable()) {
            getControlLayoutParent().editControlButton(this);
            getControlLayoutParent().mActionRow.setFollowedButton(this);
        }

        return true;
    }
/**
 * 「SnapDistance」の値を取得します。
 */

    static float getSnapDistance() {
        return Tools.dpToPx(AllSettings.getButtonSnappingDistance().getValue());
    }
/**
 * 「MarginDistance」の値を取得します。
 */
    static float getMarginDistance() {
        return Tools.dpToPx(2);
    }
}
