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

enum class Animations(val animator: BaseAnimator) {
    //Bounce
    BounceInDown(BounceInDownAnimator()),
    BounceInLeft(BounceInLeftAnimator()),
    BounceInRight(BounceInRightAnimator()),
    BounceInUp(BounceInUpAnimator()),
    BounceEnlarge(BounceEnlargeAnimator()),
    BounceShrink(BounceShrinkAnimator()),

    //Fade in
    FadeIn(FadeInAnimator()),
    FadeInLeft(FadeInLeftAnimator()),
    FadeInRight(FadeInRightAnimator()),
    FadeInUp(FadeInUpAnimator()),
    FadeInDown(FadeInDownAnimator()),

    //Fade out
    FadeOut(FadeOutAnimator()),
    FadeOutLeft(FadeOutLeftAnimator()),
    FadeOutRight(FadeOutRightAnimator()),
    FadeOutUp(FadeOutUpAnimator()),
    FadeOutDown(FadeOutDownAnimator()),

    //Slide in
    SlideInLeft(SlideInLeftAnimator()),
    SlideInRight(SlideInRightAnimator()),
    SlideInUp(SlideInUpAnimator()),
    SlideInDown(SlideInDownAnimator()),

    //Slide out
    SlideOutLeft(SlideOutLeftAnimator()),
    SlideOutRight(SlideOutRightAnimator()),
    SlideOutUp(SlideOutUpAnimator()),
    SlideOutDown(SlideOutDownAnimator()),

    //Other
    Pulse(PulseAnimator()),
    Wobble(WobbleAnimator()),
    Shake(ShakeAnimator())
}