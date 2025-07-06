package com.example.tournote.Functionality

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tournote.R

class GeocodingResultsAdapter(
    private val results: MutableList<GeocodingResultsDataClass>,
    private val onItemClick: (GeocodingResultsDataClass) -> Unit
) : RecyclerView.Adapter<GeocodingResultsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewName: TextView = view.findViewById(R.id.textViewResultName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_geocode_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.textViewName.text = result.name
        holder.itemView.setOnClickListener {
            onItemClick(result)
        }
    }

    override fun getItemCount(): Int = results.size

    fun updateResults(newResults: List<GeocodingResultsDataClass>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }
}