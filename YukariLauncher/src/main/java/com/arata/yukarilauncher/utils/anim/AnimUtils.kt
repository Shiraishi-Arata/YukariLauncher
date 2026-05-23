package com.arata.yukarilauncher.utils.anim

import android.view.View
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.utils.anim.ViewAnimUtils.Companion.setViewAnim

class AnimUtils {
    companion object {
        /**
         * デフォルトの持続時間（300ms）でビューの表示・非表示アニメーションを設定する
         */
        @JvmStatic
        fun setVisibilityAnim(view: View, shouldShow: Boolean) {
            setVisibilityAnim(view, shouldShow, 300, null)
        }

        /**
         * アニメーションリスナー付きでビューの表示・非表示アニメーションを設定する
         */
        @JvmStatic
        fun setVisibilityAnim(view: View, shouldShow: Boolean, listener: AnimationListener?) {
            setVisibilityAnim(view, shouldShow, 300, listener)
        }

        /**
         * 持続時間を指定してビューの表示・非表示アニメーションを設定する
         */
        @JvmStatic
        fun setVisibilityAnim(view: View, shouldShow: Boolean, duration: Int) {
            setVisibilityAnim(view, shouldShow, duration, null)
        }

        /**
         * 持続時間とリスナーを指定してビューの表示・非表示アニメーションを設定する
         */
        @JvmStatic
        fun setVisibilityAnim(
            view: View,
            shouldShow: Boolean,
            duration: Int,
            listener: AnimationListener?
        ) {
            setVisibilityAnim(view, 0, shouldShow, duration, listener)
        }

        /**
         * フェードイン・フェードアウトアニメーションでビューの表示・非表示を切り替える
         */
        @JvmStatic
        fun playVisibilityAnim(view: View, visible: Boolean) {
            val targetVisibility = if (visible) View.VISIBLE else View.GONE
            if (view.visibility == targetVisibility) return

            setViewAnim(view, if (visible) Animations.FadeIn else Animations.FadeOut,
                (AllSettings.animationSpeed.getValue() * 0.7).toLong(),
                { view.visibility = View.VISIBLE },
                { view.visibility = if (visible) View.VISIBLE else View.GONE })
        }

        /**
         * ビューの表示・非表示をアニメーションで切り替える（詳細版）
         * @param view 操作対象のビュー
         * @param startDelay 開始前の遅延
         * @param shouldShow trueの場合は表示、falseの場合は非表示
         * @param duration アニメーションの持続時間
         * @param listener アニメーションの開始時と終了時のコールバックリスナー
         */
        @JvmStatic
        fun setVisibilityAnim(
            view: View,
            startDelay: Int,
            shouldShow: Boolean,
            duration: Int,
            listener: AnimationListener?
        ) {
            listener?.onStart()

            if (shouldShow && view.visibility != View.VISIBLE) {
                fadeAnim(view, startDelay.toLong(), 0f, 1f, duration) {
                    view.visibility = View.VISIBLE
                    listener?.onEnd()
                }
            } else if (!shouldShow && view.visibility != View.GONE) {
                fadeAnim(view, startDelay.toLong(), view.alpha, 0f, duration) {
                    view.visibility = View.GONE
                    listener?.onEnd()
                }
            }
        }

        /**
         * フェードイン・フェードアウトアニメーションを実行する
         * @param view 操作対象のビュー
         * @param startDelay 開始前の遅延
         * @param begin 開始時の透明度（Alpha）
         * @param end 終了時の透明度（Alpha）
         * @param duration アニメーションの持続時間
         * @param endAction アニメーション終了時に実行するタスク
         */
        @JvmStatic
        fun fadeAnim(
            view: View,
            startDelay: Long,
            begin: Float,
            end: Float,
            duration: Int,
            endAction: Runnable?
        ) {
            if ((view.visibility != View.VISIBLE && end == 0f) || (view.visibility == View.VISIBLE && end == 1f)) {
                endAction?.let { r -> Task.runTask { r.run() }.execute() }
                return
            }
            view.visibility = View.VISIBLE
            view.alpha = begin
            view.animate()
                .alpha(end)
                .setStartDelay(startDelay)
                .setDuration(duration.toLong())
                .withEndAction(endAction)
        }
    }

    /**
     * アニメーションの開始と終了を通知するリスナーインターフェース
     */
    interface AnimationListener {
        fun onStart()
        fun onEnd()
    }
}
