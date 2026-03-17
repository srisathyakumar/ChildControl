package com.child.app.parent

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R

class PairedChildrenAdapter(
    private val list: List<Pair<String, String>> // childId , childName
) : RecyclerView.Adapter<PairedChildrenAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val txtChildName: TextView =
            itemView.findViewById(R.id.txtChildName)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_child, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val childId = list[position].first
        val childName = list[position].second

        holder.txtChildName.text = childName

        holder.itemView.setOnClickListener {

            val context = holder.itemView.context

            val intent = Intent(
                context,
                ChildControlActivity::class.java
            )

            intent.putExtra("childUid", childId)

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}