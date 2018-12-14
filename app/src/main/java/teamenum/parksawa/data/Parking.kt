package teamenum.parksawa.data

import ch.hsr.geohash.GeoHash
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Parking(
        var name: String = "",
        var owner: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var booked: Int = 0,
        var slotCount: Int = 0,
        var reservable: Boolean = true,
        var pricing: List<Int> = listOf(-1, -1, -1),
        var images: List<String> = listOf("")
) : ListItem {
    override val VIEW_TYPE = ViewType.PARKING
        @Exclude get
    var id = ""
        @Exclude get

    var geoHash = GeoHash.withBitPrecision(latitude, longitude, 64)
}