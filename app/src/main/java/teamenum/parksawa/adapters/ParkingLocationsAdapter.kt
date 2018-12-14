package teamenum.parksawa.adapters

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.view_parking.view.*
import kotlinx.android.synthetic.main.view_search_or_change.view.*
import teamenum.parksawa.R
import teamenum.parksawa.data.ListItem
import teamenum.parksawa.data.Parking
import teamenum.parksawa.data.ViewType

class ParkingLocationsAdapter(override val items: ArrayList<ListItem>, private val c: Context, private val listener: OnLocationsListener) :
        ListItemAdapter(items) {

    // View Types: SEARCH_HERE, CHANGE_SEARCH, PARKING

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(c)
        return when(viewType) {
            ViewType.SEARCH_HERE, ViewType.CHANGE_SEARCH ->
                SearchOrChangeHolder(inflater.inflate(R.layout.view_search_or_change, parent, false))
            ViewType.PARKING -> LocationHolder(inflater.inflate(R.layout.view_parking, parent, false))
            else -> BlankHolder(inflater.inflate(R.layout.blank, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SearchOrChangeHolder -> {
                val state = items[position].VIEW_TYPE
                holder.text?.text = when (state) {
                    ViewType.SEARCH_HERE -> c.getString(R.string.search_near_this_location)
                    ViewType.CHANGE_SEARCH -> c.getString(R.string.pick_a_different_location)
                    else -> "An error occurred"
                }
                holder.clickable?.setOnClickListener { listener.onSearchOrChange() }
            }
            is LocationHolder -> {
                val item = items[position] as? Parking
                if (item != null) {
                    holder.clickable?.setOnClickListener { _ -> listener.onLocationClick(item.name, item.id) }
                    holder.textName?.text = item.name
                }
            }
        }
    }

    fun setSearchOrChange(item: ListItem?) {
        if (item != null) {
            if (items[0].VIEW_TYPE in arrayOf(ViewType.SEARCH_HERE, ViewType.CHANGE_SEARCH)) {
                items[0] = item
                notifyItemChanged(0)
            } else {
                items.add(0, item)
                notifyItemInserted(0)
            }
        } else {
            if (items[0].VIEW_TYPE in arrayOf(ViewType.SEARCH_HERE, ViewType.CHANGE_SEARCH)) {
                items.removeAt(0)
                notifyItemRemoved(0)
            }
        }
    }

    class SearchOrChangeHolder(view: View): RecyclerView.ViewHolder(view) {
        val clickable: ConstraintLayout? = view.clickable
        val text: TextView? = view.text
    }

    class LocationHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clickable: ConstraintLayout? = view.clickableParking
        val textName: TextView? = view.textName
    }

    class BlankHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnLocationsListener {
        fun onSearchOrChange()
        fun onLocationClick(item: String, id: String)
    }

}