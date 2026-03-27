package com.child.app.parent

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ParentUsageActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var usageSummaryText: TextView
    private lateinit var recyclerApps: RecyclerView
    private lateinit var barChart: BarChart
    private var childUid: String = ""
    private var usageListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_usage)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        usageSummaryText = findViewById(R.id.usageSummaryText)
        recyclerApps = findViewById(R.id.recyclerAppsUsage)
        barChart = findViewById(R.id.usageChart)
        childUid = intent.getStringExtra("childUid") ?: ""

        setupChart()
        startListeningUsage()
    }

    private fun setupChart() {
        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            isHighlightFullBarEnabled = false
            legend.isEnabled = false
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.GRAY
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                textColor = Color.GRAY
                axisMinimum = 0f
                setDrawAxisLine(false)
            }
            axisRight.isEnabled = false
        }
    }

    private fun startListeningUsage() {
        if (childUid.isEmpty()) return

        usageListener = firestore.collection("usage")
            .document(childUid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e("ParentUsage", "Error listening for usage", error)
                    return@addSnapshotListener
                }

                if (doc == null || !doc.exists()) {
                    usageSummaryText.text = "No data"
                    return@addSnapshotListener
                }

                val totalMs = doc.getLong("totalTime") ?: 0L
                val topAppsList = doc.get("topApps") as? List<Map<String, Any>> ?: emptyList()

                // Format total time: Xh Ym
                val h = totalMs / 3600000
                val m = (totalMs % 3600000) / 60000
                usageSummaryText.text = if (h > 0) "${h}h ${m}m" else "${m}m"

                // Map to Pair<String, Long> for the adapter (pkg to time)
                val displayList = topAppsList.map { 
                    (it["pkg"] as? String ?: "") to (it["time"] as? Long ?: 0L)
                }

                // Update UI components
                updateChart(totalMs)
                
                val adapter = AppsAdapter(displayList, totalMs) { pkg ->
                    // Handle app click if needed
                }
                recyclerApps.adapter = adapter
            }
    }

    private fun updateChart(totalTodayMs: Long) {
        val entries = ArrayList<BarEntry>()
        val labels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        // Mocking historical data for the chart, but showing today's actual data
        entries.add(BarEntry(0f, 3.5f))
        entries.add(BarEntry(1f, 5.2f))
        entries.add(BarEntry(2f, 6.1f))
        entries.add(BarEntry(3f, 5.8f))
        
        val todayHours = totalTodayMs.toFloat() / 3600000f
        entries.add(BarEntry(4f, todayHours))

        val dataSet = BarDataSet(entries, "Screen Time")
        dataSet.color = Color.parseColor("#3F51B5")
        dataSet.setDrawValues(false)

        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.data = BarData(dataSet)
        barChart.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        usageListener?.remove()
    }
}
