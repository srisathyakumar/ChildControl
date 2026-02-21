package com.child.app.child

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R

class UsageAppsAdapter(
    private val list: List<Triple<String, String, Long>>,
    private val pm: PackageManager
) : RecyclerView.Adapter<UsageAppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgAppIcon)
        val name: TextView = view.findViewById(R.id.txtAppName)
        val time: TextView = view.findViewById(R.id.txtUsageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usage_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val (packageName, appName, timeMs) = list[position]

        holder.name.text = appName

        val minutes = timeMs / 1000 / 60
        holder.time.text = "Used: $minutes min"

        try {
            val icon = pm.getApplicationIcon(packageName)
            holder.icon.setImageDrawable(icon)
        } catch (e: Exception) {
            holder.icon.setImageResource(R.mipmap.ic_launcher)
        }
    }

    override fun getItemCount() = list.size
}