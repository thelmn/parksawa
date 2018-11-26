package teamenum.parksawa.widgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup

class TransparentTouchView : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var viewBeneath: ViewGroup? = null
        get() = if (transparent) field else null
    var transparent = false

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (viewBeneath == null) {
            Log.d("TransparentTouchView", "onInterceptTouchEvent: null view")
        }
        Log.d("TransparentTouchView", "onInterceptTouchEvent: transparent: $transparent")
        return viewBeneath?.onInterceptTouchEvent(ev) ?: super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (viewBeneath == null) {
            Log.d("TransparentTouchView", "onTouchEvent: null view")
        }
        Log.d("TransparentTouchView", "onTouchEvent: transparent: $transparent")
        return viewBeneath?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return if (transparent) false
        else super.performClick()
    }
}