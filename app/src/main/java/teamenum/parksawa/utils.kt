package teamenum.parksawa

import android.graphics.BitmapFactory
import android.widget.ImageView
import kotlin.reflect.KProperty


fun dpToPx(dp: Int) : Int {
    return (dp* screenScale() + 0.5f).toInt()
}

fun pxToDp(px: Int) : Int {
    return ((px - 0.5f)/ screenScale()).toInt()
}

fun screenHeightDp() : Int {
    return pxToDp(MyApplication.inst.resources.displayMetrics.heightPixels)
}

fun screenHeightPx() : Int {
    return MyApplication.inst.resources.displayMetrics.heightPixels
}

fun screenScale() : Float {
    return MyApplication.inst.resources.displayMetrics.density
}

fun largePic(imageView: ImageView, filePath: String) {
    val targetW = imageView.width
    val targetH = imageView.height

    val bitmapOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, this)
        val photoW = outWidth
        val photoH = outHeight

        val scaleFactor = Math.min(photoW/targetW, photoH/targetH)

        inJustDecodeBounds = false
        inSampleSize = scaleFactor
        inPurgeable = true
    }
    BitmapFactory.decodeFile(filePath, bitmapOptions)?.also { bitmap ->
        imageView.setImageBitmap(bitmap)
    }
}

// Lazy initialization through delegated properties
// or just use the builtin (v 1.3.11) synchronized lazy and stop being a schitt
class Once<T>(private val generator: () -> T) {
    private var value: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val localVal = value ?: generator()
        value = localVal
        return localVal
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}