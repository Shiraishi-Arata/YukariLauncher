package net.kdt.pojavlaunch.customcontrols;

import static com.arata.yukarilauncher.context.ContextExecutor.getString;
import static net.kdt.pojavlaunch.LwjglGlfwKeycode.GLFW_KEY_UNKNOWN;

import android.content.Context;
import android.util.ArrayMap;

import androidx.annotation.Keep;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.utils.stringutils.StringUtils;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;
import net.kdt.pojavlaunch.utils.JSONUtils;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import org.lwjgl.glfw.CallbackBridge;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Keep
public class ControlData {

    public static final int SPECIALBTN_KEYBOARD = -1;
    public static final int SPECIALBTN_TOGGLECTRL = -2;
    public static final int SPECIALBTN_MOUSEPRI = -3;
    public static final int SPECIALBTN_MOUSESEC = -4;
    public static final int SPECIALBTN_VIRTUALMOUSE = -5;
    public static final int SPECIALBTN_MOUSEMID = -6;
    public static final int SPECIALBTN_SCROLLUP = -7;
    public static final int SPECIALBTN_SCROLLDOWN = -8;
    public static final int SPECIALBTN_MENU = -9;

    private static ControlData[] SPECIAL_BUTTONS;
    private static List<String> SPECIAL_BUTTON_NAME_ARRAY;
    private static WeakReference<ExpressionBuilder> builder = new WeakReference<>(null);
    private static WeakReference<ArrayMap<String, String>> conversionMap = new WeakReference<>(null);

    static {
        buildExpressionBuilder();
        buildConversionMap();
    }

    // 内部使用のみ
    public transient boolean isHideable;
    /**
     * 以下のフィールドは動的な位置データで、自動更新されます
     * X/Y座標。元の固定値方式とは異なります
     * そのため、小型デバイスで作成したコントロールを大型デバイスにインポートした場合や、
     * その逆の場合、自動位置調整は提供されません。
     */
    public String dynamicX, dynamicY;
    public boolean isToggle, passThruEnabled;
    public String name;
    public int[] keycodes;      //最大4つのキーを格納
    public float opacity;       //0から1の間のアルファ値
    public int bgColor;
    public int strokeColor;
    public float strokeWidth;     // 現在は%ではなくDp
    public float cornerRadius;  //0-100%
    public boolean isSwipeable;
    public boolean repeatedlyEnabled;
    public int repeatCps = 10;
    public int repeatLongPressDelayMs = 300;
    public boolean displayInGame;
    public boolean displayInMenu;
    private float width;         // 現在はPxではなくDp
    private float height;        // 現在はPxではなくDp
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData() {
        this(getString(R.string.controls_add_control_button));
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(String name) {
        this(name, new int[]{});
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(String name, int[] keycodes) {
        this(name, keycodes, Tools.currentDisplayMetrics.widthPixels / 2f, Tools.currentDisplayMetrics.heightPixels / 2f);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(String name, int[] keycodes, float x, float y) {
        this(name, keycodes, x, y, 50, 50);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(Context ctx, int resId, int[] keycodes, float x, float y, boolean isSquare) {
        this(ctx.getResources().getString(resId), keycodes, x, y, isSquare);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(String name, int[] keycodes, float x, float y, boolean isSquare) {
        this(name, keycodes, x, y, isSquare ? 50 : 80, isSquare ? 50 : 30);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(String name, int[] keycodes, float x, float y, float width, float height) {
        this(name, keycodes, Float.toString(x), Float.toString(y), width, height, false);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(String name, int[] keycodes, String dynamicX, String dynamicY) {
        this(name, keycodes, dynamicX, dynamicY, 50, 50, false);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(Context ctx, int resId, int[] keycodes, String dynamicX, String dynamicY, boolean isSquare) {
        this(ctx.getResources().getString(resId), keycodes, dynamicX, dynamicY, isSquare);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(String name, int[] keycodes, String dynamicX, String dynamicY, boolean isSquare) {
        this(name, keycodes, dynamicX, dynamicY, isSquare ? 50 : 80, isSquare ? 50 : 30, false);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(String name, int[] keycodes, String dynamicX, String dynamicY, float width, float height, boolean isToggle) {
        this(name, keycodes, dynamicX, dynamicY, width, height, isToggle, 1, 0x4D000000, 0xFFFFFFFF, 0, 0, true, true, false, false);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlData(String name, int[] keycodes, String dynamicX, String dynamicY, float width, float height, boolean isToggle, float opacity, int bgColor, int strokeColor, float strokeWidth, float cornerRadius, boolean displayInGame, boolean displayInMenu, boolean isSwipable, boolean mousePassthrough) {
        this.name = name;
        this.keycodes = inflateKeycodeArray(keycodes);
        this.dynamicX = dynamicX;
        this.dynamicY = dynamicY;
        this.width = width;
        this.height = height;
        this.isToggle = isToggle;
        this.opacity = opacity;
        this.bgColor = bgColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.cornerRadius = cornerRadius;
        this.displayInGame = displayInGame;
        this.displayInMenu = displayInMenu;
        this.isSwipeable = isSwipable;
        this.passThruEnabled = mousePassthrough;
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */

    // ディープコピーコンストラクタ
    public ControlData(ControlData controlData) {
        this(
                controlData.name,
                controlData.keycodes,
                controlData.dynamicX,
                controlData.dynamicY,
                controlData.width,
                controlData.height,
                controlData.isToggle,
                controlData.opacity,
                controlData.bgColor,
                controlData.strokeColor,
                controlData.strokeWidth,
                controlData.cornerRadius,
                controlData.displayInGame,
                controlData.displayInMenu,
                controlData.isSwipeable,
                controlData.passThruEnabled
        );
        this.repeatedlyEnabled = controlData.repeatedlyEnabled;
        this.repeatCps = controlData.repeatCps;
        this.repeatLongPressDelayMs = controlData.repeatLongPressDelayMs;
    }
/**
 * 「SpecialButtons」の値を取得します。
 */
    public static ControlData[] getSpecialButtons(Context context) {
        if (SPECIAL_BUTTONS == null) {
            SPECIAL_BUTTONS = new ControlData[]{
                    new ControlData(context.getString(R.string.keycode_special_keyboard), new int[]{SPECIALBTN_KEYBOARD}, "${margin} * 3 + ${width} * 2", "${margin}", false),
                    new ControlData("GUI", new int[]{SPECIALBTN_TOGGLECTRL}, "${margin}", "${bottom} - ${margin}"),
                    new ControlData(context.getString(R.string.keycode_special_pri), new int[]{SPECIALBTN_MOUSEPRI}, "${margin}", "${screen_height} - ${margin} * 3 - ${height} * 3"),
                    new ControlData(context.getString(R.string.keycode_special_sec), new int[]{SPECIALBTN_MOUSESEC}, "${margin} * 3 + ${width} * 2", "${screen_height} - ${margin} * 3 - ${height} * 3"),
                    new ControlData(context.getString(R.string.keycode_special_mouse), new int[]{SPECIALBTN_VIRTUALMOUSE}, "${right}", "${margin}", false),

                    new ControlData(context.getString(R.string.keycode_special_mid), new int[]{SPECIALBTN_MOUSEMID}, "${margin}", "${margin}"),
                    new ControlData(context.getString(R.string.keycode_special_scrollup), new int[]{SPECIALBTN_SCROLLUP}, "${margin}", "${margin}"),
                    new ControlData(context.getString(R.string.keycode_special_scrolldown), new int[]{SPECIALBTN_SCROLLDOWN}, "${margin}", "${margin}"),
                    new ControlData(context.getString(R.string.keycode_special_menu), new int[]{SPECIALBTN_MENU}, "${margin}", "${margin}")
            };
        }

        return SPECIAL_BUTTONS;
    }
/**
 * 「build Special Button Array」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public static List<String> buildSpecialButtonArray(Context context) {
        if (SPECIAL_BUTTON_NAME_ARRAY == null) {
            List<String> nameList = new ArrayList<>();
            for (ControlData btn : getSpecialButtons(context)) {
                nameList.add(StringUtils.insertSpace(context.getString(R.string.keycode_special), btn.name));
            }
            SPECIAL_BUTTON_NAME_ARRAY = nameList;
            Collections.reverse(SPECIAL_BUTTON_NAME_ARRAY);
        }

        return SPECIAL_BUTTON_NAME_ARRAY;
    }
/**
 * 「calculate」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    private static float calculate(String math) {
        setExpression(math);
        return (float) builder.get().build().evaluate();
    }
/**
 * 「inflate Keycode Array」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private static int[] inflateKeycodeArray(int[] keycodes) {
        int[] inflatedArray = new int[]{GLFW_KEY_UNKNOWN, GLFW_KEY_UNKNOWN, GLFW_KEY_UNKNOWN, GLFW_KEY_UNKNOWN};
        System.arraycopy(keycodes, 0, inflatedArray, 0, keycodes.length);
        return inflatedArray;
    }

    /**
     * ビルダーを作成し、初回インフレーション時にすべてのビューで使用するために弱参照を保持します
     */
    private static void buildExpressionBuilder() {
        ExpressionBuilder expressionBuilder = new ExpressionBuilder("1 + 1")
                .function(new Function("dp", 1) {
/**
 * 「apply」メソッド。
 * このクラスに定義された機能メソッドです。
 */
                    @Override
                    public double apply(double... args) {
                        return Tools.pxToDp((float) args[0]);
                    }
                })
                .function(new Function("px", 1) {
/**
 * 「apply」メソッド。
 * このクラスに定義された機能メソッドです。
 */
                    @Override
                    public double apply(double... args) {
                        return Tools.dpToPx((float) args[0]);
                    }
                });
        builder = new WeakReference<>(expressionBuilder);
    }

    /**
     * wrapper for the WeakReference to the expressionField.
     *
     * @param stringExpression 設定する式
     */
    private static void setExpression(String stringExpression) {
        if (builder.get() == null) buildExpressionBuilder();
        builder.get().expression(stringExpression);
    }

    /**
     * ControlData依存の値なしで共有変換マップを構築します
     * 使用する前にビュー依存の値を設定する必要があります。
     */
    private static void buildConversionMap() {
        // Values in the map below may be always changed
        ArrayMap<String, String> keyValueMap = new ArrayMap<>(10);
        keyValueMap.put("top", "0");
        keyValueMap.put("left", "0");
        keyValueMap.put("right", "DUMMY_RIGHT");
        keyValueMap.put("bottom", "DUMMY_BOTTOM");
        keyValueMap.put("width", "DUMMY_WIDTH");
        keyValueMap.put("height", "DUMMY_HEIGHT");
        keyValueMap.put("screen_width", "DUMMY_DATA");
        keyValueMap.put("screen_height", "DUMMY_DATA");
        keyValueMap.put("margin", Integer.toString((int) ControlInterface.getMarginDistance()));
        keyValueMap.put("preferred_scale", "DUMMY_DATA");

        conversionMap = new WeakReference<>(keyValueMap);
    }
/**
 * 「insert Dynamic Pos」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public float insertDynamicPos(String dynamicPos) {
        // Insert value to ${variable}
        String insertedPos = JSONUtils.insertSingleJSONValue(dynamicPos, fillConversionMap());

        // Calculate, because the dynamic position contains some math equations
        return calculate(insertedPos);
    }
/**
 * 「contains Keycode」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean containsKeycode(int keycodeToCheck) {
        for (int keycode : keycodes)
            if (keycodeToCheck == keycode)
                return true;

        return false;
    }
/**
 * 「Width」の値を取得します。
 */

    // ゲッター || セッター（使いやすさのための変換付き）
    public float getWidth() {
        return Tools.dpToPx(width);
    }
/**
 * 「Width」の値を設定します。
 */
    public void setWidth(float widthInPx) {
        width = Tools.pxToDp(widthInPx);
    }
/**
 * 「Height」の値を取得します。
 */
    public float getHeight() {
        return Tools.dpToPx(height);
    }
/**
 * 「Height」の値を設定します。
 */
    public void setHeight(float heightInPx) {
        height = Tools.pxToDp(heightInPx);
    }

    /**
     * conversionMapにControlData依存の値を設定します。
     * 返されたvalueMapはメモリに保持しないでください。
     *
     * @return 使用するvalueMap
     */
    private Map<String, String> fillConversionMap() {
        ArrayMap<String, String> valueMap = conversionMap.get();
        if (valueMap == null) {
            buildConversionMap();
            valueMap = conversionMap.get();
        }

        valueMap.put("right", Float.toString(CallbackBridge.physicalWidth - getWidth()));
        valueMap.put("bottom", Float.toString(CallbackBridge.physicalHeight - getHeight()));
        valueMap.put("width", Float.toString(getWidth()));
        valueMap.put("height", Float.toString(getHeight()));
        valueMap.put("screen_width", Integer.toString(CallbackBridge.physicalWidth));
        valueMap.put("screen_height", Integer.toString(CallbackBridge.physicalHeight));
        valueMap.put("preferred_scale", Float.toString(AllSettings.getButtonScale().getValue()));

        return valueMap;
    }

}
