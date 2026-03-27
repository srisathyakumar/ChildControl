package com.child.app.child

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R

class UsageAppsAdapter(
    private val list: List<Pair<String, Long>>,
    private val totalTime: Long
) : RecyclerView.Adapter<UsageAppsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val txtName: TextView = itemView.findViewById(R.id.appName)
        val txtTime: TextView = itemView.findViewById(R.id.appUsage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (packageName, timeUsed) = list[position]
        val context = holder.itemView.context
        val pm = context.packageManager

        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            holder.txtName.text = pm.getApplicationLabel(appInfo).toString()
            holder.imgIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
        } catch (e: Exception) {
            holder.txtName.text = packageName
            holder.imgIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        val totalSeconds = timeUsed / 1000
        val minutes = totalSeconds / 60
        val hours = minutes / 60
        val remMinutes = minutes % 60
        
        holder.txtTime.text = when {
            hours > 0 -> "${hours}h ${remMinutes}m"
            minutes > 0 -> "${minutes}m"
            timeUsed > 0 -> "Less than 1m"
            else -> "0m"
        }
    }

    override fun getItemCount(): Int = list.size
}
