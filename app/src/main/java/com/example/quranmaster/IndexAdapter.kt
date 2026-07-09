package com.example.quranmaster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IndexAdapter(
    private val items: List<IndexItem>,
    private val onItemClick: (IndexItem) -> Unit
) : RecyclerView.Adapter<IndexAdapter.IndexViewHolder>() {

    class IndexViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemNumber: TextView = view.findViewById(R.id.itemNumber)
        val itemName: TextView = view.findViewById(R.id.itemName)
        val itemSubInfo: TextView = view.findViewById(R.id.itemSubInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndexViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_index, parent, false)
        return IndexViewHolder(view)
    }

    override fun onBindViewHolder(holder: IndexViewHolder, position: Int) {
        val item = items[position]
        holder.itemNumber.text = item.number.toString()
        holder.itemName.text = item.name
        holder.itemSubInfo.text = item.subInfo
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
