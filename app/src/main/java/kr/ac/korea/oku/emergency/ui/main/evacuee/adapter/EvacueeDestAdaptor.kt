package kr.ac.korea.oku.emergency.ui.main.evacuee.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.databinding.ItemDestMapBinding

class EvacueeDestAdaptor(private val onClicked : (dest:Dest)->Unit) : RecyclerView.Adapter<EvacueeDestAdaptor.DestViewHolder>() {
    var items : MutableList<Dest> = mutableListOf()
    override fun getItemViewType(position: Int): Int = R.layout.item_dest_map

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: EvacueeDestAdaptor.DestViewHolder, position: Int) {
        items.getOrNull(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvacueeDestAdaptor.DestViewHolder {
        return LayoutInflater.from(parent.context).run {
            DestViewHolder(
                onClicked,
                ItemDestMapBinding.inflate(this,parent,false)
            )
        }
    }

    fun getItem(position: Int): Dest? = items.getOrNull(position)
    fun getItemRange(from: Int, to : Int ) : List<Dest> = items.subList(from, to)

    fun dataChanged(){
        notifyDataSetChanged()
    }

    fun updateData(destList : List<Dest>) {
        items.clear()
        items.addAll(destList)
        notifyDataSetChanged()
        notifyItemRangeInserted(0,itemCount)
    }

    inner class DestViewHolder(
        private val onClicked : (dest:Dest)->Unit,
        private val binding: ItemDestMapBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        var dest : Dest? = null
        var tvTitle: TextView = itemView.findViewById<TextView>(R.id.tvTitle)
        var tvLocation: TextView = itemView.findViewById<TextView>(R.id.tvLocation)
        var tvDuration: TextView = itemView.findViewById<TextView>(R.id.tvDuration)
        init {
            itemView.setOnClickListener {
                _ -> dest?.let { onClicked.invoke(it) }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(destination: Dest){
            dest = destination
            tvTitle.text = destination.name

            if(destination.isMeter) {
                tvLocation.text = "${destination.distance} m"
            } else {
                tvLocation.text = "${String.format("%.2f", destination.distance)} Km"
            }

            tvDuration.text = if(destination.totalTime > 0){
                "${destination.totalTime / 60}분"
            } else {
                ""
            }
        }

    }
}

