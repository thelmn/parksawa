package teamenum.parksawa.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.view_tag.view.*
import teamenum.parksawa.R

class TagsAdapter(private val items: List<String>, private val c: Context, private val listener: OnTagsListener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var selectedIndex = 0


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(c)
        return TagHolder(inflater.inflate(R.layout.view_tag, parent, false))
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is TagHolder) {
            holder.tag?.text = item
            holder.tag?.setOnClickListener { _ ->
                run {
                    notifyItemChanged(selectedIndex)
                    selectedIndex = holder.adapterPosition
                    notifyItemChanged(selectedIndex)
                    listener.onTagClick(item, holder.adapterPosition)
                }
            }
            holder.tag?.isSelected = selectedIndex == holder.adapterPosition
        }
    }

    class TagHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tag: TextView? = view.buttonTag
    }

    interface OnTagsListener {
        fun onTagClick(item: String, position: Int)
    }

}