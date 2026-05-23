package com.arata.anim

import android.animation.Animator
import android.animation.AnimatorSet
import android.view.View
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.setting.AllSettings

/**
 * アニメーションの再生を管理するクラス
 * ビルダーパターンで設定をチェーンし、最後にstart()で再生する
 */
class AnimPlayer {
    private var mAnimatorSet: AnimatorSet = AnimatorSet()
    private var mAnimators: MutableList<Animator> = ArrayList()
    private var mOnStartCallback: AnimCallback? = null
    private var mOnEndCallback: AnimCallback? = null
    private var mDuration: Long? = null
    private var mDelay: Long? = null

    /**
     * アニメーターリストをクリアする
     */
    fun clearEntries() {
        mAnimators.clear()
    }

    /**
     * アニメーションエントリを追加する
     * @param entry 対象Viewとアニメーション種別を含むエントリ
     * @return 自身のインスタンス（チェーン用）
     */
    fun apply(entry: Entry): AnimPlayer {
        mAnimators.addAll(entry.animations.animator.getAnimators(entry.target))
        return this
    }

    /**
     * アニメーションの再生時間を設定する
     * @param long 再生時間（ミリ秒）
     * @return 自身のインスタンス（チェーン用）
     */
    fun duration(long: Long): AnimPlayer {
        mDuration = long
        return this
    }

    /**
     * アニメーション開始前の遅延を設定する
     * @param long 遅延時間（ミリ秒）
     * @return 自身のインスタンス（チェーン用）
     */
    fun delay(long: Long): AnimPlayer {
        mDelay = long
        return this
    }

    /**
     * アニメーション開始時のコールバックを設定する
     * @param callback 開始時に呼ばれるコールバック
     * @return 自身のインスタンス（チェーン用）
     */
    fun setOnStart(callback: AnimCallback): AnimPlayer {
        mOnStartCallback = callback
        return this
    }

    /**
     * アニメーション終了時のコールバックを設定する
     * @param callback 終了時に呼ばれるコールバック
     * @return 自身のインスタンス（チェーン用）
     */
    fun setOnEnd(callback: AnimCallback): AnimPlayer {
        mOnEndCallback = callback
        return this
    }

    /**
     * アニメーションを再生する
     * 既に再生中の場合は停止してから再開する
     */
    fun start() {
        if (mAnimatorSet.isStarted || mAnimatorSet.isRunning) {
            stop()
        }

        mAnimatorSet.apply {
            duration = mDuration ?: AllSettings.animationSpeed.getValue().toLong()
            startDelay = mDelay ?: 0

            removeAllListeners()
            addListener(object : Animator.AnimatorListener {
                /** アニメーション開始時にコールバックを呼び出す */
                override fun onAnimationStart(animation: Animator) {
                    mOnStartCallback?.call()
                }

                /** アニメーション終了時にコールバックを呼び出し、状態をクリアする */
                override fun onAnimationEnd(animation: Animator) {
                    mOnEndCallback?.call()
                    clearState()
                }

                /** アニメーションキャンセル時に状態をクリアする */
                override fun onAnimationCancel(animation: Animator) {
                    clearState()
                }

                /** アニメーション繰り返し時に呼ばれる（何もしない） */
                override fun onAnimationRepeat(animation: Animator) {
                }
            })
            playTogether(mAnimators)
            start()
        }
    }

    /**
     * アニメーションを停止する
     * 再生中の場合のみキャンセルし、状態をクリアする
     */
    fun stop() {
        if (mAnimatorSet.isRunning) {
            mAnimatorSet.cancel()
            clearState()
        }
    }

    /**
     * 内部状態をクリアする
     * 新しいAnimatorSetを割り当てて再利用できるようにする
     */
    private fun clearState() {
        mAnimatorSet = AnimatorSet()
    }

    companion object {
        /**
         * AnimPlayerのインスタンスを生成する
         * @return 新しいAnimPlayerインスタンス
         */
        fun play(): AnimPlayer {
            return AnimPlayer()
        }
    }

    /**
     * アニメーション対象と種別を保持するデータクラス
     * @property target アニメーションを適用するView
     * @property animations 適用するアニメーション種別
     */
    data class Entry(val target: View, val animations: Animations)
}
