package teamenum.parksawa.adapters

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.view_parking.view.*
import kotlinx.android.synthetic.main.view_top.view.*
import teamenum.parksawa.R
import teamenum.parksawa.data.Parking
import teamenum.parksawa.dpToPx
import teamenum.parksawa.screenHeightDp
import teamenum.parksawa.widgets.TransparentTouchView

class ParkingLocationsAdapter(private val items: List<Parking>, private val c: Context, private val listener: OnLocationsListener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var fullView = false
    var topViewHeight = dpToPx(screenHeightDp()/2)
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    var viewBeneath: ViewGroup? = null
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    private val TYPE_TOP_VIEW = 0
    private val TYPE_LOCATION_VIEW = 1

    init {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(c)
        return if (viewType == TYPE_TOP_VIEW) {
            TopViewHolder(inflater.inflate(R.layout.view_top, parent, false))
        } else {
            LocationHolder(inflater.inflate(R.layout.view_parking, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_TOP_VIEW else TYPE_LOCATION_VIEW
    }

    override fun getItemCount(): Int {
        return items.count()+1 // top view
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TopViewHolder -> {
                Log.d("ParkingLocationsAdapter", "onBindViewHolder: topview height: $topViewHeight, fullview: $fullView")
                holder.clickable?.layoutParams?.height = topViewHeight
                holder.clickable?.viewBeneath = viewBeneath
                holder.clickable?.transparent = fullView
                holder.clickable?.setOnClickListener(
                        if (fullView) null
                        else View.OnClickListener { _ -> listener.onTopViewClick() }
                )
            }
            is LocationHolder -> {
                val item = items[position-1] // top view
                holder.clickable?.layoutParams?.height = item.height
                holder.clickable?.setOnClickListener { _ -> listener.onLocationClick(item.name, position-1) }
            }
        }
    }

    class TopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clickable: TransparentTouchView? = view.clickableTop
    }
    class LocationHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clickable: ConstraintLayout? = view.clickableParking
    }

    interface OnLocationsListener {
        fun onTopViewClick()
        fun onLocationClick(item: String, position: Int)
    }

}