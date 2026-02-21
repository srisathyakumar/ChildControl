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
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imgIcon: ImageView =
            itemView.findViewById(R.id.imgAppIcon)

        val txtName: TextView =
            itemView.findViewById(R.id.txtAppName)

        val txtTime: TextView =
            itemView.findViewById(R.id.txtTime)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)

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

            val appInfo =
                pm.getApplicationInfo(packageName, 0)

            val appName =
                pm.getApplicationLabel(appInfo).toString()

            val appIcon =
                pm.getApplicationIcon(appInfo)

            holder.txtName.text = appName
            holder.imgIcon.setImageDrawable(appIcon)

        } catch (e: Exception) {

            // Fallback icon
            holder.txtName.text = packageName
            holder.imgIcon.setImageResource(
                android.R.drawable.sym_def_app_icon
            )
        }

        holder.txtTime.text =
            "Used: ${timeUsed / 60} min"

        holder.itemView.setOnClickListener {
            onClick(packageName)
        }
    }

    override fun getItemCount(): Int = list.size
}