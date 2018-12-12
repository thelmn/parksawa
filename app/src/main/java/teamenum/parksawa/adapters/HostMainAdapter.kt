package teamenum.parksawa.adapters

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.view_button.view.*
import kotlinx.android.synthetic.main.view_parking_attendant.view.*
import kotlinx.android.synthetic.main.view_parking_space.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import teamenum.parksawa.R
import teamenum.parksawa.data.*

class HostMainAdapter(private val items: ArrayList<ListItem>, private val c: Context, private val listener: OnMainHostListener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TAG_ADD_SPACE = "teamenum.parksawa.adapters.HostMainAdapter.TAG_ADD_SPACE"
        const val TAG_ADD_ATTENDANT = "teamenum.parksawa.adapters.HostMainAdapter.TAG_ADD_ATTENDANT"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(c)
        return when(viewType) {
            ViewType.HOST_ADD_BUTTON-> ButtonHolder(inflater.inflate(R.layout.view_button, parent, false))
            ViewType.TITLE-> TitleHolder(inflater.inflate(R.layout.view_title, parent, false))
            ViewType.PARKING-> ParkingSpaceHolder(inflater.inflate(R.layout.view_parking_space, parent, false))
            ViewType.ATTENDANT -> ParkingAttendantHolder(inflater.inflate(R.layout.view_parking_attendant, parent, false))
            else -> BlankHolder(inflater.inflate(R.layout.blank, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].VIEW_TYPE
    }

    override fun getItemCount(): Int {
        return items.count() // top view
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ButtonHolder -> {
                if (item is AddButton) {
                    holder.clickable?.setOnClickListener { listener.onButtonClick(item.tag) }
                    holder.text?.text =
                            if (item.tag == TAG_ADD_SPACE) "Add a Parking space"
                            else "Add a parking attendant"
                }
            }
            is TitleHolder -> {
                if (item is Title) {
                    holder.title?.text = item.title
                }
            }
            is ParkingSpaceHolder -> {
                if (item is Parking) {
                    holder.clickable?.setOnClickListener { listener.onSpaceClick(item.id) }
                    holder.name?.text = item.name
                    holder.usage?.text = "Usage: ${item.booked} of ${item.slotCount}"
                }
            }
            is ParkingAttendantHolder -> {
                if (item is Attendant) {
                    holder.clickable?.setOnClickListener { listener.onAttendantClick(item.id) }
                    holder.name?.text = item.username
                    holder.email?.text = item.email
                }
            }
        }
    }

    class ButtonHolder(view: View): RecyclerView.ViewHolder(view) {
        val clickable: ConstraintLayout? = view.clickable
        val text: TextView? = view.text
    }

    class TitleHolder(view: View): RecyclerView.ViewHolder(view) {
        val title: TextView? = view.title
    }

    class ParkingSpaceHolder(view: View): RecyclerView.ViewHolder(view) {
        val clickable: ConstraintLayout? = view.clickableParking
        val name: TextView? = view.textParkingName
        val usage: TextView? = view.textUsage
        val reservable: TextView? = view.textReservable
        val rating: TextView? = view.textRating
        val reviews: TextView? = view.textReviews
        val image: ImageView? = view.imageParking
        val price: TextView? = view.textPrice
    }

    class ParkingAttendantHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clickable: ConstraintLayout? = view.clickableAttendant
        val name: TextView? = view.textName
        val image: ImageView? = view.imageUser
        val email: TextView? = view.textEmail
        val status: TextView? = view.textStatus
    }

    class BlankHolder(view: View) : RecyclerView.ViewHolder(view)

    data class AddButton(val tag: String) : ListItem {
        override val VIEW_TYPE: Int = ViewType.HOST_ADD_BUTTON
    }

    interface OnMainHostListener {
        fun onButtonClick(tag: String)
        fun onSpaceClick(id: Long)
        fun onAttendantClick(id: Long)
    }

}