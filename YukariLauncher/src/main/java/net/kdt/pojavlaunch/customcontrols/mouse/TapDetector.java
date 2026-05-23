package net.kdt.pojavlaunch.customcontrols.mouse;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

import android.view.MotionEvent;

import net.kdt.pojavlaunch.Tools;

/**
 * ポインターに関係なくXタップイベントをより正確に検出するためのクラス
 * 可能な限り最小限のイベントのみを使用します。
 * すべてのイベントが順番通りに届くとは限らないため
 */
public class TapDetector {

    public final static int DETECTION_METHOD_DOWN = 0x1;
    public final static int DETECTION_METHOD_UP = 0x2;
    public final static int DETECTION_METHOD_BOTH = 0x3; //現在未使用

    private final static int TAP_MIN_DELTA_MS = -1;
    private final static int TAP_MAX_DELTA_MS = 300;
    private final static int TAP_SLOP_SQUARE_PX = (int) Tools.dpToPx(2500);

    private final int mTapNumberToDetect;
    private int mCurrentTapNumber = 0;

    private final int mDetectionMethod;

    private long mLastEventTime = 0;
    private float mLastX = 9999;
    private float mLastY = 9999;

    /**
     * @param tapNumberToDetect onTouchEventがTrueを返す前に必要なタップ数
     * @param detectionMethod タッチ検出に使用する方法。上記のDETECTION_METHOD定数を参照。
     */
    public TapDetector(int tapNumberToDetect, int detectionMethod){
        this.mDetectionMethod = detectionMethod;
        //DETECTION_METHOD_BOTHではACTION_DOWNとACTION_UPの両方を期待
        this.mTapNumberToDetect = detectBothTouch() ? 2*tapNumberToDetect : tapNumberToDetect;
    }

    /**
     * タッチイベント発生時に呼び出す関数です。
     * @param e 検査するMotionEvent
     * @return ポインターでXタップが発生したかどうか
     */
    public boolean onTouchEvent(MotionEvent e){
        int eventAction = e.getActionMasked();
        int pointerIndex = -1;

        //前方参照するイベントを取得
        if(detectDownTouch()){
            if(eventAction == ACTION_DOWN) pointerIndex = 0;
            else if(eventAction == ACTION_POINTER_DOWN) pointerIndex = e.getActionIndex();
        }
        if(detectUpTouch()){
            if(eventAction == ACTION_UP) pointerIndex = 0;
            else if(eventAction == ACTION_POINTER_UP) pointerIndex = e.getActionIndex();
        }

        if(pointerIndex == -1) return false; // Useless event

        // 現在のイベント情報を保存
        float eventX = e.getX(pointerIndex);
        float eventY = e.getY(pointerIndex);
        long eventTime = e.getEventTime();

        // デルタ値を計算
        long deltaTime = eventTime - mLastEventTime;
        int deltaX = (int) mLastX - (int) eventX;
        int deltaY = (int) mLastY - (int) eventY;

        // 現在のイベント情報を保存 to persist on next event
        mLastEventTime = eventTime;
        mLastX = eventX;
        mLastY = eventY;

        // 十分な速度と精度があるか確認
        if(mCurrentTapNumber > 0){
            if  ((deltaTime < TAP_MIN_DELTA_MS || deltaTime > TAP_MAX_DELTA_MS) ||
                ((deltaX*deltaX + deltaY*deltaY) > TAP_SLOP_SQUARE_PX)) {
                if (mDetectionMethod == DETECTION_METHOD_BOTH && (eventAction == ACTION_UP || eventAction == ACTION_POINTER_UP)) {
                    // For the both method, the user is expected to start with a down action.
                    resetTapDetectionState();
                    return false;
                } else {
                    // We invalidate previous taps, not this one though
                    mCurrentTapNumber = 0;
                }
            }
        }

        // 有意義なタップが発生
        mCurrentTapNumber += 1;
        if(mCurrentTapNumber >= mTapNumberToDetect){
           resetTapDetectionState();
           return true;
        }

        // 十分なタップ数に達していない場合
        return false;
    }

    /**
     * ダブルタップの値をリセットします。
     */
   private void resetTapDetectionState(){
       mCurrentTapNumber = 0;
       mLastEventTime = 0;
       mLastX = 9999;
       mLastY = 9999;
   }
/**
 * 「detect Down Touch」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
   private boolean detectDownTouch(){
       return (mDetectionMethod & DETECTION_METHOD_DOWN) == DETECTION_METHOD_DOWN;
   }
/**
 * 「detect Up Touch」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
   private boolean detectUpTouch(){
       return (mDetectionMethod & DETECTION_METHOD_UP) == DETECTION_METHOD_UP;
   }
/**
 * 「detect Both Touch」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
   private boolean detectBothTouch(){
       return mDetectionMethod == DETECTION_METHOD_BOTH;
   }
}
