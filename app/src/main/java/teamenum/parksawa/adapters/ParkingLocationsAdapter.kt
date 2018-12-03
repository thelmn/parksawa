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

class ParkingLocationsAdapter(private val items: ArrayList<ListItem>, private val c: Context, private val listener: OnLocationsListener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_SEARCH_HERE = 942
        const val TYPE_CHANGE_SEARCH = 347
        const val TYPE_PARKING = 839
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(c)
        return when(viewType) {
            TYPE_SEARCH_HERE, TYPE_CHANGE_SEARCH ->
                SearchOrChangeHolder(inflater.inflate(R.layout.view_search_or_change, parent, false))
            TYPE_PARKING -> LocationHolder(inflater.inflate(R.layout.view_parking, parent, false))
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
        when (holder) {
            is SearchOrChangeHolder -> {
                val state = items[position].VIEW_TYPE
                holder.text?.text = when (state) {
                    TYPE_SEARCH_HERE -> c.getString(R.string.search_near_this_location)
                    TYPE_CHANGE_SEARCH -> c.getString(R.string.pick_a_different_location)
                    else -> "An error occurred"
                }
                holder.clickable?.setOnClickListener { listener.onSearchOrChange() }
            }
            is LocationHolder -> {
                val item = items[position] as? Parking
                if (item != null) {
                    holder.clickable?.layoutParams?.height = item.height
                    holder.clickable?.setOnClickListener { _ -> listener.onLocationClick(item.name, position - 1) }
                }
            }
        }
    }

    fun setSearchOrChange(item: ListItem?) {
        if (item != null) {
            if (items[0].VIEW_TYPE in arrayOf(TYPE_SEARCH_HERE, TYPE_CHANGE_SEARCH)) {
                items[0] = item
                notifyItemChanged(0)
            } else {
                items.add(0, item)
                notifyItemInserted(0)
            }
        } else {
            if (items[0].VIEW_TYPE in arrayOf(TYPE_SEARCH_HERE, TYPE_CHANGE_SEARCH)) {
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
    }

    class BlankHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnLocationsListener {
        fun onSearchOrChange()
        fun onLocationClick(item: String, position: Int)
    }

}