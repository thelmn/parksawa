package teamenum.parksawa.data

data class Attendant(val id: Long,
                     val username: String,
                     val email: String,
                     var profile: String = "",
                     var status: String = "") : ListItem {
    override val VIEW_TYPE: Int = ViewType.ATTENDANT
}