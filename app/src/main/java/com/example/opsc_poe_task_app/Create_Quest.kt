package com.example.opsc_poe_task_app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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
            },
            year,
            month,
            day
        )

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

                //Add option to add a new category
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
                //Add the new category
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
                    //Set the spinner to the new category
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
        val date = findViewById<EditText>(R.id.dateInput).text.toString()
        val startTime = findViewById<EditText>(R.id.startTimeInput).text.toString()
        val endTime = findViewById<EditText>(R.id.endTimeInput).text.toString()
        val difficulty = findViewById<RatingBar>(R.id.difficultyRatingBar).rating.toInt()
        val description = descriptionInput.text.toString().trim()

        //Check which days of the week are selected
        val selectedDays = daysOfWeek.filter { it.isChecked }.map { it.text.toString() }

        val minGoal = minDailyGoalInput.text.toString().toIntOrNull()
        val maxGoal = maxDailyGoalInput.text.toString().toIntOrNull()

        if (questName.isEmpty() || selectedCategory == "No categories found" || selectedCategory == "Add New Category" || minGoal == null || maxGoal == null || selectedColor == "None") {
            Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        //Create a new quest map to store quest data
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

        numberPicker.minValue = 0
        numberPicker.maxValue = 24
        numberPicker.wrapSelectorWheel = false

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
