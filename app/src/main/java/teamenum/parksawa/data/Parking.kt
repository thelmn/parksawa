package teamenum.parksawa.data

import android.view.ViewGroup

data class Parking(
        val id: Long,
        val name: String,
        var latitude: Long = 0,
        var longitude: Long = 0,
        var height: Int = ViewGroup.LayoutParams.WRAP_CONTENT
        )