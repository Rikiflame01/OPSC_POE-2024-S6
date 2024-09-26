package com.example.opsc_poe_task_app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class TimeSheet : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_sheet)

        val questBoardButton = findViewById<Button>(R.id.questBoardButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val createQuestButton = findViewById<Button>(R.id.createQuestButton)
        val addTimesheetEntryButton = findViewById<FloatingActionButton>(R.id.addTimesheetEntryButton)

        questBoardButton.setOnClickListener {
            val intent = Intent(this, QuestBoard::class.java)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        createQuestButton.setOnClickListener {
            val intent = Intent(this, CreateQuest::class.java)
            startActivity(intent)
        }

        //Show the dialog to add a timesheet entry when the FAB is clicked
        addTimesheetEntryButton.setOnClickListener {
            showAddTimesheetDialog()
        }
    }

    private fun showAddTimesheetDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_timesheet_entry, null)

        val dateInput = dialogView.findViewById<EditText>(R.id.dateInput)
        val startTimeInput = dialogView.findViewById<EditText>(R.id.startTimeInput)
        val endTimeInput = dialogView.findViewById<EditText>(R.id.endTimeInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.descriptionInput)
        val categoryInput = dialogView.findViewById<EditText>(R.id.categoryInput)
        val addPhotoButton = dialogView.findViewById<Button>(R.id.addPhotoButton)

        dateInput.setOnClickListener {
            showDatePickerDialog(dateInput)
        }

        startTimeInput.setOnClickListener {
            showTimePickerDialog(startTimeInput)
        }

        endTimeInput.setOnClickListener {
            showTimePickerDialog(endTimeInput)
        }

        addPhotoButton.setOnClickListener {
            openImagePicker()
        }

        //Build and show the dialog
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Timesheet Entry")
            .setPositiveButton("Save") { dialog, _ ->
                //Validation
                val date = dateInput.text.toString()
                val startTime = startTimeInput.text.toString()
                val endTime = endTimeInput.text.toString()
                val description = descriptionInput.text.toString()
                val category = categoryInput.text.toString()

                if (date.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || description.isEmpty() || category.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    saveTimesheetEntry(date, startTime, endTime, description, category)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    //Function to open the image picker
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    //Function to show the Date Picker Dialog
    private fun showDatePickerDialog(dateInput: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            dateInput.setText(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    //Function to show the Time Picker Dialog
    private fun showTimePickerDialog(timeInput: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            timeInput.setText(selectedTime)
        }, hour, minute, true)

        timePickerDialog.show()
    }

    //Function to save the timesheet entry
    private fun saveTimesheetEntry(date: String, startTime: String, endTime: String, description: String, category: String) {
        //To be implemented....
        Toast.makeText(this, "Timesheet entry saved", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data

            Toast.makeText(this, "Image selected: $imageUri", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val REQUEST_IMAGE_PICK = 1
    }
}
