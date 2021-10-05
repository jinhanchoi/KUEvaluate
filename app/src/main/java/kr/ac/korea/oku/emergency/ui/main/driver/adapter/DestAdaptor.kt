package kr.ac.korea.oku.emergency.ui.main.driver.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.databinding.ItemDestGridBinding

class DestAdaptor() : RecyclerView.Adapter<DestAdaptor.DestViewHolder>() {
    var items : List<Destination> = listOf(Destination("Test",""),Destination("Testttt",""),Destination("Tetsdfsd",""),Destination("sdsdf",""))
    override fun getItemViewType(position: Int): Int = R.layout.item_dest_grid

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: DestAdaptor.DestViewHolder, position: Int) {
        items.getOrNull(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestAdaptor.DestViewHolder {
        return LayoutInflater.from(parent.context).run {
            DestViewHolder(
                ItemDestGridBinding.inflate(this,parent,false)
            )
        }
    }

    inner class DestViewHolder(
        private val binding: ItemDestGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        var tvName = itemView.findViewById<TextView>(R.id.tvName)
        init {

        }
        fun bind(destination: Destination){
            tvName.text = destination.name
        }
    }
}

data class Destination(
    val name : String,
    val address : String
)