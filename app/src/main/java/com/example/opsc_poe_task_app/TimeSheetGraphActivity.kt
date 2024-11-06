package com.example.opsc_poe_task_app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter

class TimeSheetGraphActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var startDateInput: EditText
    private lateinit var endDateInput: EditText
    private var startDateFilter: Date? = null
    private var endDateFilter: Date? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

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

        startDateInput.setOnClickListener {
            showDatePickerDialog(startDateInput, true)
        }

        endDateInput.setOnClickListener {
            showDatePickerDialog(endDateInput, false)
        }

        //Load and display the graph
        loadAndDisplayGraph()
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
                loadAndDisplayGraph()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun loadAndDisplayGraph() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        //Show a loading indicator if needed

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
        val entries = ArrayList<Entry>()
        val xAxisLabels = ArrayList<String>()

        for ((index, date) in sortedDates.withIndex()) {
            val dateStr = dateFormat.format(date)
            val hours = dateHoursMap[dateStr] ?: 0.0
            entries.add(Entry(index.toFloat(), hours.toFloat()))
            xAxisLabels.add(dateStr)
        }

        val lineDataSet = LineDataSet(entries, "Total Hours per Day")
        lineDataSet.color = resources.getColor(R.color.colourPrimary)
        lineDataSet.valueTextColor = resources.getColor(R.color.colourPrimaryDark)
        lineDataSet.lineWidth = 2f
        lineDataSet.circleRadius = 4f
        lineDataSet.setDrawValues(true)

        val lineData = LineData(lineDataSet)
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
        lineChart.description.text = "Total Hours Worked per Day"

        //Refresh the chart
        lineChart.invalidate()
    }
}
