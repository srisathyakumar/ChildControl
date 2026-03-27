package com.child.app.child

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.widget.NestedScrollView
import com.child.app.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class UsageStatsActivity : AppCompatActivity() {

    private lateinit var txtScreenTime: TextView
    private lateinit var txtCategoryUsage: TextView
    private lateinit var txtLastUpdated: TextView
    private lateinit var chartByTime: BarChart
    private lateinit var chartByApp: BarChart
    private lateinit var appIconsContainer: LinearLayout
    private lateinit var mainScrollView: NestedScrollView
    
    private lateinit var txtHeaderName: TextView
    private lateinit var txtAvatarLetter: TextView

    private var currentFilter = "Daily"
    private val calendar = Calendar.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val childId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stats)

        initViews()
        setupProfileHeader()
        setupCharts()
        setupToggles()
        loadData()

        // Navigation buttons in header
        findViewById<View>(R.id.btnLocation)?.setOnClickListener {
            startActivity(Intent(this, ChildLocationActivity::class.java))
        }

        findViewById<View>(R.id.btnRiskHeader)?.setOnClickListener {
            // Can open risk analysis
        }
    }

    private fun initViews() {
        mainScrollView = findViewById(R.id.mainScrollView)
        txtScreenTime = findViewById(R.id.txtScreenTime)
        txtCategoryUsage = findViewById(R.id.txtCategoryUsage)
        txtLastUpdated = findViewById(R.id.txtLastUpdated)
        chartByTime = findViewById(R.id.chartByTime)
        chartByApp = findViewById(R.id.chartByApp)
        appIconsContainer = findViewById(R.id.appIconsContainer)
        
        txtHeaderName = findViewById(R.id.txtHeaderName)
        txtAvatarLetter = findViewById(R.id.txtAvatarLetter)
    }

    private fun setupProfileHeader() {
        if (childId.isEmpty()) return
        firestore.collection("users").document(childId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: doc.getString("email")?.split("@")?.get(0) ?: "satya"
                txtHeaderName.text = name
                txtAvatarLetter.text = name.take(1).uppercase()
            }
    }

    private fun setupCharts() {
        configureChart(chartByTime)
        configureChart(chartByApp)
    }

    private fun configureChart(chart: BarChart) {
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        chart.setDrawValueAboveBar(false)
        chart.setPinchZoom(false)
        chart.setDoubleTapToZoomEnabled(false)
        chart.setScaleEnabled(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = "#938F99".toColorInt()
        xAxis.textSize = 10f

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = "#F2F4F7".toColorInt()
        leftAxis.setDrawAxisLine(false)
        leftAxis.textColor = "#938F99".toColorInt()
        leftAxis.axisMinimum = 0f

        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
    }

    private fun setupToggles() {
        findViewById<MaterialButtonToggleGroup>(R.id.toggleUsageRange)?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentFilter = if (checkedId == R.id.btnDay) "Daily" else "Weekly"
                loadData()
            }
        }
    }

    private fun loadData() {
        val range = getRangeStartEnd()
        val usageMap = UsageHelper.getFilteredUsage(this, range.first, range.second)
        val totalMs = usageMap.values.sum()

        updateScreenTimeUI(totalMs)
        updateChartsData(usageMap)
        
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        txtLastUpdated.text = "Last updated: $now"
    }

    private fun updateScreenTimeUI(totalMs: Long) {
        val hours = totalMs / 3600000
        val minutes = (totalMs % 3600000) / 60000
        txtScreenTime.text = if (hours > 0) "${hours} h ${minutes} m" else "${minutes} min"
        txtCategoryUsage.text = "Gaming: 0m | Social: 7m"
    }

    private fun updateChartsData(usageMap: Map<String, Long>) {
        // Hourly Usage Chart
        val hourlyEntries = mutableListOf<BarEntry>()
        val hourlyLabels = listOf("0:00", "6:00", "12:00", "18:00", "24:00")
        
        for (i in 0..23) {
            val value = if (i in 8..11) (15..35).random().toFloat() else 0f
            hourlyEntries.add(BarEntry(i.toFloat(), value))
        }
        
        val hourlyDataSet = BarDataSet(hourlyEntries, "Usage")
        hourlyDataSet.color = "#00C853".toColorInt()
        hourlyDataSet.setDrawValues(false)
        
        chartByTime.data = BarData(hourlyDataSet)
        chartByTime.data.barWidth = 0.5f
        chartByTime.xAxis.valueFormatter = IndexAxisValueFormatter(hourlyLabels)
        chartByTime.xAxis.setLabelCount(5, true)
        chartByTime.invalidate()

        // App Usage Chart
        val topApps = usageMap.toList().sortedByDescending { it.second }.take(6)
        val appEntries = mutableListOf<BarEntry>()
        
        appIconsContainer.removeAllViews()
        
        topApps.forEachIndexed { index, pair ->
            val minutes = pair.second / 60000f
            appEntries.add(BarEntry(index.toFloat(), minutes))
            addAppIconToContainer(pair.first)
        }

        val appDataSet = BarDataSet(appEntries, "App Usage")
        appDataSet.color = "#00C853".toColorInt()
        appDataSet.setDrawValues(false)

        chartByApp.data = BarData(appDataSet)
        chartByApp.data.barWidth = 0.6f
        chartByApp.xAxis.valueFormatter = IndexAxisValueFormatter(List(6) { "" })
        chartByApp.invalidate()
    }

    private fun addAppIconToContainer(packageName: String) {
        val imageView = ImageView(this)
        val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics).toInt()
        val params = LinearLayout.LayoutParams(0, size, 1f)
        imageView.layoutParams = params
        try {
            imageView.setImageDrawable(packageManager.getApplicationIcon(packageName))
        } catch (e: Exception) {
            imageView.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        appIconsContainer.addView(imageView)
    }

    private fun getRangeStartEnd(): Pair<Long, Long> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()
        return Pair(start, end)
    }
}
