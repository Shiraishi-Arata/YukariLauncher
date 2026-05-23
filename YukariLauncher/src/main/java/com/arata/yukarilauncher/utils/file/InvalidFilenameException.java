package com.arata.yukarilauncher.utils.file;

/**
 * ファイル名が無効であることを示す例外クラス
 * 不正な文字が含まれている場合や長さが制限を超えている場合にスローされる
 */
public class InvalidFilenameException extends RuntimeException {
    private final FilenameErrorType type;
    private String illegalCharacters = null;
    private int invalidLength = -1;

    /**
     * 不正な文字を含むファイル名の場合のコンストラクタ
     * @param message エラーメッセージ
     * @param illegalCharacters 含まれている不正な文字
     */
    public InvalidFilenameException(String message, String illegalCharacters) {
        super(message);
        this.type = FilenameErrorType.CONTAINS_ILLEGAL_CHARACTERS;
        this.illegalCharacters = illegalCharacters;
    }

    /**
     * 長さが無効なファイル名の場合のコンストラクタ
     * @param message エラーメッセージ
     * @param invalidLength 無効な長さ
     */
    public InvalidFilenameException(String message, int invalidLength) {
        super(message);
        this.type = FilenameErrorType.INVALID_LENGTH;
        this.invalidLength = invalidLength;
    }

    /**
     * 不正な文字が含まれているかどうかを判定する
     */
    public boolean containsIllegalCharacters() {
        return type == FilenameErrorType.CONTAINS_ILLEGAL_CHARACTERS;
    }

    /**
     * 不正な文字を取得する
     */
    public String getIllegalCharacters() {
        return illegalCharacters;
    }

    /**
     * 長さが無効かどうかを判定する
     */
    public boolean isInvalidLength() {
        return type == FilenameErrorType.INVALID_LENGTH;
    }

    /**
     * 無効な長さを取得する
     */
    public int getInvalidLength() {
        return invalidLength;
    }

    /**
     * ファイル名エラーの種類を表す内部列挙型
     */
    private enum FilenameErrorType {
        /** 不正な文字が含まれている */
        CONTAINS_ILLEGAL_CHARACTERS,
        /** 長さが制限を超えている */
        INVALID_LENGTH
    }
}
