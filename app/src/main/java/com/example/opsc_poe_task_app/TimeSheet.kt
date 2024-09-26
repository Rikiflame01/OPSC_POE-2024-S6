package com.example.opsc_poe_task_app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TimeSheet : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var categorySpinner: Spinner
    private lateinit var categoryAdapter: ArrayAdapter<String>

    private var imageUri: Uri? = null

    private lateinit var timesheetRecyclerView: RecyclerView
    private lateinit var timesheetAdapter: TimeSheetAdapter
    private val timesheetEntries = ArrayList<TimeSheetEntry>()

    private lateinit var totalHoursValue: TextView

    //Date range filtering
    private lateinit var startDateInput: EditText
    private lateinit var endDateInput: EditText
    private var startDateFilter: Date? = null
    private var endDateFilter: Date? = null

    private lateinit var dialogView: View

    companion object {
        const val REQUEST_IMAGE_PICK = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //content view to timesheet activity layout
        setContentView(R.layout.activity_time_sheet)

        //Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database =
                FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/categories")
        }

        //Initialize UI elements
        val questBoardButton = findViewById<Button>(R.id.questBoardButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val createQuestButton = findViewById<Button>(R.id.createQuestButton)
        val addTimesheetEntryButton =
            findViewById<FloatingActionButton>(R.id.addTimesheetEntryButton)

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

        //Initialize RecyclerView
        timesheetRecyclerView = findViewById(R.id.timesheetRecyclerView)
        timesheetRecyclerView.layoutManager = LinearLayoutManager(this)
        timesheetAdapter = TimeSheetAdapter(this, timesheetEntries)
        timesheetRecyclerView.adapter = timesheetAdapter

        //Total hours TextView
        totalHoursValue = findViewById(R.id.totalHoursValue)

        //Initialize date range inputs
        startDateInput = findViewById(R.id.startDateInput)
        endDateInput = findViewById(R.id.endDateInput)

        startDateInput.setOnClickListener {
            showDatePickerDialog(startDateInput, true)
        }

        endDateInput.setOnClickListener {
            showDatePickerDialog(endDateInput, false)
        }

        //Load timesheet entries
        loadTimeSheetEntries()

        //Show the dialog to add a timesheet entry when the FAB is clicked
        addTimesheetEntryButton.setOnClickListener {
            showAddTimesheetDialog()
        }
    }

    private fun loadTimeSheetEntries() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryHoursMap = mutableMapOf<String, Double>()

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                timesheetEntries.clear()
                categoryHoursMap.clear()

                for (categorySnapshot in snapshot.children) {
                    val categoryName =
                        categorySnapshot.child("categoryName").getValue(String::class.java)?.trim()
                    val timesheetEntriesSnapshot = categorySnapshot.child("TimesheetEntries")

                    if (categoryName == null) {
                        Log.e("TimeSheet", "Category without categoryName field. Skipping.")
                        continue
                    }

                    for (entrySnapshot in timesheetEntriesSnapshot.children) {
                        val entry = entrySnapshot.getValue(TimeSheetEntry::class.java)
                        if (entry != null) {
                            //Parse the entry date
                            val entryDate = parseDate(entry.date)
                            if (entryDate != null) {
                                //Apply date filters
                                val isWithinStartDate = startDateFilter?.let { !entryDate.before(it) } ?: true
                                val isWithinEndDate = endDateFilter?.let { !entryDate.after(it) } ?: true

                                if (isWithinStartDate && isWithinEndDate) {
                                    entry.categoryName = categoryName
                                    timesheetEntries.add(entry)
                                    val entryHours = entry.getDurationInHours()

                                    //Add to category hours
                                    categoryHoursMap[categoryName] =
                                        categoryHoursMap.getOrDefault(categoryName, 0.0) + entryHours
                                }
                            }
                        }
                    }
                }

                timesheetAdapter.notifyDataSetChanged()

                //Display total hours per category
                if (categoryHoursMap.isNotEmpty()) {
                    val categoryHoursText = categoryHoursMap.entries.joinToString(separator = "\n") { (category, hours) ->
                        "$category: ${String.format("%.2f", hours)} hrs"
                    }
                    totalHoursValue.text = categoryHoursText
                } else {
                    totalHoursValue.text = "No Data"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TimeSheet", "Failed to load timesheet entries: ${error.message}")
            }
        })
    }

    private fun parseDate(dateStr: String): Date? {
        return try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun showAddTimesheetDialog() {
        dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_timesheet_entry, null)

        val dateInput = dialogView.findViewById<EditText>(R.id.dateInput)
        val startTimeInput = dialogView.findViewById<EditText>(R.id.startTimeInput)
        val endTimeInput = dialogView.findViewById<EditText>(R.id.endTimeInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.descriptionInput)
        val addPhotoButton = dialogView.findViewById<Button>(R.id.addPhotoButton)

        //category spinner
        categorySpinner = dialogView.findViewById(R.id.categorySpinner)
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        loadCategoriesIntoSpinner()

        //Date and time pickers
        dateInput.setOnClickListener {
            showDatePickerDialog(dateInput, null)
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
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Timesheet Entry")
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()

        //Override the positive button to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            //validation
            val date = dateInput.text.toString()
            val startTime = startTimeInput.text.toString()
            val endTime = endTimeInput.text.toString()
            val description = descriptionInput.text.toString()
            val selectedCategory = categorySpinner.selectedItem.toString()

            if (date.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || description.isEmpty() || selectedCategory == "No categories found") {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                //Save the timesheet entry
                saveTimesheetEntry(date, startTime, endTime, description, selectedCategory)
                dialog.dismiss()
            }
        }
    }

    private fun loadCategoriesIntoSpinner() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = currentUser.uid
        val userReference = FirebaseDatabase.getInstance().getReference("users/$uid/categories")

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val categories = ArrayList<String>()

                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        var categoryName = snapshot.child("categoryName").getValue(String::class.java)
                        if (categoryName == null) {
                            //Fallback
                            categoryName = snapshot.key
                        }
                        categoryName?.let { categories.add(it) }
                    }
                }

                if (categories.isEmpty()) {
                    categories.add("No categories found")
                }

                //Add option to add a new category(same as the spinner in the create quest activity)
                categories.add("Add New Category")

                categoryAdapter.clear()
                categoryAdapter.addAll(categories)
                categoryAdapter.notifyDataSetChanged()

                //Set up listener for "Add New Category" option
                categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selected = parent.getItemAtPosition(position).toString()
                        if (selected == "Add New Category") {
                            showAddCategoryDialog()
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        //Do nothing
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@TimeSheet, "Failed to load categories.", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun showDatePickerDialog(dateInput: EditText, isStartDate: Boolean?) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDateStr = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            dateInput.setText(selectedDateStr)

            //Parse the selected date
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
            when (isStartDate) {
                true -> {
                    startDateFilter = selectedDate.time
                }
                false -> {
                    endDateFilter = selectedDate.time
                }
                null -> {
                    //Do nothing for date inputs in the dialog
                }
            }

            //Reload entries with the new filters
            if (isStartDate != null) {
                loadTimeSheetEntries()
            }

        }, year, month, day)

        datePickerDialog.show()
    }

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

    private fun saveTimesheetEntry(
        date: String,
        startTime: String,
        endTime: String,
        description: String,
        categoryName: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val categoryReference = FirebaseDatabase.getInstance()
                .getReference("users/${currentUser.uid}/categories/$categoryName")

            //Ensure the categoryName field is set
            categoryReference.child("categoryName").setValue(categoryName)

            //Prepare the timesheet entry data
            val timesheetEntryData = HashMap<String, Any>()
            timesheetEntryData["date"] = date
            timesheetEntryData["startTime"] = startTime
            timesheetEntryData["endTime"] = endTime
            timesheetEntryData["description"] = description

            //Handle image upload if an image was selected
            if (imageUri != null) {
                val storageReference = FirebaseStorage.getInstance().reference.child("timesheet_images/${UUID.randomUUID()}")
                val uploadTask = storageReference.putFile(imageUri!!)

                uploadTask.addOnSuccessListener {
                    //Get the download URL of the uploaded image
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        val photoUrl = uri.toString()
                        timesheetEntryData["photoUrl"] = photoUrl

                        //save the timesheet entry data to the database
                        saveEntryToDatabase(categoryReference, timesheetEntryData)
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to get photo URL", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show()
                }
            } else {
                //No image selected, save the data directly
                saveEntryToDatabase(categoryReference, timesheetEntryData)
            }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveEntryToDatabase(
        categoryReference: DatabaseReference,
        timesheetEntryData: HashMap<String, Any>
    ) {
        val timesheetEntriesReference = categoryReference.child("TimesheetEntries")
        val entryId = timesheetEntriesReference.push().key
        if (entryId != null) {
            timesheetEntriesReference.child(entryId).setValue(timesheetEntryData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Timesheet entry saved", Toast.LENGTH_SHORT).show()
                        imageUri = null
                    } else {
                        Toast.makeText(this, "Failed to save timesheet entry", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }
    }

    //Function to open the image picker
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            imageUri = data.data

            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
        }
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
            categoryReference.child("categoryName").setValue(newCategory)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT)
                            .show()
                        loadCategoriesIntoSpinner()
                        val position = categoryAdapter.getPosition(newCategory)
                        categorySpinner.setSelection(position)
                    } else {
                        Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
