package net.kdt.pojavlaunch.customcontrols;

public class ControlJoystickData extends ControlData {

    /* ジョイスティックが前方に固定可能かどうか */
    public boolean forwardLock = false;
    /*
     * 指の追跡が絶対方式（タッチした位置にジョイスティックが移動）か
     * 相対方式（ジョイスティックは中央に留まる）かどうか
     */
    public boolean absolute = false;
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlJoystickData(){
        super();
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlJoystickData(ControlJoystickData properties) {
        super(properties);
        forwardLock = properties.forwardLock;
        absolute = properties.absolute;
    }
}
