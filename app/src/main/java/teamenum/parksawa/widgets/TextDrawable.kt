package teamenum.parksawa.widgets

import android.graphics.*
import android.graphics.drawable.Drawable

class TextDrawable(private val text: String, private val textSize: Float) : Drawable() {

    private val paint: Paint = Paint()

    init {
        paint.color = Color.BLACK
        paint.textSize = textSize
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.LEFT
    }

    override fun draw(canvas: Canvas?) {
        canvas?.drawText(text, 0f, 14f, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }
}