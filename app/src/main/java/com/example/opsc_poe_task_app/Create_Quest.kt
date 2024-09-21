package com.example.opsc_poe_task_app

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import android.net.Uri
import android.view.LayoutInflater
import android.widget.NumberPicker
import android.widget.RatingBar

class CreateQuest : AppCompatActivity() {

    private lateinit var minDailyGoalInput: EditText
    private lateinit var maxDailyGoalInput: EditText

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    private val colorMap = mapOf(
        "Red" to Color.RED,
        "Green" to Color.GREEN,
        "Blue" to Color.BLUE,
        "Yellow" to Color.YELLOW,
        "Black" to Color.BLACK
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_quest)

        //Tab buttons
        val questBoardButton = findViewById<Button>(R.id.questBoardButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val createQuestButton = findViewById<Button>(R.id.createQuestButton)

        questBoardButton.setOnClickListener {
            val intent = Intent(this, QuestBoard::class.java)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        createQuestButton.setOnClickListener {
            //Already on Create Quest screen, no action needed
        }

        // Date and time inputs (already implemented)
        val dateInput = findViewById<EditText>(R.id.dateInput)
        dateInput.setOnClickListener { showDatePickerDialog(dateInput) }

        val startTimeInput = findViewById<EditText>(R.id.startTimeInput)
        startTimeInput.setOnClickListener { showTimePickerDialog(startTimeInput) }

        val endTimeInput = findViewById<EditText>(R.id.endTimeInput)
        endTimeInput.setOnClickListener { showTimePickerDialog(endTimeInput) }

        //Colour selector button
        val selectColorButton = findViewById<Button>(R.id.selectColorButton)
        val colorDisplay = findViewById<TextView>(R.id.colorDisplay)

        selectColorButton.setOnClickListener {
            showColorPickerDialog(colorDisplay)
        }

        //Find the category selector (Spinner)
        val categorySpinner = findViewById<Spinner>(R.id.categorySelector)

        //ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.category_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            //Apply the adapter to the spinner
            categorySpinner.adapter = adapter
        }

        val uploadPhotoButton = findViewById<Button>(R.id.uploadPhotoButton)

        uploadPhotoButton.setOnClickListener {
            //Launch the photo picker
            openPhotoPicker()
        }

        minDailyGoalInput = findViewById(R.id.minDailyGoalInput)
        maxDailyGoalInput = findViewById(R.id.maxDailyGoalInput)

        minDailyGoalInput.setOnClickListener {
            showNumberPickerDialog(minDailyGoalInput)
        }

        maxDailyGoalInput.setOnClickListener {
            showNumberPickerDialog(maxDailyGoalInput)
        }

        val difficultyRatingBar = findViewById<RatingBar>(R.id.difficultyRatingBar)

        difficultyRatingBar.setOnRatingBarChangeListener { ratingBar, rating, _ ->
            Toast.makeText(this, "Selected Difficulty: $rating stars", Toast.LENGTH_SHORT).show()
        }
    }

    //Function to show the DatePickerDialog
    private fun showDatePickerDialog(dateInput: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val formattedDate = "$selectedDayOfMonth/${selectedMonth + 1}/$selectedYear"
                dateInput.setText(formattedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }

    //Function to show the TimePickerDialog
    private fun showTimePickerDialog(timeInput: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                timeInput.setText(formattedTime)
            },
            hour,
            minute,
            true
        )

        timePickerDialog.show()
    }

    //Function to show the Colour Picker Dialog
    private fun showColorPickerDialog(colorDisplay: TextView) {
        val colorNames = colorMap.keys.toTypedArray()

        //Show a dialog with colour names
        AlertDialog.Builder(this)
            .setTitle("Select Category Color")
            .setItems(colorNames) { _, which ->
                val selectedColorName = colorNames[which]
                val selectedColor = colorMap[selectedColorName] ?: Color.BLACK

                //Update the TextView to show the selected color
                colorDisplay.text = "Selected Color: $selectedColorName"
                colorDisplay.setTextColor(selectedColor)
            }
            .show()
    }

    private fun openPhotoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    //Handle the result when the user selects a photo
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.data != null) {
                selectedImageUri = data.data

                Toast.makeText(this, "Photo selected!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Function to show the NumberPicker dialog
    private fun showNumberPickerDialog(goalInput: EditText) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_number_picker, null)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)

        //Set the range of hours (e.g., 0 to 24 hours)
        numberPicker.minValue = 0
        numberPicker.maxValue = 24
        numberPicker.wrapSelectorWheel = false

        //Create and show the dialog
        AlertDialog.Builder(this)
            .setTitle("Select Hours")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                goalInput.setText(numberPicker.value.toString())
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}
