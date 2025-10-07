package com.arata.yukarilauncher.ui.view

import android.animation.AnimatorInflater
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.utils.anim.ViewAnimUtils.Companion.setViewAnim

class AnimEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {
    init {
        stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.xml.anim_scale)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        post {
            pivotX = width / 2f
            pivotY = height / 2f
        }
    }

    override fun setError(error: CharSequence?) {
        super.setError(error)
        error?.let { setErrorAnim() }
    }

    override fun setError(error: CharSequence?, icon: Drawable?) {
        super.setError(error, icon)
        error?.let { setErrorAnim() }
    }

    private fun setErrorAnim() {
        setViewAnim(this, Animations.Shake)
    }
}
