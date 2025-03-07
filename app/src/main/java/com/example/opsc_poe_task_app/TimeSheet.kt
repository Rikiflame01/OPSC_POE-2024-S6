package com.example.opsc_poe_task_app

import android.Manifest
import androidx.lifecycle.lifecycleScope
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class TimeSheet : AppCompatActivity() {

    private lateinit var totalCategoriesValue: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var categorySpinner: Spinner
    private lateinit var categoryAdapter: ArrayAdapter<String>

    private var imageUri: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraImageUri: Uri
    private var imageFilePath: String = ""

    private lateinit var timesheetRecyclerView: RecyclerView
    private lateinit var timesheetAdapter: TimeSheetAdapter
    private val timesheetEntries = mutableListOf<TimeSheetEntry>()

    private lateinit var totalHoursValue: TextView

    private lateinit var startDateInput: EditText
    private lateinit var endDateInput: EditText
    private var startDateFilter: Date? = null
    private var endDateFilter: Date? = null

    private lateinit var dialogView: View

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)

    private val CAMERA_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_sheet)

        totalCategoriesValue = findViewById(R.id.totalCategoriesValue)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/categories")
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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

        timesheetRecyclerView = findViewById(R.id.timesheetRecyclerView)
        timesheetRecyclerView.layoutManager = LinearLayoutManager(this)
        timesheetAdapter = TimeSheetAdapter(this, timesheetEntries)
        timesheetRecyclerView.adapter = timesheetAdapter

        totalHoursValue = findViewById(R.id.totalHoursValue)

        startDateInput = findViewById(R.id.startDateInput)
        endDateInput = findViewById(R.id.endDateInput)

        startDateInput.setOnClickListener {
            showDatePickerDialog(startDateInput, true)
        }

        endDateInput.setOnClickListener {
            showDatePickerDialog(endDateInput, false)
        }

        initializeActivityResultLaunchers()

        loadTimeSheetEntries()

        addTimesheetEntryButton.setOnClickListener {
            showAddTimesheetDialog()
        }
    }

    private fun initializeActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                imageUri = result.data?.data

                if (imageUri != null) {
                    Log.d("TimeSheet", "Image URI: $imageUri")
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()

                    dialogView.findViewById<ImageView>(R.id.selectedImageView).apply {
                        visibility = View.VISIBLE
                        Glide.with(this@TimeSheet)
                            .load(imageUri)
                            .into(this)
                    }

                    dialogView.findViewById<Button>(R.id.removePhotoButton).visibility = View.VISIBLE
                } else {
                    Log.e("TimeSheet", "Image URI is null")
                }
            } else {
                Log.e("TimeSheet", "Image selection canceled or failed")
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imageUri = cameraImageUri

                if (imageUri != null) {
                    Log.d("TimeSheet", "Camera Image URI: $imageUri")
                    Toast.makeText(this, "Photo taken", Toast.LENGTH_SHORT).show()

                    dialogView.findViewById<ImageView>(R.id.selectedImageView).apply {
                        visibility = View.VISIBLE
                        Glide.with(this@TimeSheet)
                            .load(imageUri)
                            .into(this)
                    }

                    dialogView.findViewById<Button>(R.id.removePhotoButton).visibility = View.VISIBLE
                } else {
                    Log.e("TimeSheet", "Camera Image URI is null")
                }
            } else {
                Log.e("TimeSheet", "Photo capture canceled or failed")
            }
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
                lifecycleScope.launch {
                    timesheetEntries.clear()
                    categoryHoursMap.clear()

                    for (categorySnapshot in snapshot.children) {
                        val categoryName = categorySnapshot.child("categoryName").getValue(String::class.java)?.trim()
                        val timesheetEntriesSnapshot = categorySnapshot.child("TimesheetEntries")

                        if (categoryName == null) {
                            Log.e("TimeSheet", "Category without categoryName field. Skipping.")
                            continue
                        }

                        for (entrySnapshot in timesheetEntriesSnapshot.children) {
                            val entry = entrySnapshot.getValue(TimeSheetEntry::class.java)
                            if (entry != null) {
                                entry.entryId = entrySnapshot.key ?: ""
                                entry.categoryName = categoryName

                                val entryDate = parseDate(entry.date)
                                if (entryDate != null) {
                                    val isWithinStartDate = startDateFilter?.let { !entryDate.before(it) } ?: true
                                    val isWithinEndDate = endDateFilter?.let { !entryDate.after(it) } ?: true

                                    if (isWithinStartDate && isWithinEndDate) {
                                        timesheetEntries.add(entry)
                                        val entryHours = entry.getDurationInHours()
                                        categoryHoursMap[categoryName] = categoryHoursMap.getOrDefault(categoryName, 0.0) + entryHours
                                    }
                                } else {
                                    Log.e("TimeSheet", "Failed to parse entry date: ${entry.date}")
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        timesheetAdapter.notifyDataSetChanged()

                        //Update the total category count after counting
                        totalCategoriesValue.text = categoryHoursMap.size.toString()
                        Log.d("TimeSheet", "Total categories displayed in UI: ${categoryHoursMap.size}")

                        if (categoryHoursMap.isNotEmpty()) {
                            val categoryHoursText = categoryHoursMap.entries.joinToString(separator = "\n") { (category, hours) ->
                                "$category: ${String.format("%.2f", hours)} hrs"
                            }
                            totalHoursValue.text = categoryHoursText
                        } else {
                            totalHoursValue.text = "No Data"
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TimeSheet", "Failed to load timesheet entries: ${error.message}")
                Toast.makeText(this@TimeSheet, "Failed to load timesheet entries: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun parseDate(dateStr: String): Date? {
        return try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            Log.e("TimeSheet", "Failed to parse date: $dateStr", e)
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
        val removePhotoButton = dialogView.findViewById<Button>(R.id.removePhotoButton)
        val selectedImageView = dialogView.findViewById<ImageView>(R.id.selectedImageView)

        val progressOverlay = dialogView.findViewById<FrameLayout>(R.id.progressOverlay)
        val uploadProgressBar = dialogView.findViewById<ProgressBar>(R.id.uploadProgressBar)
        val progressText = dialogView.findViewById<TextView>(R.id.progressText)

        categorySpinner = dialogView.findViewById(R.id.categorySpinner)
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        loadCategoriesIntoSpinner()

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parent.getItemAtPosition(position).toString()
                if (selectedCategory == "Add New Category") {
                    categorySpinner.setSelection(0)
                    showAddCategoryDialog()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed
            }
        }

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

        removePhotoButton.setOnClickListener {
            imageUri = null
            selectedImageView.visibility = View.GONE
            removePhotoButton.visibility = View.GONE
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Timesheet Entry")
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()

        val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        saveButton.setOnClickListener {
            dateInput.error = null
            startTimeInput.error = null
            endTimeInput.error = null
            descriptionInput.error = null

            val date = dateInput.text.toString()
            val startTime = startTimeInput.text.toString()
            val endTime = endTimeInput.text.toString()
            val description = descriptionInput.text.toString()
            val selectedCategory = categorySpinner.selectedItem.toString()
            var valid = true

            val errorMessages = mutableListOf<String>()

            if (date.isEmpty()) {
                dateInput.error = "Please select a date"
                errorMessages.add("Please select a date")
                valid = false
            }
            if (startTime.isEmpty()) {
                startTimeInput.error = "Please select a start time"
                errorMessages.add("Please select a start time")
                valid = false
            }
            if (endTime.isEmpty()) {
                endTimeInput.error = "Please select an end time"
                errorMessages.add("Please select an end time")
                valid = false
            }
            if (description.isEmpty()) {
                descriptionInput.error = "Please enter a description"
                errorMessages.add("Please enter a description")
                valid = false
            }
            if (selectedCategory == "No categories found" || selectedCategory == "Add New Category") {
                Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show()
                valid = false
            }

            if (valid) {
                val dateValid = validateDate(date)
                val timesValid = validateTimes(startTime, endTime)

                if (!dateValid) {
                    dateInput.error = "Invalid date"
                    errorMessages.add("Invalid date. Date cannot be in the future.")
                    valid = false
                }
                if (!timesValid) {
                    startTimeInput.error = "Start time must be before end time"
                    endTimeInput.error = "End time must be after start time"
                    errorMessages.add("Start time must be before end time")
                    valid = false
                }
            }

            if (valid) {
                progressOverlay.visibility = View.VISIBLE

                saveButton.isEnabled = false
                addPhotoButton.isEnabled = false
                categorySpinner.isEnabled = false

                saveTimesheetEntry(
                    date,
                    startTime,
                    endTime,
                    description,
                    selectedCategory,
                    progressOverlay,
                    saveButton
                )
            } else {
                Toast.makeText(this, errorMessages.joinToString("\n"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateDate(dateStr: String): Boolean {
        return try {
            val date = dateFormat.parse(dateStr)
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val selectedDate = Calendar.getInstance()
            selectedDate.time = date

            !selectedDate.after(today.time)
        } catch (e: ParseException) {
            false
        }
    }

    private fun validateTimes(startTimeStr: String, endTimeStr: String): Boolean {
        return try {
            val startTime = timeFormat.parse(startTimeStr)
            val endTime = timeFormat.parse(endTimeStr)
            startTime.before(endTime)
        } catch (e: ParseException) {
            false
        }
    }

    private fun loadCategoriesIntoSpinner() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("TimeSheet", "User not logged in")
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = currentUser.uid
        val userReference = FirebaseDatabase.getInstance().getReference("users/$uid/categories")
        Log.d("TimeSheet", "Attempting to load categories from Firebase for user: $uid")

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d("TimeSheet", "onDataChange triggered in loadCategoriesIntoSpinner. Data snapshot exists: ${dataSnapshot.exists()}")

                val categories = ArrayList<String>()
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        val categoryName = snapshot.key
                        Log.d("TimeSheet", "Found category: $categoryName")
                        categoryName?.let {
                            categories.add(it)
                        }
                    }
                } else {
                    Log.d("TimeSheet", "No categories found in the data snapshot")
                }

                // Update category count and UI
                val categoryCount = categories.size
                Log.d("TimeSheet", "Total categories counted: $categoryCount")

                runOnUiThread {
                    totalCategoriesValue.text = categoryCount.toString()
                }

                if (categoryCount == 0) {
                    categories.add("No categories found")
                }

                categories.add("Add New Category")
                categoryAdapter.clear()
                categoryAdapter.addAll(categories)
                categoryAdapter.notifyDataSetChanged()

                Log.d("TimeSheet", "Category spinner populated with ${categories.size} items")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TimeSheet", "Failed to load categories: ${databaseError.message}")
                Toast.makeText(this@TimeSheet, "Failed to load categories: ${databaseError.message}", Toast.LENGTH_LONG).show()
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
            val categoryReference = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/categories/$newCategory")
            categoryReference.child("categoryName").setValue(newCategory)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT).show()
                        loadCategoriesIntoSpinner()
                        val position = categoryAdapter.getPosition(newCategory)
                        categorySpinner.setSelection(position)
                    } else {
                        Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun showDatePickerDialog(dateInput: EditText, isStartDate: Boolean?) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
            val selectedDateStr = dateFormat.format(selectedDate.time)
            dateInput.setText(selectedDateStr)

            when (isStartDate) {
                true -> {
                    startDateFilter = selectedDate.time
                }
                false -> {
                    endDateFilter = selectedDate.time
                }
                null -> {}
            }

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
        categoryName: String,
        progressOverlay: FrameLayout,
        saveButton: Button
    ) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val categoryReference = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/categories/$categoryName")

            categoryReference.child("categoryName").setValue(categoryName)

            val timesheetEntryData = HashMap<String, Any>()
            timesheetEntryData["date"] = date
            timesheetEntryData["startTime"] = startTime
            timesheetEntryData["endTime"] = endTime
            timesheetEntryData["description"] = description

            if (imageUri != null) {
                val fileExtension = getFileExtension(imageUri!!) ?: "jpg"
                val fileName = "${UUID.randomUUID()}.$fileExtension"
                val storagePath = "timesheet_images/$fileName"
                val storageReference = FirebaseStorage.getInstance().reference.child(storagePath)

                Log.d("TimeSheet", "Uploading image to: ${storageReference.path}")

                val uploadTask = storageReference.putFile(imageUri!!)

                uploadTask.addOnSuccessListener {
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        val photoUrl = uri.toString()
                        timesheetEntryData["photoUrl"] = photoUrl
                        timesheetEntryData["photoPath"] = storagePath

                        saveEntryToDatabase(
                            categoryReference,
                            timesheetEntryData,
                            progressOverlay,
                            saveButton
                        )
                    }.addOnFailureListener { exception ->
                        Log.e("TimeSheet", "Failed to get photo URL", exception)
                        Toast.makeText(this, "Failed to get photo URL: ${exception.message}", Toast.LENGTH_SHORT).show()

                        progressOverlay.visibility = View.GONE
                        saveButton.isEnabled = true
                        findViewById<Button>(R.id.addPhotoButton).isEnabled = true
                        categorySpinner.isEnabled = true
                    }
                }.addOnFailureListener { exception ->
                    Log.e("TimeSheet", "Failed to upload photo", exception)
                    Toast.makeText(this, "Failed to upload photo: ${exception.message}", Toast.LENGTH_SHORT).show()

                    progressOverlay.visibility = View.GONE
                    saveButton.isEnabled = true
                    findViewById<Button>(R.id.addPhotoButton).isEnabled = true
                    categorySpinner.isEnabled = true
                }
            } else {
                saveEntryToDatabase(
                    categoryReference,
                    timesheetEntryData,
                    progressOverlay,
                    saveButton
                )
            }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()

            progressOverlay.visibility = View.GONE
            saveButton.isEnabled = true
            findViewById<Button>(R.id.addPhotoButton).isEnabled = true
            categorySpinner.isEnabled = true
        }
    }

    private fun saveEntryToDatabase(
        categoryReference: DatabaseReference,
        timesheetEntryData: HashMap<String, Any>,
        progressOverlay: FrameLayout,
        saveButton: Button
    ) {
        val timesheetEntriesReference = categoryReference.child("TimesheetEntries")
        val entryId = timesheetEntriesReference.push().key
        if (entryId != null) {
            timesheetEntryData["entryId"] = entryId
            timesheetEntriesReference.child(entryId).setValue(timesheetEntryData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("TimeSheet", "Timesheet entry saved successfully")
                        Toast.makeText(this, "Timesheet entry saved", Toast.LENGTH_SHORT).show()
                        imageUri = null
                        dialogView.findViewById<ImageView>(R.id.selectedImageView).visibility = View.GONE
                        dialogView.findViewById<Button>(R.id.removePhotoButton).visibility = View.GONE

                        progressOverlay.visibility = View.GONE
                        saveButton.isEnabled = true
                        findViewById<Button>(R.id.addPhotoButton).isEnabled = true
                        categorySpinner.isEnabled = true
                    } else {
                        Log.e("TimeSheet", "Failed to save timesheet entry", task.exception)
                        Toast.makeText(this, "Failed to save timesheet entry: ${task.exception?.message}", Toast.LENGTH_SHORT).show()

                        progressOverlay.visibility = View.GONE
                        saveButton.isEnabled = true
                        findViewById<Button>(R.id.addPhotoButton).isEnabled = true
                        categorySpinner.isEnabled = true
                    }
                }
        } else {
            Log.e("TimeSheet", "Failed to generate entry ID")
            Toast.makeText(this, "Failed to generate entry ID", Toast.LENGTH_SHORT).show()

            progressOverlay.visibility = View.GONE
            saveButton.isEnabled = true
            findViewById<Button>(R.id.addPhotoButton).isEnabled = true
            categorySpinner.isEnabled = true
        }
    }

    private fun openImagePicker() {
        showImageSourceDialog()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun openCamera() {
        if (!checkCameraPermission()) {
            requestCameraPermission()
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("TimeSheet", "Error creating image file", ex)
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
                null
            }

            photoFile?.also {
                cameraImageUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                cameraLauncher.launch(intent)
            }
        } else {
            Toast.makeText(this, "No camera application found", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        ).apply {
            imageFilePath = absolutePath
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        val contentResolver = contentResolver
        val mimeTypeMap = android.webkit.MimeTypeMap.getSingleton()
        val mimeType = contentResolver.getType(uri)
        return mimeTypeMap.getExtensionFromMimeType(mimeType)
    }

    private fun deleteTimeSheetEntry(entry: TimeSheetEntry) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val categoryName = entry.categoryName.trim()
            val entryId = entry.entryId

            if (entryId.isNotEmpty()) {
                val entryReference = FirebaseDatabase.getInstance()
                    .getReference("users/$uid/categories/$categoryName/TimesheetEntries/$entryId")

                entryReference.child("photoPath").get().addOnSuccessListener { snapshot ->
                    val photoPath = snapshot.getValue(String::class.java)
                    if (!photoPath.isNullOrEmpty()) {
                        FirebaseStorage.getInstance().reference.child(photoPath).delete()
                            .addOnSuccessListener {
                                Log.d("TimeSheet", "Image deleted successfully")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("TimeSheet", "Failed to delete image", exception)
                            }
                    }

                    entryReference.removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show()
                            val index = timesheetEntries.indexOf(entry)
                            if (index != -1) {
                                timesheetEntries.removeAt(index)
                                timesheetAdapter.notifyItemRemoved(index)
                            }
                        } else {
                            Toast.makeText(this, "Failed to delete entry", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.addOnFailureListener { exception ->
                    Log.e("TimeSheet", "Failed to retrieve photo path", exception)
                    entryReference.removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show()
                            val index = timesheetEntries.indexOf(entry)
                            if (index != -1) {
                                timesheetEntries.removeAt(index)
                                timesheetAdapter.notifyItemRemoved(index)
                            }
                        } else {
                            Toast.makeText(this, "Failed to delete entry", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Invalid entry ID", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
