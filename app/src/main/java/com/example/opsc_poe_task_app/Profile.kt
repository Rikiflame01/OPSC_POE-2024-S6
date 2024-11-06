package com.example.opsc_poe_task_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.opsc_poe_task_app.com.example.opsc_poe_task_app.DayStatus
import com.example.opsc_poe_task_app.com.example.opsc_poe_task_app.HeatmapDay
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class Profile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var profilePicture: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var database: FirebaseDatabase

    //Heatmap variables
    private lateinit var heatmapRecyclerView: RecyclerView
    private lateinit var heatmapAdapter: HeatmapAdapter

    //Date format for parsing dates
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        profilePicture = findViewById(R.id.profilePicture)
        userNameTextView = findViewById(R.id.userName)
        heatmapRecyclerView = findViewById(R.id.heatmapRecyclerView)
        heatmapRecyclerView.layoutManager = GridLayoutManager(this, 7) // 7 columns for 7 days a week

        val questBoardButton = findViewById<Button>(R.id.questBoardButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val createQuestButton = findViewById<Button>(R.id.createQuestButton)
        val timeSpentButton = findViewById<FloatingActionButton>(R.id.timeSpentButton)
        val checkGraphsButton = findViewById<Button>(R.id.checkGraphsButton)
        val signOutButton = findViewById<Button>(R.id.signOutButtonBottom)

        questBoardButton.setOnClickListener {
            startActivity(Intent(this, QuestBoard::class.java))
        }

        profileButton.setOnClickListener {
            //Already on Profile screen, no action needed
        }

        createQuestButton.setOnClickListener {
            startActivity(Intent(this, CreateQuest::class.java))
        }

        timeSpentButton.setOnClickListener {
            startActivity(Intent(this, TimeSheet::class.java))
        }

        checkGraphsButton.setOnClickListener {
            startActivity(Intent(this, TimeSheetGraphActivity::class.java))
        }

        signOutButton.setOnClickListener {
            auth.signOut()
            googleSignInClient.signOut().addOnCompleteListener(this) {
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                //Redirect to login screen
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        //Initialize GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //Check if user is signed in and retrieve user information
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUserProfile(currentUser)
        } else {
            Toast.makeText(this, "No user is logged in", Toast.LENGTH_SHORT).show()
        }

        //Load heatmap data
        loadHeatmapData()
    }

    private fun updateUserProfile(user: FirebaseUser) {
        val isGoogleUser = user.providerData.any { it.providerId == "google.com" }

        if (isGoogleUser) {
            //If signed in via Google, sync the Google profile picture and name
            val googleUser = GoogleSignIn.getLastSignedInAccount(this)
            googleUser?.let {
                userNameTextView.text = it.displayName ?: "Username"

                it.photoUrl?.let { photoUrl ->
                    Glide.with(this).load(photoUrl).into(profilePicture)
                } ?: run {
                    profilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }
        } else {
            //If signed in via email/password, fetch username from the database
            fetchUsernameForNonGoogleUser(user)
        }
    }

    private fun fetchUsernameForNonGoogleUser(user: FirebaseUser) {
        val uid = user.uid
        val usersRef = database.getReference("users")

        //Query to find the username where uid matches
        usersRef.orderByChild("uid").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            val username = userSnapshot.child("username").getValue(String::class.java)
                            userNameTextView.text = username ?: "Username"
                            break
                        }
                    } else {
                        Toast.makeText(this@Profile, "Username not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@Profile, "Database error occurred.", Toast.LENGTH_SHORT).show()
                }
            })

        profilePicture.setImageResource(R.drawable.ic_profile_placeholder)
    }

    //Load heatmap data
    private fun loadHeatmapData() {
        val currentUser = auth.currentUser ?: return
        val userCategoriesRef = database.getReference("users/${currentUser.uid}/categories")

        //Get date range
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        val startDate = calendar.time

        userCategoriesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lifecycleScope.launch(Dispatchers.Default) {
                    //Load timesheet entries
                    val dateHoursMap = loadTimesheetEntries(snapshot, startDate, endDate)

                    //Load goals
                    val dateGoalMap = loadGoals(snapshot, startDate, endDate)

                    //Process heatmap data
                    val heatmapDays = processHeatmapData(dateHoursMap, dateGoalMap, startDate, endDate)

                    //Update UI
                    withContext(Dispatchers.Main) {
                        heatmapAdapter = HeatmapAdapter(heatmapDays)
                        heatmapRecyclerView.adapter = heatmapAdapter
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Profile, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private suspend fun loadTimesheetEntries(
        snapshot: DataSnapshot,
        startDate: Date,
        endDate: Date
    ): Map<String, Double> {
        val dateHoursMap = mutableMapOf<String, Double>()

        for (categorySnapshot in snapshot.children) {
            val categoryName = categorySnapshot.key ?: "Unknown"

            val timesheetEntriesSnapshot = categorySnapshot.child("TimesheetEntries")
            if (timesheetEntriesSnapshot.exists()) {
                for (entrySnapshot in timesheetEntriesSnapshot.children) {
                    val entry = entrySnapshot.getValue(TimeSheetEntry::class.java)
                    entry?.let {
                        val entryDate = parseDate(it.date)
                        if (entryDate != null && !entryDate.before(startDate) && !entryDate.after(endDate)) {
                            val dateKey = dateFormat.format(entryDate)
                            val entryHours = it.getDurationInHours()
                            dateHoursMap[dateKey] = dateHoursMap.getOrDefault(dateKey, 0.0) + entryHours
                        }
                    }
                }
            }
        }

        return dateHoursMap
    }

    private suspend fun loadGoals(
        snapshot: DataSnapshot,
        startDate: Date,
        endDate: Date
    ): Map<String, Pair<Double, Double>> {
        val dateGoalMap = mutableMapOf<String, Pair<Double, Double>>()

        for (categorySnapshot in snapshot.children) {
            val categoryName = categorySnapshot.key ?: "Unknown"

            val questsSnapshot = categorySnapshot.child("quests")
            if (questsSnapshot.exists()) {
                //Initialize variables to store category-wide goals
                var categoryMinGoal: Double? = null
                var categoryMaxGoal: Double? = null

                //Iterate through all quests to ensure consistent goals
                for (questSnapshot in questsSnapshot.children) {
                    val quest = questSnapshot.getValue(Quest::class.java)
                    if (quest != null) {
                        if (categoryMinGoal == null && categoryMaxGoal == null) {
                            categoryMinGoal = quest.minGoal.toDouble()
                            categoryMaxGoal = quest.maxGoal.toDouble()
                        } else {
                            //Verify that all quests within the category have the same goals
                            if (categoryMinGoal != quest.minGoal.toDouble() || categoryMaxGoal != quest.maxGoal.toDouble()) {
                                //Inconsistent goals found; using initial category goals
                            }
                        }
                    }
                }

                if (categoryMinGoal != null && categoryMaxGoal != null) {
                    //Assign the category's goals to all dates with entries under this category
                    val timesheetEntriesSnapshot = categorySnapshot.child("TimesheetEntries")
                    if (timesheetEntriesSnapshot.exists()) {
                        for (entrySnapshot in timesheetEntriesSnapshot.children) {
                            val entry = entrySnapshot.getValue(TimeSheetEntry::class.java)
                            entry?.let {
                                val entryDate = parseDate(it.date)
                                if (entryDate != null && !entryDate.before(startDate) && !entryDate.after(endDate)) {
                                    val dateKey = dateFormat.format(entryDate)
                                    dateGoalMap[dateKey] = Pair(
                                        categoryMinGoal,
                                        categoryMaxGoal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        return dateGoalMap
    }


    private fun processHeatmapData(
        dateHoursMap: Map<String, Double>,
        dateGoalMap: Map<String, Pair<Double, Double>>,
        startDate: Date,
        endDate: Date
    ): MutableList<HeatmapDay> {
        val heatmapDays = mutableListOf<HeatmapDay>()
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        var belowGoalCount = 0
        var withinGoalCount = 0
        var aboveGoalCount = 0
        var noDataCount = 0
        var hoursWorkedNoGoalCount = 0

        while (!calendar.time.after(endDate)) {
            try {
                val date = calendar.time
                val dateStr = dateFormat.format(date)
                val hoursWorked = dateHoursMap[dateStr] ?: 0.0
                val goals = dateGoalMap[dateStr] ?: Pair(0.0, 0.0)

                val status = when {
                    goals.first == 0.0 && goals.second == 0.0 && hoursWorked == 0.0 -> DayStatus.NO_DATA
                    goals.first == 0.0 && goals.second == 0.0 && hoursWorked > 0.0 -> DayStatus.HOURS_WORKED_NO_GOAL
                    hoursWorked < goals.first -> DayStatus.BELOW_GOAL
                    hoursWorked > goals.second -> DayStatus.ABOVE_GOAL
                    else -> DayStatus.WITHIN_GOAL
                }

                heatmapDays.add(HeatmapDay(date, status))

                //Update counters
                when (status) {
                    DayStatus.BELOW_GOAL -> belowGoalCount++
                    DayStatus.WITHIN_GOAL -> withinGoalCount++
                    DayStatus.ABOVE_GOAL -> aboveGoalCount++
                    DayStatus.NO_DATA -> noDataCount++
                    DayStatus.HOURS_WORKED_NO_GOAL -> hoursWorkedNoGoalCount++
                }
            } catch (e: Exception) {

            }

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return heatmapDays
    }

    //Helper function to parse dates
    private fun parseDate(dateStr: String): Date? {
        return try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
}
