package com.ucbanilad.waitwise  // ← IMPORTANT! Your package name

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.*

// ── Data Models ────────────────────────────────────────────────────────────────

data class QueueStudent(
    val queueNumber: String = "",
    val name: String = "",
    val serviceType: String = "",
    val estimatedWait: String = "",
    val status: QueueStatus = QueueStatus.WAITING,
    val userId: String = "",
    val documentId: String = ""
)
// ── Adapter ────────────────────────────────────────────────────────────── EXAMPLE

enum class QueueStatus { WAITING, SERVING, SKIPPED, DONE }

// ── Adapter ───────────────────────────────────────────────────────────────────

class QueueAdapter(
    private var students: MutableList<QueueStudent>,
    private val onStudentClick: (QueueStudent) -> Unit
) : RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {

    class QueueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQueueNumber: TextView = view.findViewById(R.id.tvQueueNumber)
        val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
        val tvServiceType: TextView = view.findViewById(R.id.tvServiceType)
        val tvWaitTime: TextView = view.findViewById(R.id.tvWaitTime)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_queue_student, parent, false)
        return QueueViewHolder(view)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        val student = students[position]
        holder.tvQueueNumber.text = student.queueNumber
        holder.tvStudentName.text = student.name
        holder.tvServiceType.text = student.serviceType
        holder.tvWaitTime.text = student.estimatedWait

        when (student.status) {
            QueueStatus.WAITING -> {
                holder.tvStatus.text = "Waiting"
                holder.tvStatus.setTextColor(0xFF1D4ED8.toInt())
            }
            QueueStatus.SERVING -> {
                holder.tvStatus.text = "Serving"
                holder.tvStatus.setTextColor(0xFF166534.toInt())
            }
            QueueStatus.SKIPPED -> {
                holder.tvStatus.text = "Skipped"
                holder.tvStatus.setTextColor(0xFF991B1B.toInt())
            }
            QueueStatus.DONE -> {
                holder.tvStatus.text = "Done"
                holder.tvStatus.setTextColor(0xFF374151.toInt())
            }
        }

        holder.itemView.setOnClickListener { onStudentClick(student) }
    }

    override fun getItemCount() = students.size

    fun updateList(newList: MutableList<QueueStudent>) {
        students = newList
        notifyDataSetChanged()
    }
}

// ── Dashboard Activity ────────────────────────────────────────────────────────

class DashboardActivity : AppCompatActivity() {

    // Views
    private lateinit var tvNowServing: TextView
    private lateinit var tvCurrentStudent: TextView
    private lateinit var tvWaitingCount: TextView
    private lateinit var tvServedCount: TextView
    private lateinit var tvAvgWait: TextView
    private lateinit var tvQueueCount: TextView
    private lateinit var btnCallNext: Button
    private lateinit var btnSkip: Button
    private lateinit var rvQueueList: RecyclerView
    private lateinit var llServiceFilters: LinearLayout
    private lateinit var bottomNav: BottomNavigationView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var selectedFilter = "All"
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var queueListener: ListenerRegistration? = null

    private val serviceTypes = listOf(
        "All", "Document Request", "Enrollment",
        "Course Concern", "Transcript", "Clearance"
    )

    private val fullQueueList = mutableListOf<QueueStudent>()
    private lateinit var adapter: QueueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        onBackPressedDispatcher.addCallback(this) {
            showLogoutDialog()
        }

        initViews()
        setupToolbar()
        displayUserInfo()
        setupServiceFilters()
        setupRecyclerView()
        setupButtons()
        setupBottomNav()

        startFirebaseListener()
    }

    private fun initViews() {
        tvNowServing = findViewById(R.id.tvNowServing)
        tvCurrentStudent = findViewById(R.id.tvCurrentStudent)
        tvWaitingCount = findViewById(R.id.tvWaitingCount)
        tvServedCount = findViewById(R.id.tvServedCount)
        tvAvgWait = findViewById(R.id.tvAvgWait)
        tvQueueCount = findViewById(R.id.tvQueueCount)
        btnCallNext = findViewById(R.id.btnCallNext)
        btnSkip = findViewById(R.id.btnSkip)
        rvQueueList = findViewById(R.id.rvQueueList)
        llServiceFilters = findViewById(R.id.llServiceFilters)
        bottomNav = findViewById(R.id.bottomNav)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun displayUserInfo() {
        val user = auth.currentUser
        Toast.makeText(this, "Welcome, ${user?.email ?: "Staff"}!", Toast.LENGTH_SHORT).show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ -> logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        auth.signOut()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun startFirebaseListener() {
        queueListener = db.collection("queues")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    updateQueueFromFirebase(snapshots)
                }
            }
    }

    private fun updateQueueFromFirebase(snapshots: QuerySnapshot) {
        fullQueueList.clear()

        for (doc in snapshots) {
            val data = doc.data
            val statusString = data["status"] as? String ?: "waiting"
            val queueStatus = when (statusString) {
                "serving" -> QueueStatus.SERVING
                "skipped" -> QueueStatus.SKIPPED
                "done" -> QueueStatus.DONE
                else -> QueueStatus.WAITING
            }

            val student = QueueStudent(
                queueNumber = data["queueNumber"] as? String ?: "",
                name = data["studentName"] as? String ?: "",
                serviceType = data["serviceType"] as? String ?: "",
                estimatedWait = "~5 min",
                status = queueStatus,
                userId = data["studentId"] as? String ?: "",
                documentId = doc.id
            )
            fullQueueList.add(student)
        }

        val waiting = fullQueueList.count { it.status == QueueStatus.WAITING }
        tvWaitingCount.text = waiting.toString()
        tvQueueCount.text = "${fullQueueList.size} in queue"

        filterQueueList()
    }

    private fun setupServiceFilters() {
        llServiceFilters.removeAllViews()
        serviceTypes.forEach { type ->
            val chip = TextView(this).apply {
                text = type
                textSize = 12f
                setPadding(28, 16, 28, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
                isClickable = true
                isFocusable = true
                setOnClickListener { onFilterSelected(type) }
            }
            applyFilterStyle(chip, type == selectedFilter)
            llServiceFilters.addView(chip)
        }
    }

    private fun applyFilterStyle(chip: TextView, isSelected: Boolean) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.chip_selected)
            chip.setTextColor(0xFFFFFFFF.toInt())
        } else {
            chip.setBackgroundResource(R.drawable.chip_default)
            chip.setTextColor(0xFF6B7280.toInt())
        }
    }

    private fun onFilterSelected(filter: String) {
        selectedFilter = filter
        setupServiceFilters()
        filterQueueList()
    }

    private fun filterQueueList() {
        val filtered = if (selectedFilter == "All") fullQueueList.toMutableList()
        else fullQueueList.filter { it.serviceType == selectedFilter }.toMutableList()
        adapter.updateList(filtered)
        tvQueueCount.text = "${filtered.size} in queue"
    }

    private fun setupRecyclerView() {
        adapter = QueueAdapter(fullQueueList.toMutableList()) { student ->
            Toast.makeText(
                this,
                "Selected: ${student.name} (${student.queueNumber})",
                Toast.LENGTH_SHORT
            ).show()
        }
        rvQueueList.layoutManager = LinearLayoutManager(this)
        rvQueueList.adapter = adapter
    }

    private fun setupButtons() {
        btnCallNext.setOnClickListener {
            val nextIndex = fullQueueList.indexOfFirst { it.status == QueueStatus.WAITING }
            if (nextIndex == -1) {
                Toast.makeText(this, "No more students waiting.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val next = fullQueueList[nextIndex]

            if (next.documentId.isNotEmpty()) {
                db.collection("queues").document(next.documentId)
                    .update(
                        mapOf(
                            "status" to "serving",
                            "calledAt" to System.currentTimeMillis()
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(this, "Now calling: ${next.name}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        btnSkip.setOnClickListener {
            val idx = fullQueueList.indexOfFirst {
                it.status == QueueStatus.SERVING || it.status == QueueStatus.WAITING
            }
            if (idx == -1) {
                Toast.makeText(this, "No student to skip.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val skipped = fullQueueList[idx]

            if (skipped.documentId.isNotEmpty()) {
                db.collection("queues").document(skipped.documentId)
                    .update("status", "skipped")
                    .addOnSuccessListener {
                        Toast.makeText(this, "${skipped.name} has been skipped.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_dashboard
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_queue -> {
                    Toast.makeText(this, "Queue screen coming soon!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_history -> {
                    Toast.makeText(this, "History screen coming soon!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        queueListener?.remove()
        activityScope.cancel()
    }
}