package com.child.app.child

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class WellbeingDashboardActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wellbeing_dashboard)

        pieChart = findViewById(R.id.pieChart)
        
        // Example usage
        val dummyUsage = mapOf("Gaming" to 120L, "Social" to 60L, "Education" to 30L)
        setupPieChart(dummyUsage)
    }

    private fun setupPieChart(appUsage: Map<String, Long>) {
        val entries = ArrayList<PieEntry>()
        for ((app, minutes) in appUsage) {
            entries.add(PieEntry(minutes.toFloat(), app))
        }

        val dataSet = PieDataSet(entries, "Apps")
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.isDrawHoleEnabled = true
        pieChart.invalidate()
    }
}
