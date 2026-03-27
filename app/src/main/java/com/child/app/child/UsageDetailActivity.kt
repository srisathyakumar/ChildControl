package com.child.app.child

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class UsageDetailActivity : AppCompatActivity() {

    private lateinit var txtTotalTime: TextView
    private lateinit var txtTimeLabel: TextView
    private lateinit var txtCurrentDate: TextView
    private lateinit var btnFilter: MaterialButton
    private lateinit var usageChart: BarChart
    private lateinit var recyclerUsage: RecyclerView
    private lateinit var btnPrevDate: View
    private lateinit var btnNextDate: View

    private var currentFilter = "Daily"
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.usage)

        initViews()
        setupChart()
        loadData()

        btnFilter.setOnClickListener {
            toggleFilter()
        }

        btnPrevDate.setOnClickListener {
            changeDate(-1)
        }

        btnNextDate.setOnClickListener {
            changeDate(1)
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        txtTotalTime = findViewById(R.id.txtTotalTime)
        txtTimeLabel = findViewById(R.id.txtTimeLabel)
        txtCurrentDate = findViewById(R.id.txtCurrentDate)
        btnFilter = findViewById(R.id.btnTimeFilter)
        usageChart = findViewById(R.id.usageChart)
        recyclerUsage = findViewById(R.id.recyclerUsage)
        btnPrevDate = findViewById(R.id.btnPrevDate)
        btnNextDate = findViewById(R.id.btnNextDate)

        recyclerUsage.layoutManager = LinearLayoutManager(this)
    }

    private fun setupChart() {
        usageChart.description.isEnabled = false
        usageChart.setDrawGridBackground(false)
        usageChart.setDrawBarShadow(false)
        usageChart.setDrawValueAboveBar(true)
        usageChart.setPinchZoom(false)
        usageChart.setDoubleTapToZoomEnabled(false)
        usageChart.setScaleEnabled(false)

        val xAxis = usageChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = "#44474E".toColorInt()

        val leftAxis = usageChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.setDrawAxisLine(false)
        leftAxis.textColor = "#44474E".toColorInt()
        leftAxis.axisMinimum = 0f

        usageChart.axisRight.isEnabled = false
        usageChart.legend.isEnabled = false
    }

    private fun toggleFilter() {
        currentFilter = when (currentFilter) {
            "Daily" -> "Weekly"
            "Weekly" -> "Monthly"
            else -> "Daily"
        }
        btnFilter.text = currentFilter
        calendar.time = Date()
        loadData()
    }

    private fun changeDate(amount: Int) {
        when (currentFilter) {
            "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, amount)
            "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, amount)
            "Monthly" -> calendar.add(Calendar.MONTH, amount)
        }
        loadData()
    }

    private fun loadData() {
        updateDateText()
        val range = getRangeStartEnd()
        val usageMap = UsageHelper.getFilteredUsage(this, range.first, range.second)
        val totalMs = usageMap.values.sum()
        
        updateTotalTimeText(totalMs)
        recyclerUsage.adapter = UsageAppsAdapter(usageMap.toList().sortedByDescending { it.second }, totalMs)
        
        fetchChartData()
    }

    private fun updateDateText() {
        val sdf = SimpleDateFormat("EEE dd MMM", Locale.getDefault())
        txtCurrentDate.text = sdf.format(calendar.time)
    }

    private fun updateTotalTimeText(totalMs: Long) {
        val hours = totalMs / 3600000
        val minutes = (totalMs % 3600000) / 60000
        txtTotalTime.text = "${hours}h ${minutes}m"
    }

    private fun getRangeStartEnd(): Pair<Long, Long> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    private fun fetchChartData() {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        val hourlyData = UsageHelper.getHourlyUsage(this)
        hourlyData.forEachIndexed { index, value ->
            entries.add(BarEntry(index.toFloat(), value))
            if (index % 6 == 0) labels.add("$index:00") else labels.add("")
        }

        val dataSet = BarDataSet(entries, "Usage")
        dataSet.color = "#00C853".toColorInt()
        dataSet.setDrawValues(false)

        usageChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        usageChart.data = BarData(dataSet)
        usageChart.data.barWidth = 0.5f
        usageChart.invalidate()
    }
}
