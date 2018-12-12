package teamenum.parksawa.data

data class Parking(
        val id: Long,
        val name: String,
        var latitude: Long = 0,
        var longitude: Long = 0,
        var booked: Int = 0,
        var slotCount: Int = 0
        ) : ListItem {
        override val VIEW_TYPE = ViewType.PARKING
}