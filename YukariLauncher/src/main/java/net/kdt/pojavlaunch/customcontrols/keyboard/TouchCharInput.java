package net.kdt.pojavlaunch.customcontrols.keyboard;


import static android.content.Context.INPUT_METHOD_SERVICE;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arata.yukarilauncher.R;

/**
 * このクラスは、仮想キーボードを介してチャットで使用される文字を送信するためのものです
 */
public class TouchCharInput extends androidx.appcompat.widget.AppCompatEditText {
    public static final String TEXT_FILLER = "                              ";
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public TouchCharInput(@NonNull Context context) {
        this(context, null);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public TouchCharInput(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.editTextStyle);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public TouchCharInput(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }


    private boolean mIsDoingInternalChanges = false;
    private CharacterSenderStrategy mCharacterSender;

    /**
     * アプリ間を移動するとキーボードが無効になります。
     * そのため、オブジェクトを無効化します
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        disable();
    }

    /**
     * バックキーをインターセプトしてフォーカスを無効にします
     * アクティビティの残りの部分には影響しません。
     */
    @Override
    public boolean onKeyPreIme(final int keyCode, final KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            disable();
        }
        return super.onKeyPreIme(keyCode, event);
    }


    /**
     * 状態に応じてソフトキーボードのオン/オフを切り替えます
     */
    public void switchKeyboardState(){
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
        // Allow, regardless of whether or not a hardware keyboard is declared
        if(hasFocus()){
            clear();
            disable();
        }else{
            enable();
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
        }
    }


    /**
     * EditTextから残りの入力をクリアします
     * ゲーム内の入力には影響しません
     */
    public void clear(){
        mIsDoingInternalChanges = true;
        // Edit the Editable directly as it doesn't affect the state
        // of the TextView.
        Editable editable = getEditableText();
        editable.clear();
        // 点字スペース。キーボードの自動補完をトリガーしない
        editable.append(TEXT_FILLER);
        Selection.setSelection(editable, TEXT_FILLER.length());
        mIsDoingInternalChanges = false;
    }

    /**
     * 存在、フォーカス取得、テキスト入力の機能を回復します。
     */
    public void enable(){
        setEnabled(true);
        setFocusable(true);
        setVisibility(VISIBLE);
        requestFocus();
    }

    /**
     * 存在、フォーカス取得、テキスト入力の機能を失います。
     */
    public void disable(){
        clear();
        setVisibility(GONE);
        clearFocus();
        setEnabled(false);
        //setFocusable(false);
    }

    /**
     * Enterキーを送信します。
     */
    private void sendEnter(){
        mCharacterSender.sendEnter();
        clear();
    }

    /**
     * 使用する文字送信戦略を設定します。
     */
    public void setCharacterSender(CharacterSenderStrategy characterSender){
        mCharacterSender = characterSender;
    }

    /**
     * コンストラクタが呼び出されたときに実行する必要がある処理を行います。
     */
    private void setup(){
        // Using TextWatcher instead of overriding onTextChanged because some Huawei firmware
        // calls setText in constructor, causing havoc for our listener
        addTextChangedListener(new InputTextWatcher());
        setOnEditorActionListener((textView, i, keyEvent) -> {
            sendEnter();
            clear();
            disable();
            return false;
        });
        clear();
        disable();
    }
    private class InputTextWatcher implements android.text.TextWatcher {
/**
 * 「before Text Changed」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }
        /**
         * 新しい文字を取得してゲームに送信します。
         * 文字数が少ない場合は、いくつか削除します。
         * テキストは常にクリーンアップされます。
         */
        @Override
        public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
            if(mIsDoingInternalChanges) return;
            if(mCharacterSender != null){
                for(int i=0; i < lengthBefore; ++i){
                    mCharacterSender.sendBackspace();
                }
                for(int i=start, count = 0; count < lengthAfter; ++i){
                    mCharacterSender.sendChar(text.charAt(i));
                    ++count;
                }
            }
        }
/**
 * 「after Text Changed」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
        @Override
        public void afterTextChanged(Editable editable) {
            if(mIsDoingInternalChanges) return;
            // Moved from onTextChanged because "It is an error to attempt to make changes to s from this callback."
            // reference: https://developer.android.com/reference/android/text/TextWatcher#onTextChanged(java.lang.CharSequence,%20int,%20int,%20int)
            if(editable.length() < 1) clear();
        }
    }
}
