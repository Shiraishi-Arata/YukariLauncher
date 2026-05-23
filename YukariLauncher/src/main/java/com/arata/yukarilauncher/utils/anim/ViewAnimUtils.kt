package com.arata.yukarilauncher.utils.anim

import android.view.View
import com.arata.anim.AnimCallback
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations

class ViewAnimUtils {
    companion object {
        /**
         * ビューにアニメーションを設定して再生する
         */
        @JvmStatic
        fun setViewAnim(view: View, animations: Animations) {
            getAnimPlayer(view, animations).start()
        }

        /**
         * ビューにアニメーションを設定し、再生時間を指定して再生する
         */
        @JvmStatic
        fun setViewAnim(view: View, animations: Animations, duration: Long) {
            getAnimPlayer(view, animations).duration(duration).start()
        }

        /**
         * ビューにアニメーションを設定し、開始時と終了時のコールバックを指定して再生する
         */
        @JvmStatic
        fun setViewAnim(view: View, animations: Animations, onStart: AnimCallback, onEnd: AnimCallback) {
            getAnimPlayer(view, animations).setOnStart(onStart).setOnEnd(onEnd).start()
        }

        /**
         * ビューにアニメーションを設定し、再生時間とコールバックを指定して再生する
         */
        @JvmStatic
        fun setViewAnim(view: View, animations: Animations, duration: Long, onStart: AnimCallback, onEnd: AnimCallback) {
            getAnimPlayer(view, animations).duration(duration).setOnStart(onStart).setOnEnd(onEnd).start()
        }

        /**
         * AnimPlayerインスタンスを取得する
         */
        private fun getAnimPlayer(view: View, animations: Animations) = AnimPlayer.play().apply(AnimPlayer.Entry(view, animations))
    }
}
