package teamenum.parksawa.data

import android.view.ViewGroup
import teamenum.parksawa.adapters.ParkingLocationsAdapter

data class Parking(
        val id: Long,
        val name: String,
        var latitude: Long = 0,
        var longitude: Long = 0,
        var height: Int = ViewGroup.LayoutParams.WRAP_CONTENT
        ) : ListItem {
        override val VIEW_TYPE = ParkingLocationsAdapter.TYPE_PARKING
}