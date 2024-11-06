package com.example.opsc_poe_task_app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class CreateQuest : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var categorySpinner: Spinner
    private lateinit var categoryAdapter: ArrayAdapter<String>

    private lateinit var minDailyGoalInput: EditText
    private lateinit var maxDailyGoalInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var daysOfWeek: List<CheckBox>
    private var selectedColor: String = "None"

    private val colorMap = mapOf(
        "Green" to android.graphics.Color.GREEN,
        "Blue" to android.graphics.Color.BLUE,
        "Yellow" to android.graphics.Color.YELLOW,
        "Purple" to android.graphics.Color.MAGENTA
    )

    private val datetimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_quest)

        //Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/categories")
        }

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

        //Date and time inputs
        val dateInput = findViewById<EditText>(R.id.dateInput)
        dateInput.setOnClickListener { showDatePickerDialog(dateInput) }

        val startTimeInput = findViewById<EditText>(R.id.startTimeInput)
        startTimeInput.setOnClickListener { showTimePickerDialog(startTimeInput) }

        val endTimeInput = findViewById<EditText>(R.id.endTimeInput)
        endTimeInput.setOnClickListener { showTimePickerDialog(endTimeInput) }

        //Spinner setup
        categorySpinner = findViewById(R.id.categorySelector)
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        loadCategories()

        //Listener for category selection
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()
                if (selected == "Add New Category") {
                    showAddCategoryDialog()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                //Do nothing
            }
        }

        //Daily goal inputs
        minDailyGoalInput = findViewById(R.id.minDailyGoalInput)
        maxDailyGoalInput = findViewById(R.id.maxDailyGoalInput)

        minDailyGoalInput.setOnClickListener {
            showNumberPickerDialog(minDailyGoalInput)
        }

        maxDailyGoalInput.setOnClickListener {
            showNumberPickerDialog(maxDailyGoalInput)
        }

        //Add TextWatchers for min and max goal inputs
        addGoalInputListeners()

        //Description input
        descriptionInput = findViewById(R.id.descriptionInput)

        //Days of the week checkboxes
        daysOfWeek = listOf(
            findViewById(R.id.mondayCheckBox),
            findViewById(R.id.tuesdayCheckBox),
            findViewById(R.id.wednesdayCheckBox),
            findViewById(R.id.thursdayCheckBox),
            findViewById(R.id.fridayCheckBox),
            findViewById(R.id.saturdayCheckBox),
            findViewById(R.id.sundayCheckBox)
        )

        //Difficulty selector
        val difficultyRatingBar = findViewById<RatingBar>(R.id.difficultyRatingBar)

        //Quest name input
        val questTitleInput = findViewById<EditText>(R.id.questTitleInput)

        //Colour selector button
        val selectColorButton = findViewById<Button>(R.id.selectColorButton)
        val colorDisplay = findViewById<TextView>(R.id.colorDisplay)

        selectColorButton.setOnClickListener {
            showColorPickerDialog(colorDisplay)
        }

        val addQuestButton = findViewById<Button>(R.id.AddQuestButton)
        addQuestButton.setOnClickListener {
            createQuest(questTitleInput.text.toString())
        }

        //validation listeners for date and time inputs
        addDateTimeInputListeners()
    }

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
                // Validate date immediately after selection
                validateDateInput(formattedDate, dateInput)
                validateTimeInput()
            },
            year,
            month,
            day
        )

        //Set minimum date to today
        datePickerDialog.datePicker.minDate = calendar.timeInMillis

        datePickerDialog.show()
    }

    private fun showTimePickerDialog(timeInput: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                timeInput.setText(formattedTime)
                //Validate time immediately after selection
                validateTimeInput()
            },
            hour,
            minute,
            true
        )

        timePickerDialog.show()
    }

    private fun loadCategories() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = currentUser.uid
        val userReference = FirebaseDatabase.getInstance().getReference("users/$uid/categories")

        userReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val categories = ArrayList<String>()

                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        var categoryName = snapshot.child("categoryName").getValue(String::class.java)
                        if (categoryName == null) {
                            //Fallback to the key if categoryName is missing
                            categoryName = snapshot.key
                        }
                        categoryName?.let { categories.add(it) }
                    }
                }

                if (categories.isEmpty()) {
                    categories.add("No categories found")
                }

                //option to add a new category
                categories.add("Add New Category")

                categoryAdapter.clear()
                categoryAdapter.addAll(categories)
                categoryAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@CreateQuest, "Failed to load categories.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddCategoryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Category")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val newCategory = input.text.toString().trim()
            if (newCategory.isNotEmpty()) {
                addNewCategory(newCategory)
            } else {
                Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun addNewCategory(newCategory: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val categoryReference = FirebaseDatabase.getInstance()
                .getReference("users/${currentUser.uid}/categories/$newCategory")
            categoryReference.child("categoryName").setValue(newCategory).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT).show()
                    loadCategories()
                    val position = categoryAdapter.getPosition(newCategory)
                    categorySpinner.setSelection(position)
                } else {
                    Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createQuest(questName: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCategory = categorySpinner.selectedItem.toString()
        val dateInputField = findViewById<EditText>(R.id.dateInput)
        val date = dateInputField.text.toString()
        val startTimeInputField = findViewById<EditText>(R.id.startTimeInput)
        val startTime = startTimeInputField.text.toString()
        val endTimeInputField = findViewById<EditText>(R.id.endTimeInput)
        val endTime = endTimeInputField.text.toString()
        val difficulty = findViewById<RatingBar>(R.id.difficultyRatingBar).rating.toInt()
        val description = descriptionInput.text.toString().trim()

        //Check which days of the week are selected
        val selectedDays = daysOfWeek.filter { it.isChecked }.map { it.text.toString() }

        val minGoal = minDailyGoalInput.text.toString().toIntOrNull()
        val maxGoal = maxDailyGoalInput.text.toString().toIntOrNull()

        //Validate required fields
        if (questName.isEmpty()) {
            Toast.makeText(this, "Please enter a quest name", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategory == "No categories found" || selectedCategory == "Add New Category") {
            Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show()
            return
        }

        if (minGoal == null || maxGoal == null || minDailyGoalInput.error != null || maxDailyGoalInput.error != null) {
            Toast.makeText(this, "Please enter valid min and max goals", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedColor == "None") {
            Toast.makeText(this, "Please select a color", Toast.LENGTH_SHORT).show()
            return
        }

        if (date.isEmpty() || dateInputField.error != null) {
            Toast.makeText(this, "Please select a valid date", Toast.LENGTH_SHORT).show()
            return
        }

        if (startTime.isEmpty() || endTime.isEmpty() || startTimeInputField.error != null || endTimeInputField.error != null) {
            Toast.makeText(this, "Please select valid start and end times", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day of the week", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new quest map to store quest data
        val questData = mapOf(
            "name" to questName,
            "description" to description,
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime,
            "difficulty" to difficulty,
            "minGoal" to minGoal,
            "maxGoal" to maxGoal,
            "daysOfWeek" to selectedDays,
            "color" to selectedColor
        )

        val categoryReference = FirebaseDatabase.getInstance()
            .getReference("users/${currentUser.uid}/categories/$selectedCategory")

        //Set the categoryName if it doesn't exist
        categoryReference.child("categoryName").setValue(selectedCategory)

        //Store the quest under the selected category
        val questsReference = categoryReference.child("quests")
        val questId = questsReference.push().key
        if (questId != null) {
            questsReference.child(questId).setValue(questData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Quest added successfully!", Toast.LENGTH_SHORT).show()
                    clearFields()
                } else {
                    Toast.makeText(this, "Failed to add quest", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Failed to generate quest ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFields() {
        findViewById<EditText>(R.id.questTitleInput).text.clear()
        findViewById<EditText>(R.id.dateInput).text.clear()
        findViewById<EditText>(R.id.startTimeInput).text.clear()
        findViewById<EditText>(R.id.endTimeInput).text.clear()
        findViewById<RatingBar>(R.id.difficultyRatingBar).rating = 1f
        descriptionInput.text.clear()
        daysOfWeek.forEach { it.isChecked = false }
        findViewById<TextView>(R.id.colorDisplay).text = "Selected Color: None"
        selectedColor = "None"
        minDailyGoalInput.text.clear()
        maxDailyGoalInput.text.clear()
        minDailyGoalInput.error = null
        maxDailyGoalInput.error = null
        findViewById<EditText>(R.id.dateInput).error = null
        findViewById<EditText>(R.id.startTimeInput).error = null
        findViewById<EditText>(R.id.endTimeInput).error = null
    }

    private fun showColorPickerDialog(colorDisplay: TextView) {
        val colorNames = colorMap.keys.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Quest Color")
            .setItems(colorNames) { _, which ->
                val selectedColorName = colorNames[which]
                selectedColor = selectedColorName

                colorDisplay.text = "Selected Color: $selectedColorName"
            }
            .show()
    }

    private fun showNumberPickerDialog(goalInput: EditText) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_number_picker, null)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)

        numberPicker.minValue = 1  // Set minimum value to 1
        numberPicker.maxValue = 24
        numberPicker.wrapSelectorWheel = false

        AlertDialog.Builder(this)
            .setTitle("Select Hours")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                goalInput.setText(numberPicker.value.toString())
                dialog.dismiss()
                //Validate goals immediately after selection
                validateGoalInputs()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun addGoalInputListeners() {
        minDailyGoalInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateGoalInputs()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //Do nothing
            }
        })

        maxDailyGoalInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateGoalInputs()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //Do nothing
            }
        })
    }

    private fun validateGoalInputs() {
        val minGoal = minDailyGoalInput.text.toString().toIntOrNull()
        val maxGoal = maxDailyGoalInput.text.toString().toIntOrNull()

        minDailyGoalInput.error = null
        maxDailyGoalInput.error = null

        if (minGoal != null && maxGoal != null) {
            if (minGoal > maxGoal) {
                minDailyGoalInput.error = "Min goal cannot be greater than max goal"
            }
            if (minGoal < 1) {
                minDailyGoalInput.error = "Min goal must be at least 1"
            }
            if (maxGoal < 1) {
                maxDailyGoalInput.error = "Max goal must be at least 1"
            }
        } else {
            if (minGoal == null) {
                minDailyGoalInput.error = "Enter a valid number"
            }
            if (maxGoal == null) {
                maxDailyGoalInput.error = "Enter a valid number"
            }
        }
    }

    private fun addDateTimeInputListeners() {
        val dateInputField = findViewById<EditText>(R.id.dateInput)
        val startTimeInputField = findViewById<EditText>(R.id.startTimeInput)
        val endTimeInputField = findViewById<EditText>(R.id.endTimeInput)

        //Add listeners to validate date and time inputs when they change
        dateInputField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateDateInput(dateInputField.text.toString(), dateInputField)
                validateTimeInput()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //Do nothing
            }
        })

        startTimeInputField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateTimeInput()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //Do nothing
            }
        })

        endTimeInputField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateTimeInput()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //Do nothing
            }
        })
    }

    private fun validateDateInput(dateString: String, dateInputField: EditText) {
        dateInputField.error = null

        val calendarCurrentDate = Calendar.getInstance()
        val calendarSelectedDate = Calendar.getInstance()

        try {
            val selectedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString)
            if (selectedDate != null) {
                calendarSelectedDate.time = selectedDate

                //Reset time components to zero for both calendars
                calendarCurrentDate.set(Calendar.HOUR_OF_DAY, 0)
                calendarCurrentDate.set(Calendar.MINUTE, 0)
                calendarCurrentDate.set(Calendar.SECOND, 0)
                calendarCurrentDate.set(Calendar.MILLISECOND, 0)

                calendarSelectedDate.set(Calendar.HOUR_OF_DAY, 0)
                calendarSelectedDate.set(Calendar.MINUTE, 0)
                calendarSelectedDate.set(Calendar.SECOND, 0)
                calendarSelectedDate.set(Calendar.MILLISECOND, 0)

                if (calendarSelectedDate.before(calendarCurrentDate)) {
                    dateInputField.error = "Date cannot be in the past"
                }
            } else {
                dateInputField.error = "Invalid date format"
            }
        } catch (e: ParseException) {
            dateInputField.error = "Invalid date format"
        }
    }

    private fun validateTimeInput() {
        val dateInputField = findViewById<EditText>(R.id.dateInput)
        val startTimeInputField = findViewById<EditText>(R.id.startTimeInput)
        val endTimeInputField = findViewById<EditText>(R.id.endTimeInput)

        startTimeInputField.error = null
        endTimeInputField.error = null

        val dateString = dateInputField.text.toString()
        val startTimeString = startTimeInputField.text.toString()
        val endTimeString = endTimeInputField.text.toString()

        if (dateString.isEmpty() || startTimeString.isEmpty() || endTimeString.isEmpty()) {
            return
        }

        try {
            val startDateTime = datetimeFormat.parse("$dateString $startTimeString")
            val endDateTime = datetimeFormat.parse("$dateString $endTimeString")

            if (startDateTime != null && endDateTime != null) {
                val currentDateTime = Calendar.getInstance().time

                if (isSameDay(currentDateTime, startDateTime)) {
                    if (startDateTime.before(currentDateTime)) {
                        startTimeInputField.error = "Start time cannot be in the past"
                    }
                }

                if (startDateTime.after(endDateTime)) {
                    startTimeInputField.error = "Start time cannot be after end time"
                    endTimeInputField.error = "End time cannot be before start time"
                }
            }
        } catch (e: ParseException) {
            //Handle invalid date/time formats
            startTimeInputField.error = "Invalid time format"
            endTimeInputField.error = "Invalid time format"
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
