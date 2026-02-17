package com.child.app.parent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R

class AppsAdapter(
    private val list: List<Pair<String, Long>>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    class ViewHolder(v: View) :
        RecyclerView.ViewHolder(v) {

        val txtName: TextView =
            v.findViewById(R.id.txtAppName)

        val txtTime: TextView =
            v.findViewById(R.id.txtTimeUsed)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)

        return ViewHolder(v)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val (pkg, time) = list[position]

        holder.txtName.text = pkg
        holder.txtTime.text =
            "Used: ${time / 60000} min"

        holder.itemView.setOnClickListener {
            onClick(pkg)
        }
    }

    override fun getItemCount() = list.size
}
