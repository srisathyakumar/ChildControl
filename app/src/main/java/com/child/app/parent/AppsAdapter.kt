package com.child.app.parent

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R

class AppsAdapter(
    private val list: List<Pair<String, Long>>,
    private val totalTime: Long,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imgIcon: ImageView =
            itemView.findViewById(R.id.appIcon)

        val txtName: TextView =
            itemView.findViewById(R.id.appName)

        val txtTime: TextView =
            itemView.findViewById(R.id.appUsage)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val (packageName, timeUsed) = list[position]
        val context = holder.itemView.context
        val pm = context.packageManager

        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            val appIcon = pm.getApplicationIcon(appInfo)

            holder.txtName.text = appName
            holder.imgIcon.setImageDrawable(appIcon)

        } catch (e: Exception) {
            holder.txtName.text = packageName
            holder.imgIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        // Format app usage time
        val totalMinutes = timeUsed / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        
        holder.txtTime.text = if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }

        holder.itemView.setOnClickListener {
            onClick(packageName)
        }
    }

    override fun getItemCount(): Int = list.size
}
