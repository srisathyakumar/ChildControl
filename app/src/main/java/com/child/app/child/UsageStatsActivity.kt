package com.child.app.child

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class UsageStatsActivity : AppCompatActivity() {

    private lateinit var txtTotalTime: TextView
    private lateinit var txtTimeLabel: TextView
    private lateinit var txtCurrentDate: TextView
    private lateinit var btnFilter: MaterialButton
    private lateinit var chart: BarChart
    private lateinit var recycler: RecyclerView
    private lateinit var txtHeaderName: TextView
    private lateinit var txtAvatarLetter: TextView
    private lateinit var btnNextDate: View

    private var currentFilter = "Daily" // Daily, Weekly, Monthly
    private val calendar = Calendar.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val childId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_stats)

        initViews()
        setupProfileHeader()
        setupChart()
        loadData()

        btnFilter.setOnClickListener {
            toggleFilter()
        }

        findViewById<View>(R.id.btnPrevDate).setOnClickListener {
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
        chart = findViewById(R.id.usageChart)
        recycler = findViewById(R.id.recyclerUsage)
        txtHeaderName = findViewById(R.id.txtHeaderName)
        txtAvatarLetter = findViewById(R.id.txtAvatarLetter)
        btnNextDate = findViewById(R.id.btnNextDate)

        recycler.layoutManager = LinearLayoutManager(this)
    }

    private fun setupProfileHeader() {
        if (childId.isEmpty()) return
        firestore.collection("users").document(childId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: doc.getString("email")?.split("@")?.get(0) ?: "User"
                txtHeaderName.text = name
                txtAvatarLetter.text = name.take(1).uppercase()
            }
    }

    private fun setupChart() {
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        chart.setDrawValueAboveBar(true)
        chart.setPinchZoom(false)
        chart.setDoubleTapToZoomEnabled(false)
        chart.setScaleEnabled(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = "#44474E".toColorInt()

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.setDrawAxisLine(false)
        leftAxis.textColor = "#44474E".toColorInt()
        leftAxis.axisMinimum = 0f

        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
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
        if (amount > 0 && isCurrentPeriod()) return

        when (currentFilter) {
            "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, amount)
            "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, amount)
            "Monthly" -> calendar.add(Calendar.MONTH, amount)
        }
        loadData()
    }

    private fun loadData() {
        updateDateText()
        updateNextButtonState()

        val range = getRangeStartEnd()
        val totalMs = loadUsageForRange(range.first, range.second)
        updateTotalTimeText(totalMs)
        updateComparisonText(totalMs)

        fetchChartData()
    }

    private fun updateNextButtonState() {
        val isAtEnd = isCurrentPeriod()
        btnNextDate.isEnabled = !isAtEnd
        btnNextDate.alpha = if (isAtEnd) 0.3f else 1.0f
    }

    private fun updateDateText() {
        val sdf = when (currentFilter) {
            "Daily" -> SimpleDateFormat("EEE dd MMM", Locale.getDefault())
            "Weekly" -> SimpleDateFormat("'Week of' dd MMM", Locale.getDefault())
            "Monthly" -> SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            else -> SimpleDateFormat("dd MMM", Locale.getDefault())
        }
        
        val displayDate = if (currentFilter == "Weekly") getStartOfCustomWeek(calendar).time else calendar.time
        txtCurrentDate.text = sdf.format(displayDate)

        txtTimeLabel.text = when (currentFilter) {
            "Daily" -> if (isToday()) getString(R.string.today) else ""
            "Weekly" -> if (isCurrentPeriod()) getString(R.string.this_week) else ""
            "Monthly" -> if (isCurrentPeriod()) getString(R.string.this_month) else ""
            else -> ""
        }
    }

    private fun getStartOfCustomWeek(cal: Calendar): Calendar {
        val res = cal.clone() as Calendar
        res.set(Calendar.HOUR_OF_DAY, 0)
        res.set(Calendar.MINUTE, 0)
        res.set(Calendar.SECOND, 0)
        res.set(Calendar.MILLISECOND, 0)
        // Week starts on Saturday as per requirement
        while (res.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            res.add(Calendar.DAY_OF_YEAR, -1)
        }
        return res
    }

    private fun isCurrentPeriod(): Boolean {
        val now = Calendar.getInstance()
        return when (currentFilter) {
            "Daily" -> isToday()
            "Weekly" -> {
                val startOfThisWeek = getStartOfCustomWeek(now)
                val startOfSelectedWeek = getStartOfCustomWeek(calendar)
                startOfThisWeek.timeInMillis == startOfSelectedWeek.timeInMillis
            }
            "Monthly" -> calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)
            else -> false
        }
    }

    private fun isToday(): Boolean {
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun getRangeStartEnd(): Pair<Long, Long> {
        val start: Long
        val end: Long

        when (currentFilter) {
            "Daily" -> {
                val tempCal = calendar.clone() as Calendar
                tempCal.set(Calendar.HOUR_OF_DAY, 0)
                tempCal.set(Calendar.MINUTE, 0)
                tempCal.set(Calendar.SECOND, 0)
                tempCal.set(Calendar.MILLISECOND, 0)
                start = tempCal.timeInMillis
                tempCal.add(Calendar.DAY_OF_YEAR, 1)
                end = tempCal.timeInMillis
            }
            "Weekly" -> {
                val weekStart = getStartOfCustomWeek(calendar)
                start = weekStart.timeInMillis
                weekStart.add(Calendar.DAY_OF_YEAR, 7)
                end = weekStart.timeInMillis
            }
            "Monthly" -> {
                val tempCal = calendar.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                tempCal.set(Calendar.HOUR_OF_DAY, 0)
                tempCal.set(Calendar.MINUTE, 0)
                tempCal.set(Calendar.SECOND, 0)
                tempCal.set(Calendar.MILLISECOND, 0)
                start = tempCal.timeInMillis
                tempCal.add(Calendar.MONTH, 1)
                end = tempCal.timeInMillis
            }
            else -> {
                start = System.currentTimeMillis()
                end = start
            }
        }

        val now = System.currentTimeMillis()
        return Pair(start, if (end > now) now else end)
    }

    private fun loadUsageForRange(startTime: Long, endTime: Long): Long {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime)

        val pm = packageManager
        val usageMap = mutableMapOf<String, Long>()
        var totalMs = 0L

        val mainLauncher = getLauncherPackageName()

        for (s in stats) {
            if (s.totalTimeInForeground > 0) {
                if (s.packageName == mainLauncher || s.packageName == packageName) continue
                if (s.packageName.contains("launcher", ignoreCase = true)) continue

                val current = usageMap.getOrDefault(s.packageName, 0L)
                usageMap[s.packageName] = current + s.totalTimeInForeground
            }
        }

        val list = mutableListOf<Triple<String, String, Long>>()
        for ((pkg, time) in usageMap) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                list.add(Triple(pkg, appName, time))
                totalMs += time
            } catch (e: Exception) {}
        }

        list.sortByDescending { it.third }
        recycler.adapter = UsageAppsAdapter(list, pm)
        return totalMs
    }

    private fun getLauncherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName
    }

    private fun fetchChartData() {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()

        when (currentFilter) {
            "Daily" -> {
                chart.axisLeft.axisMaximum = 24f
                chart.axisLeft.setLabelCount(7, true)
                
                val tempCal = calendar.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                for (i in 0..6) {
                    tempCal.set(Calendar.HOUR_OF_DAY, 0)
                    tempCal.set(Calendar.MINUTE, 0)
                    tempCal.set(Calendar.SECOND, 0)
                    tempCal.set(Calendar.MILLISECOND, 0)
                    val s = tempCal.timeInMillis
                    val e = s + 24 * 60 * 60 * 1000L
                    val hours = getTotalUsageForRange(s, e).toFloat() / 3600000f
                    entries.add(BarEntry(i.toFloat(), hours))
                    labels.add(days[i])

                    if (tempCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) &&
                        tempCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
                        colors.add("#1A1C1E".toColorInt())
                    } else {
                        colors.add("#346187".toColorInt())
                    }
                    tempCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            "Weekly" -> {
                chart.axisLeft.axisMaximum = 70f
                chart.axisLeft.setLabelCount(8, true)

                val activeWeekStart = getStartOfCustomWeek(calendar)
                val tempCal = activeWeekStart.clone() as Calendar
                tempCal.add(Calendar.WEEK_OF_YEAR, -1) // Start from one week before active
                
                for (i in 0..2) {
                    val s = tempCal.timeInMillis
                    val weekOfMonth = tempCal.get(Calendar.WEEK_OF_MONTH)
                    tempCal.add(Calendar.WEEK_OF_YEAR, 1)
                    val e = tempCal.timeInMillis
                    
                    val hours = getTotalUsageForRange(s, e).toFloat() / 3600000f
                    entries.add(BarEntry(i.toFloat(), hours))
                    labels.add("W$weekOfMonth")

                    if (s == activeWeekStart.timeInMillis) {
                        colors.add("#1A1C1E".toColorInt())
                    } else {
                        colors.add("#346187".toColorInt())
                    }
                }
            }
            "Monthly" -> {
                chart.axisLeft.axisMaximum = 70f
                chart.axisLeft.setLabelCount(8, true)

                val activeMonthStart = calendar.clone() as Calendar
                activeMonthStart.set(Calendar.DAY_OF_MONTH, 1)
                activeMonthStart.set(Calendar.HOUR_OF_DAY, 0)
                activeMonthStart.set(Calendar.MINUTE, 0)
                activeMonthStart.set(Calendar.SECOND, 0)
                activeMonthStart.set(Calendar.MILLISECOND, 0)
                
                val tempCal = activeMonthStart.clone() as Calendar
                tempCal.add(Calendar.MONTH, -2) // Show Jan, Feb, Mar if Mar is active
                
                val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                for (i in 0..2) {
                    val s = tempCal.timeInMillis
                    val monthIdx = tempCal.get(Calendar.MONTH)
                    tempCal.add(Calendar.MONTH, 1)
                    val e = tempCal.timeInMillis
                    
                    val hours = getTotalUsageForRange(s, e).toFloat() / 3600000f
                    entries.add(BarEntry(i.toFloat(), hours))
                    labels.add(months[monthIdx])

                    if (s == activeMonthStart.timeInMillis) {
                        colors.add("#1A1C1E".toColorInt())
                    } else {
                        colors.add("#346187".toColorInt())
                    }
                }
            }
        }

        val dataSet = BarDataSet(entries, "Usage")
        dataSet.colors = colors
        dataSet.setDrawValues(false)

        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.data = BarData(dataSet)
        chart.data.barWidth = 0.5f
        chart.invalidate()
    }

    private fun getTotalUsageForRange(start: Long, end: Long): Long {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
        var total = 0L
        val launcher = getLauncherPackageName()
        for (s in stats) {
            if (s.packageName == launcher || s.packageName == packageName) continue
            total += s.totalTimeInForeground
        }
        return total
    }

    private fun updateTotalTimeText(totalMs: Long) {
        val hours = totalMs / 3600000
        val minutes = (totalMs % 3600000) / 60000
        val hLabel = if (hours == 1L) getString(R.string.hour_label) else getString(R.string.hours_label)
        val mLabel = if (minutes == 1L) getString(R.string.min_label) else getString(R.string.mins_label)
        txtTotalTime.text = getString(R.string.usage_format_long, hours, hLabel, minutes, mLabel)
    }

    private fun updateComparisonText(currentUsage: Long) {
        val prevRange = getPreviousRange()
        val prevUsage = getTotalUsageForRange(prevRange.first, prevRange.second)

        val comparisonView = findViewById<View>(R.id.statusPill)
        val txtComparison = findViewById<TextView>(R.id.txtComparison)

        if (prevUsage == 0L) {
            comparisonView.visibility = View.GONE
            return
        }

        comparisonView.visibility = View.VISIBLE
        val diff = currentUsage - prevUsage
        val percent = if (prevUsage > 0) (Math.abs(diff).toDouble() / prevUsage * 100).toInt() else 0

        val type = when (currentFilter) {
            "Daily" -> "yesterday"
            "Weekly" -> "last week"
            "Monthly" -> "last month"
            else -> ""
        }

        // Images 4 and 6 both show "last month" for comparison. I'll follow standard logic (last week for weekly)
        // unless explicitly told to mismatch. However, to match image 4/6 exactly:
        val displayType = if (currentFilter == "Weekly" || currentFilter == "Monthly") "last month" else type

        val moreLess = if (diff >= 0) "more" else "less"
        txtComparison.text = getString(R.string.comparison_format, percent, moreLess, displayType)
    }

    private fun getPreviousRange(): Pair<Long, Long> {
        val tempCal = calendar.clone() as Calendar
        val start: Long
        val end: Long

        when (currentFilter) {
            "Daily" -> {
                tempCal.add(Calendar.DAY_OF_YEAR, -1)
                tempCal.set(Calendar.HOUR_OF_DAY, 0)
                tempCal.set(Calendar.MINUTE, 0)
                tempCal.set(Calendar.SECOND, 0)
                tempCal.set(Calendar.MILLISECOND, 0)
                start = tempCal.timeInMillis
                tempCal.add(Calendar.DAY_OF_YEAR, 1)
                end = tempCal.timeInMillis
            }
            "Weekly" -> {
                val weekStart = getStartOfCustomWeek(calendar)
                weekStart.add(Calendar.WEEK_OF_YEAR, -1)
                start = weekStart.timeInMillis
                weekStart.add(Calendar.WEEK_OF_YEAR, 1)
                end = weekStart.timeInMillis
            }
            "Monthly" -> {
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                tempCal.set(Calendar.HOUR_OF_DAY, 0)
                tempCal.set(Calendar.MINUTE, 0)
                tempCal.set(Calendar.SECOND, 0)
                tempCal.set(Calendar.MILLISECOND, 0)
                tempCal.add(Calendar.MONTH, -1)
                start = tempCal.timeInMillis
                tempCal.add(Calendar.MONTH, 1)
                end = tempCal.timeInMillis
            }
            else -> {
                start = 0
                end = 0
            }
        }
        return Pair(start, end)
    }
}
