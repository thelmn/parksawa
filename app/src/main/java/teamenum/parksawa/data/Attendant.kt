package teamenum.parksawa.data

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Attendant(@Exclude var id: String = "",
                     var username: String = "",
                     var email: String = "",
                     var profile: String = "",
                     var status: String = "") : ListItem {
    override val VIEW_TYPE: Int = ViewType.ATTENDANT
}