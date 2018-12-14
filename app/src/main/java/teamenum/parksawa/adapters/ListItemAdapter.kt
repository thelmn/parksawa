package teamenum.parksawa.adapters

import android.support.v7.widget.RecyclerView
import teamenum.parksawa.data.ListItem

abstract class ListItemAdapter(open val items: ArrayList<ListItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].VIEW_TYPE
    }

    fun insert(index: Int = items.size, item: ListItem) {
        items.add(index, item)
    }
}