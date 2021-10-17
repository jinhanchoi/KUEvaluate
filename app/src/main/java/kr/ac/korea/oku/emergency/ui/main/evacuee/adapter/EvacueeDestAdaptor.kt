package kr.ac.korea.oku.emergency.ui.main.evacuee.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.databinding.ItemDestMapBinding

class EvacueeDestAdaptor(private val onClicked : (dest:Dest)->Unit) : RecyclerView.Adapter<EvacueeDestAdaptor.DestViewHolder>() {
    val APIKEY_ID = "kio62awlhg"
    val APIKEY = "qvNDNWz3EKLRea4JlkUIDWRiLdO27ODpkzvtadT1"

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

        init {
            itemView.setOnClickListener {
                view ->
                dest?.let { onClicked.invoke(it) }
                dest?.let { Toast.makeText(view.context, it.lat.toString(),Toast.LENGTH_SHORT).show() }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(destination: Dest){
            dest = destination
            tvTitle.text = destination.name
            tvLocation.text = "${String.format("%.2f", destination.distance)} Km"
        }

    }
}

