package com.child.app.child

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R

class MaliciousAppsAdapter(private val list: List<String>) :
    RecyclerView.Adapter<MaliciousAppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtAppName: TextView = view.findViewById(R.id.txtAppName)
        val txtRisk: TextView = view.findViewById(R.id.txtRisk)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val value = list[position]
        val parts = value.split(",")
        if (parts.size >= 3) {
            val packageName = parts[0]
            val risk = parts[1]
            val status = parts[2]
            
            holder.txtAppName.text = packageName
            holder.txtRisk.text = "Risk: $risk%"
            holder.txtStatus.text = "Status: $status"
            
            if (status == "MALICIOUS") {
                holder.txtStatus.setTextColor(Color.RED)
            } else {
                holder.txtStatus.setTextColor(Color.parseColor("#2E7D32"))
            }
        }
    }

    override fun getItemCount() = list.size
}
