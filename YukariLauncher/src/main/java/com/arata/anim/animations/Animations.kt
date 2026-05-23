package com.arata.anim.animations

import com.arata.anim.animations.bounce.BounceEnlargeAnimator
import com.arata.anim.animations.bounce.BounceInDownAnimator
import com.arata.anim.animations.bounce.BounceInLeftAnimator
import com.arata.anim.animations.bounce.BounceInRightAnimator
import com.arata.anim.animations.bounce.BounceInUpAnimator
import com.arata.anim.animations.bounce.BounceShrinkAnimator
import com.arata.anim.animations.fade.FadeInAnimator
import com.arata.anim.animations.fade.FadeInDownAnimator
import com.arata.anim.animations.fade.FadeInLeftAnimator
import com.arata.anim.animations.fade.FadeInRightAnimator
import com.arata.anim.animations.fade.FadeInUpAnimator
import com.arata.anim.animations.fade.FadeOutAnimator
import com.arata.anim.animations.fade.FadeOutDownAnimator
import com.arata.anim.animations.fade.FadeOutLeftAnimator
import com.arata.anim.animations.fade.FadeOutRightAnimator
import com.arata.anim.animations.fade.FadeOutUpAnimator
import com.arata.anim.animations.other.PulseAnimator
import com.arata.anim.animations.other.ShakeAnimator
import com.arata.anim.animations.other.WobbleAnimator
import com.arata.anim.animations.slide.SlideInDownAnimator
import com.arata.anim.animations.slide.SlideInLeftAnimator
import com.arata.anim.animations.slide.SlideInRightAnimator
import com.arata.anim.animations.slide.SlideInUpAnimator
import com.arata.anim.animations.slide.SlideOutDownAnimator
import com.arata.anim.animations.slide.SlideOutLeftAnimator
import com.arata.anim.animations.slide.SlideOutRightAnimator
import com.arata.anim.animations.slide.SlideOutUpAnimator

/**
 * 利用可能なアニメーション種別を定義する列挙型
 * 各エントリは対応するBaseAnimator実装を持つ
 */
enum class Animations(val animator: BaseAnimator) {
    /** バウンス：上から落下 */
    BounceInDown(BounceInDownAnimator()),
    /** バウンス：左から出現 */
    BounceInLeft(BounceInLeftAnimator()),
    /** バウンス：右から出現 */
    BounceInRight(BounceInRightAnimator()),
    /** バウンス：下から上昇 */
    BounceInUp(BounceInUpAnimator()),
    /** バウンス：拡大表示 */
    BounceEnlarge(BounceEnlargeAnimator()),
    /** バウンス：縮小消去 */
    BounceShrink(BounceShrinkAnimator()),

    /** フェードイン */
    FadeIn(FadeInAnimator()),
    /** フェードイン：左から */
    FadeInLeft(FadeInLeftAnimator()),
    /** フェードイン：右から */
    FadeInRight(FadeInRightAnimator()),
    /** フェードイン：上から */
    FadeInUp(FadeInUpAnimator()),
    /** フェードイン：下から */
    FadeInDown(FadeInDownAnimator()),

    /** フェードアウト */
    FadeOut(FadeOutAnimator()),
    /** フェードアウト：左へ */
    FadeOutLeft(FadeOutLeftAnimator()),
    /** フェードアウト：右へ */
    FadeOutRight(FadeOutRightAnimator()),
    /** フェードアウト：上へ */
    FadeOutUp(FadeOutUpAnimator()),
    /** フェードアウト：下へ */
    FadeOutDown(FadeOutDownAnimator()),

    /** スライドイン：左から */
    SlideInLeft(SlideInLeftAnimator()),
    /** スライドイン：右から */
    SlideInRight(SlideInRightAnimator()),
    /** スライドイン：上から */
    SlideInUp(SlideInUpAnimator()),
    /** スライドイン：下から */
    SlideInDown(SlideInDownAnimator()),

    /** スライドアウト：左へ */
    SlideOutLeft(SlideOutLeftAnimator()),
    /** スライドアウト：右へ */
    SlideOutRight(SlideOutRightAnimator()),
    /** スライドアウト：上へ */
    SlideOutUp(SlideOutUpAnimator()),
    /** スライドアウト：下へ */
    SlideOutDown(SlideOutDownAnimator()),

    /** その他：パルス */
    Pulse(PulseAnimator()),
    /** その他：ぐらつき */
    Wobble(WobbleAnimator()),
    /** その他：振動 */
    Shake(ShakeAnimator())
}
