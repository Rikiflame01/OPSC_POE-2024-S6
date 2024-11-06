package com.example.opsc_poe_task_app

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

class TimeSheetGraphActivity : AppCompatActivity() {

    private var avgMinPerDay: Double = 0.0
    private var avgMaxPerDay: Double = 0.0
    private var avgMinPerDayMap: Map<String, Double> = emptyMap()
    private var avgMaxPerDayMap: Map<String, Double> = emptyMap()

    private lateinit var lineChart: LineChart
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var startDateInput: EditText
    private lateinit var endDateInput: EditText
    private var startDateFilter: Date? = null
    private var endDateFilter: Date? = null

    private lateinit var minMaxLayout: LinearLayout

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    private val daysOfWeek = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timesheet_graph)

        //Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/categories")
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lineChart = findViewById(R.id.lineChart)
        startDateInput = findViewById(R.id.startDateInput)
        endDateInput = findViewById(R.id.endDateInput)
        minMaxLayout = findViewById(R.id.minMaxLayout)

        startDateInput.setOnClickListener {
            showDatePickerDialog(startDateInput, true)
        }

        endDateInput.setOnClickListener {
            showDatePickerDialog(endDateInput, false)
        }

        //Load quests and display min/max per day
        loadQuestData {
            //load and display the graph
            loadAndDisplayGraph()
        }
    }

    private fun showDatePickerDialog(dateInput: EditText, isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                val selectedDateStr = dateFormat.format(selectedDate.time)
                dateInput.setText(selectedDateStr)

                if (isStartDate) {
                    startDateFilter = selectedDate.time
                } else {
                    endDateFilter = selectedDate.time
                }

                //Reload
                loadQuestData {
                    loadAndDisplayGraph()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun loadQuestData(onDataLoaded: () -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dayMinMaxMap = mutableMapOf<String, Pair<Double, Double>>() // Map<Day, Pair<MinTotal, MaxTotal>>

                //Initialize the map with zeros
                for (day in daysOfWeek) {
                    dayMinMaxMap[day] = Pair(0.0, 0.0)
                }

                for (categorySnapshot in snapshot.children) {
                    val questsSnapshot = categorySnapshot.child("quests")

                    for (questSnapshot in questsSnapshot.children) {
                        val quest = questSnapshot.getValue(Quest::class.java)

                        if (quest != null) {
                            val questDate = parseDate(quest.date)
                            if (questDate != null) {
                                val isWithinStartDate = startDateFilter?.let { !questDate.before(it) } ?: true
                                val isWithinEndDate = endDateFilter?.let { !questDate.after(it) } ?: true

                                if (isWithinStartDate && isWithinEndDate) {
                                    val minGoal = quest.minGoal.toDouble()
                                    val maxGoal = quest.maxGoal.toDouble()
                                    val selectedDays = quest.daysOfWeek

                                    for (day in selectedDays) {
                                        val dayAbbrev = day.take(3)
                                        if (dayMinMaxMap.containsKey(dayAbbrev)) {
                                            val currentMinMax = dayMinMaxMap[dayAbbrev] ?: Pair(0.0, 0.0)
                                            val newMin = currentMinMax.first + minGoal
                                            val newMax = currentMinMax.second + maxGoal
                                            dayMinMaxMap[dayAbbrev] = Pair(newMin, newMax)
                                        } else {
                                            Log.e("TimeSheetGraphActivity", "Invalid day abbreviation: $day")
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.e("TimeSheetGraphActivity", "Quest is null. Possible data mapping issue.")
                        }
                    }
                }

                //average min and max per day
                val totalMin = dayMinMaxMap.values.sumOf { it.first }
                val totalMax = dayMinMaxMap.values.sumOf { it.second }
                val numDays = dayMinMaxMap.count { it.value.first > 0 || it.value.second > 0 }

                avgMinPerDay = if (numDays > 0) totalMin / numDays else 0.0
                avgMaxPerDay = if (numDays > 0) totalMax / numDays else 0.0

                //Store day-wise averages
                avgMinPerDayMap = dayMinMaxMap.mapValues { it.value.first }
                avgMaxPerDayMap = dayMinMaxMap.mapValues { it.value.second }

                displayMinMaxPerDay(dayMinMaxMap, avgMinPerDay, avgMaxPerDay)

                onDataLoaded()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TimeSheetGraphActivity, "Failed to load quests: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayMinMaxPerDay(
        dayMinMaxMap: Map<String, Pair<Double, Double>>,
        avgMinPerDay: Double,
        avgMaxPerDay: Double
    ) {
        minMaxLayout.removeAllViews()

        //For each day, display the min and max hours
        for (day in daysOfWeek) {
            val minMax = dayMinMaxMap[day] ?: Pair(0.0, 0.0)
            val minHours = minMax.first
            val maxHours = minMax.second

            val textView = TextView(this)
            textView.text = "$day - Min: ${String.format("%.2f", minHours)} hrs, Max: ${String.format("%.2f", maxHours)} hrs"
            minMaxLayout.addView(textView)
        }

        //average min and max per day
        val avgTextView = TextView(this)
        avgTextView.text = "Average Minimum Hour Goal per Day: ${String.format("%.2f", avgMinPerDay)} hrs, Average Maximum Hour Goal per Day: ${String.format("%.2f", avgMaxPerDay)} hrs"
        avgTextView.setPadding(0, 16, 0, 0)
        minMaxLayout.addView(avgTextView)
    }

    private fun loadAndDisplayGraph() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dateHoursMap = mutableMapOf<String, Double>()

                for (categorySnapshot in snapshot.children) {
                    val timesheetEntriesSnapshot = categorySnapshot.child("TimesheetEntries")

                    for (entrySnapshot in timesheetEntriesSnapshot.children) {
                        val entry = entrySnapshot.getValue(TimeSheetEntry::class.java)
                        if (entry != null) {
                            val entryDate = parseDate(entry.date)
                            if (entryDate != null) {
                                val isWithinStartDate = startDateFilter?.let { !entryDate.before(it) } ?: true
                                val isWithinEndDate = endDateFilter?.let { !entryDate.after(it) } ?: true

                                if (isWithinStartDate && isWithinEndDate) {
                                    val dateKey = dateFormat.format(entryDate)
                                    val entryHours = entry.getDurationInHours()

                                    dateHoursMap[dateKey] = dateHoursMap.getOrDefault(dateKey, 0.0) + entryHours
                                }
                            }
                        }
                    }
                }

                //Prepare data
                displayGraph(dateHoursMap)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TimeSheetGraphActivity, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun parseDate(dateStr: String): Date? {
        return try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun displayGraph(dateHoursMap: Map<String, Double>) {
        if (dateHoursMap.isEmpty()) {
            Toast.makeText(this, "No data available for the selected period", Toast.LENGTH_SHORT).show()
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        val sortedDates = dateHoursMap.keys.map { dateFormat.parse(it)!! }.sorted()
        val entriesTotalHours = ArrayList<Entry>()
        val entriesAvgMinGoal = ArrayList<Entry>()
        val entriesAvgMaxGoal = ArrayList<Entry>()
        val xAxisLabels = ArrayList<String>()

        for ((index, date) in sortedDates.withIndex()) {
            val dateStr = dateFormat.format(date)
            val hours = dateHoursMap[dateStr] ?: 0.0
            entriesTotalHours.add(Entry(index.toFloat(), hours.toFloat()))
            xAxisLabels.add(dateStr)

            val calendar = Calendar.getInstance()
            calendar.time = date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayAbbrev = when (dayOfWeek) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                else -> ""
            }

            val avgMinGoal = avgMinPerDayMap[dayAbbrev] ?: 0.0
            val avgMaxGoal = avgMaxPerDayMap[dayAbbrev] ?: 0.0

            entriesAvgMinGoal.add(Entry(index.toFloat(), avgMinGoal.toFloat()))
            entriesAvgMaxGoal.add(Entry(index.toFloat(), avgMaxGoal.toFloat()))
        }

        val dataSets: MutableList<ILineDataSet> = mutableListOf()

        val lineDataSetTotalHours = LineDataSet(entriesTotalHours, "Total Hours Worked")
        lineDataSetTotalHours.color = ContextCompat.getColor(this, R.color.colourPrimary)
        lineDataSetTotalHours.valueTextColor = ContextCompat.getColor(this, R.color.colourPrimaryDark)
        lineDataSetTotalHours.lineWidth = 2f
        lineDataSetTotalHours.circleRadius = 4f
        lineDataSetTotalHours.setDrawValues(true)
        dataSets.add(lineDataSetTotalHours)

        val lineDataSetAvgMinGoal = LineDataSet(entriesAvgMinGoal, "Average Min Goal")
        lineDataSetAvgMinGoal.color = ContextCompat.getColor(this, R.color.colourAccent)
        lineDataSetAvgMinGoal.valueTextColor = ContextCompat.getColor(this, R.color.colourAccent)
        lineDataSetAvgMinGoal.lineWidth = 2f
        lineDataSetAvgMinGoal.circleRadius = 4f
        lineDataSetAvgMinGoal.setDrawValues(true)
        dataSets.add(lineDataSetAvgMinGoal)

        val lineDataSetAvgMaxGoal = LineDataSet(entriesAvgMaxGoal, "Average Max Goal")
        lineDataSetAvgMaxGoal.color = ContextCompat.getColor(this, R.color.colourSecondary)
        lineDataSetAvgMaxGoal.valueTextColor = ContextCompat.getColor(this, R.color.colourSecondary)
        lineDataSetAvgMaxGoal.lineWidth = 2f
        lineDataSetAvgMaxGoal.circleRadius = 4f
        lineDataSetAvgMaxGoal.setDrawValues(true)
        dataSets.add(lineDataSetAvgMaxGoal)

        val lineData = LineData(dataSets)
        lineChart.data = lineData

        //Configure X-Axis
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < xAxisLabels.size) {
                    xAxisLabels[index]
                } else {
                    ""
                }
            }
        }

        //Disable right Y-Axis
        lineChart.axisRight.isEnabled = false

        //Set description text
        lineChart.description.text = "Total Hours and Average Goals per Day"

        lineChart.invalidate()
    }
}
