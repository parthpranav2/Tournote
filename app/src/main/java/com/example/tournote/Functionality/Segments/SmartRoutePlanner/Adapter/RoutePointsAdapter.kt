package com.example.tournote.Functionality.Segments.SmartRoutePlanner.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tournote.R
import com.example.tournote.RoutePointDataClass

class RoutePointsAdapter(
    private var routePoints: MutableList<RoutePointDataClass>,
    private val onItemClick: (RoutePointDataClass, Int) -> Unit,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<RoutePointsAdapter.RoutePointViewHolder>() {

    inner class RoutePointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtLabel: TextView = itemView.findViewById(R.id.txtlabel)
        val txtPoints: TextView = itemView.findViewById(R.id.txtPoints)
        val btnPoints: RelativeLayout = itemView.findViewById(R.id.btnPoints)
        val btnRemove: RelativeLayout = itemView.findViewById(R.id.btnRemove)
        val labelContainer: RelativeLayout = itemView.findViewById(R.id.label)
        val labelImage: ImageView = itemView.findViewById(R.id.imglabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutePointViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_completeroute_recyclerview, parent, false)
        return RoutePointViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoutePointViewHolder, position: Int) {
        val routePoint = routePoints[position]

        // Set the text
        holder.txtPoints.text = routePoint.name

        // Set label based on position and type
        when {
            routePoint.isStartPoint -> {
                holder.txtLabel.text = ""
                // You can change the background drawable for start point if needed
                holder.labelImage.setImageResource(R.drawable.circleforlabel_startpoint)
            }
            routePoint.isEndPoint -> {
                holder.txtLabel.text = "E"
                // You can change the background drawable for end point if needed
                holder.labelImage.setImageResource(R.drawable.circleforlabel_notstartpoint)
            }
            else -> {
                // For stops, use alphabetical labels (A, B, C, etc.)
                val stopIndex = getStopIndex(position)
                holder.txtLabel.text = ('A' + stopIndex).toString()
                // Use the default drawable for stops
                holder.labelImage.setImageResource(R.drawable.circleforlabel_notstartpoint)
            }
        }

        // Handle item click
        holder.btnPoints.setOnClickListener {
            onItemClick(routePoint, position)
        }

        // Handle remove click
        holder.btnRemove.setOnClickListener {
            onRemoveClick(position)
        }

        // Hide remove button for start and end points
        if (routePoint.isStartPoint || routePoint.isEndPoint) {
            holder.btnRemove.visibility = View.GONE
        } else {
            holder.btnRemove.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = routePoints.size

    private fun getStopIndex(position: Int): Int {
        // Calculate the stop index (A, B, C, etc.) excluding start and end points
        var stopCount = 0
        for (i in 0 until position) {
            if (!routePoints[i].isStartPoint && !routePoints[i].isEndPoint) {
                stopCount++
            }
        }
        return stopCount
    }

    fun updateRoutePoints(newRoutePoints: List<RoutePointDataClass>) {
        routePoints.clear()
        routePoints.addAll(newRoutePoints)
        notifyDataSetChanged()
    }

    fun addRoutePoint(routePoint: RoutePointDataClass) {
        // Add before the last item (which should be the end point)
        val insertPosition = if (routePoints.isNotEmpty() && routePoints.last().isEndPoint) {
            routePoints.size - 1
        } else {
            routePoints.size
        }
        routePoints.add(insertPosition, routePoint)
        notifyItemInserted(insertPosition)
        // Update labels for items after the inserted position
        notifyItemRangeChanged(insertPosition, routePoints.size - insertPosition)
    }

    fun removeRoutePoint(position: Int) {
        if (position >= 0 && position < routePoints.size) {
            routePoints.removeAt(position)
            notifyItemRemoved(position)
            // Update labels for items after the removed position
            notifyItemRangeChanged(position, routePoints.size - position)
        }
    }

    fun getRoutePoints(): List<RoutePointDataClass> {
        return routePoints.toList()
    }

    fun getStopsCount(): Int {
        return routePoints.count { !it.isStartPoint && !it.isEndPoint }
    }
}