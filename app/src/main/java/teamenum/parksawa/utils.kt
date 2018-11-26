package teamenum.parksawa


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