package kr.ac.korea.oku.emergency.ui.main.driver.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.databinding.ItemDestMapBinding

class DestAdaptor() : RecyclerView.Adapter<DestAdaptor.DestViewHolder>() {
    var items : MutableList<Dest> = mutableListOf()
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

    fun getItem(position: Int): Dest? = items.getOrNull(position)
    fun getItemRange(from: Int, to : Int ) : List<Dest> = items.subList(from, to)

    fun updateData(destList : List<Dest>) {
        items.clear()
        items.addAll(destList)
        notifyDataSetChanged()
        notifyItemRangeInserted(0,itemCount)
    }

    inner class DestViewHolder(
        private val binding: ItemDestMapBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        var tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
        var tvLocation = itemView.findViewById<TextView>(R.id.tvLocation)
        init {

        }
        @SuppressLint("SetTextI18n")
        fun bind(destination: Dest){
            tvTitle.text = destination.name
            tvLocation.text = "${destination.distance.toString()} meter"
        }
    }
}

