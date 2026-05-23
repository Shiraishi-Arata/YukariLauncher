package com.arata.yukarilauncher.feature.version;

/**
 * バージョンが存在しない場合にスローされる実行時例外
 */
public class NoVersionException extends RuntimeException {
    /**
     * 指定されたメッセージでNoVersionExceptionを構築する
     * @param message 例外の詳細メッセージ
     */
    public NoVersionException(String message) {
        super(message);
    }
}
