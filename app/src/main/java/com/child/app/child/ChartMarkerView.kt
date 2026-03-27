package com.child.app.child

import android.content.Context
import android.widget.TextView
import com.child.app.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class ChartMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val txtValue: TextView = findViewById(R.id.txtMarkerValue)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            val mins = e.y.toInt()
            txtValue.text = "$mins min"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}
