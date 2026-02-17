package com.child.app.parent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R

class PairedChildrenAdapter(
    private val list: List<Pair<String, String>>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<PairedChildrenAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val txtChildName: TextView =
            itemView.findViewById(R.id.txtChildId)
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

        // ✅ Show NAME instead of UID
        holder.txtChildName.text = childName

        // ✅ Click → send childId to activity
        holder.itemView.setOnClickListener {

            val context = holder.itemView.context

            val intent = Intent(
                context,
                ChildAppsActivity::class.java
            )

            intent.putExtra("childUid", childUid)

            context.startActivity(intent)
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }
}