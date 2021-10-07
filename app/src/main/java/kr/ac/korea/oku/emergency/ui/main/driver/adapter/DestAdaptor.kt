package kr.ac.korea.oku.emergency.ui.main.driver.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.model.Destination
import kr.ac.korea.oku.emergency.databinding.ItemDestMapBinding

class DestAdaptor() : RecyclerView.Adapter<DestAdaptor.DestViewHolder>() {
    var items : MutableList<Destination> = mutableListOf()
    override fun getItemViewType(position: Int): Int = R.layout.item_dest_map

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: DestAdaptor.DestViewHolder, position: Int) {
        items.getOrNull(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestAdaptor.DestViewHolder {
        return LayoutInflater.from(parent.context).run {
            DestViewHolder(
                ItemDestMapBinding.inflate(this,parent,false)
            )
        }
    }

    fun getItem(position: Int): Destination? = items.getOrNull(position)

    fun updateData(destList : List<Destination>) {
        items.clear()
        items.addAll(destList)
        notifyDataSetChanged()
    }

    inner class DestViewHolder(
        private val binding: ItemDestMapBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        var tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
        init {

        }
        fun bind(destination: Destination){
            tvTitle.text = destination.name
        }
    }
}

