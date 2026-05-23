package net.kdt.pojavlaunch.customcontrols.keyboard;

/** 必要なブリッジを介して文字を送信するためのシンプルなインターフェース */
public interface CharacterSenderStrategy {
    /** 削除する文字があるときに呼び出されます。連続して複数回呼び出される可能性があります */
    void sendBackspace();

    /** エンターキーを送信するときに呼び出されます */
    void sendEnter();

    /** 送信する文字があるときに呼び出されます。連続して複数回呼び出される可能性があります */
    void sendChar(char character);

}
