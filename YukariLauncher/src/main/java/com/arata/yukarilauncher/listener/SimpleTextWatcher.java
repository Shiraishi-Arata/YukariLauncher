package com.arata.yukarilauncher.listener;

import android.text.TextWatcher;

/**
 * {@link TextWatcher} の実装のほとんどは afterTextChanged メソッドのみを実装する
 * このクラスは他のメソッドにデフォルト実装を提供する
 */
public interface SimpleTextWatcher extends TextWatcher {
    @Override
    default void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    default void onTextChanged(CharSequence s, int start, int before, int count) {}
}
