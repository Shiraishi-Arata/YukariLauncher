package net.kdt.pojavlaunch.progresskeeper;

/**
 * 進行状況の状態を保持するデータクラス。
 */
public class ProgressState {
    /** 現在の進行度 */
    int progress;
    /** リソースID */
    int resid;
    /** 可変長引数 */
    Object[] varArg;
}
