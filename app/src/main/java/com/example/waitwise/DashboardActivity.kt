package com.example.waitwise

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
import kotlinx.coroutines.*

// ── Data Models ────────────────────────────────────────────────────────────────

data class QueueStudent(
    val queueNumber: String,
    val name: String,
    val serviceType: String,
    val estimatedWait: String,
    val status: QueueStatus
)

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
        val tvWaitTime:    TextView = view.findViewById(R.id.tvWaitTime)
        val tvStatus:      TextView = view.findViewById(R.id.tvStatus)
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
        holder.tvWaitTime.text    = student.estimatedWait

        when (student.status) {
            QueueStatus.WAITING -> {
                holder.tvStatus.text = "Waiting"
                holder.tvStatus.setTextColor(0xFF1D4ED8.toInt())
                holder.tvStatus.setBackgroundResource(R.drawable.badge_status_waiting)
            }
            QueueStatus.SERVING -> {
                holder.tvStatus.text = "Serving"
                holder.tvStatus.setTextColor(0xFF166534.toInt())
                holder.tvStatus.setBackgroundResource(R.drawable.badge_status_serving)
            }
            QueueStatus.SKIPPED -> {
                holder.tvStatus.text = "Skipped"
                holder.tvStatus.setTextColor(0xFF991B1B.toInt())
                holder.tvStatus.setBackgroundResource(R.drawable.badge_status_skipped)
            }
            QueueStatus.DONE -> {
                holder.tvStatus.text = "Done"
                holder.tvStatus.setTextColor(0xFF374151.toInt())
                holder.tvStatus.setBackgroundResource(R.drawable.badge_status_done)
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
    private lateinit var tvNowServing:     TextView
    private lateinit var tvCurrentStudent: TextView
    private lateinit var tvWaitingCount:   TextView
    private lateinit var tvServedCount:    TextView
    private lateinit var tvAvgWait:        TextView
    private lateinit var tvQueueCount:     TextView
    private lateinit var btnCallNext:      Button
    private lateinit var btnSkip:          Button
    private lateinit var rvQueueList:      RecyclerView
    private lateinit var llServiceFilters: LinearLayout
    private lateinit var bottomNav:        BottomNavigationView

    // Session & State
    private lateinit var session:    SessionManager
    private var currentQueueNumber = 42
    private var servedCount        = 42
    private var avgWaitMinutes     = 8
    private var selectedFilter     = "All"
    private val activityScope      = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val serviceTypes = listOf(
        "All", "Document Request", "Enrollment",
        "Course Concern", "Transcript", "Clearance"
    )

    private val fullQueueList = mutableListOf(
        QueueStudent("043", "Maria Santos",    "Document Request", "~5 min",  QueueStatus.WAITING),
        QueueStudent("044", "Jose Reyes",      "Enrollment",       "~10 min", QueueStatus.WAITING),
        QueueStudent("045", "Ana Garcia",      "Course Concern",   "~15 min", QueueStatus.WAITING),
        QueueStudent("046", "Pedro Lim",       "Transcript",       "~20 min", QueueStatus.WAITING),
        QueueStudent("047", "Luz Villanueva",  "Clearance",        "~25 min", QueueStatus.WAITING),
        QueueStudent("048", "Ramon Dela Cruz", "Document Request", "~30 min", QueueStatus.WAITING),
        QueueStudent("049", "Teresa Cruz",     "Enrollment",       "~35 min", QueueStatus.WAITING),
        QueueStudent("050", "Marco Bautista",  "Course Concern",   "~40 min", QueueStatus.WAITING)
    )

    private lateinit var adapter: QueueAdapter

    // ── Lifecycle ──────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        session = SessionManager(this)

        // Guard: if no session, send back to login
        if (!session.isLoggedIn()) {
            redirectToLogin()
            return
        }

        // ── Back Button → show logout dialog ──────────────────────────────
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
        startLiveRefresh()
    }

    // ── Init Views ─────────────────────────────────────────────────────────
    private fun initViews() {
        tvNowServing     = findViewById(R.id.tvNowServing)
        tvCurrentStudent = findViewById(R.id.tvCurrentStudent)
        tvWaitingCount   = findViewById(R.id.tvWaitingCount)
        tvServedCount    = findViewById(R.id.tvServedCount)
        tvAvgWait        = findViewById(R.id.tvAvgWait)
        tvQueueCount     = findViewById(R.id.tvQueueCount)
        btnCallNext      = findViewById(R.id.btnCallNext)
        btnSkip          = findViewById(R.id.btnSkip)
        rvQueueList      = findViewById(R.id.rvQueueList)
        llServiceFilters = findViewById(R.id.llServiceFilters)
        bottomNav        = findViewById(R.id.bottomNav)
    }

    // ── Toolbar ────────────────────────────────────────────────────────────
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            showLogoutDialog()
        }
    }

    // ── Display User Info ──────────────────────────────────────────────────
    private fun displayUserInfo() {
        val name = session.getUserName()
        val role = session.getUserRole()
        Toast.makeText(this, "Welcome, $name ($role)!", Toast.LENGTH_SHORT).show()
    }

    // ── Logout ─────────────────────────────────────────────────────────────
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ -> logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        session.clearSession()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ── Service Filter Chips ───────────────────────────────────────────────
    private fun setupServiceFilters() {
        llServiceFilters.removeAllViews()
        serviceTypes.forEach { type ->
            val chip = TextView(this).apply {
                text     = type
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
        tvQueueCount.text   = "${filtered.size} in queue"
        tvWaitingCount.text = filtered.count { it.status == QueueStatus.WAITING }.toString()
    }

    // ── RecyclerView ───────────────────────────────────────────────────────
    private fun setupRecyclerView() {
        adapter = QueueAdapter(fullQueueList.toMutableList()) { student ->
            Toast.makeText(
                this,
                "Selected: ${student.name} (${student.queueNumber})",
                Toast.LENGTH_SHORT
            ).show()
        }
        rvQueueList.layoutManager = LinearLayoutManager(this)
        rvQueueList.adapter       = adapter
        updateStats()
    }

    // ── Call Next & Skip Buttons ───────────────────────────────────────────
    private fun setupButtons() {
        btnCallNext.setOnClickListener {
            val nextIndex = fullQueueList.indexOfFirst { it.status == QueueStatus.WAITING }
            if (nextIndex == -1) {
                Toast.makeText(this, "No more students waiting.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val next = fullQueueList[nextIndex]
            fullQueueList[nextIndex] = next.copy(status = QueueStatus.SERVING)
            currentQueueNumber++
            servedCount++
            tvNowServing.text     = next.queueNumber
            tvCurrentStudent.text = next.name
            tvServedCount.text    = servedCount.toString()
            filterQueueList()
            Toast.makeText(this, "Now calling: ${next.name}", Toast.LENGTH_SHORT).show()
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
            fullQueueList[idx] = skipped.copy(status = QueueStatus.SKIPPED)
            filterQueueList()
            Toast.makeText(this, "${skipped.name} has been skipped.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Update Stats Cards ─────────────────────────────────────────────────
    private fun updateStats() {
        val waiting = fullQueueList.count { it.status == QueueStatus.WAITING }
        tvNowServing.text   = String.format("%03d", currentQueueNumber)
        tvWaitingCount.text = waiting.toString()
        tvServedCount.text  = servedCount.toString()
        tvAvgWait.text      = avgWaitMinutes.toString()
        tvQueueCount.text   = "$waiting in queue"
    }

    // ── Bottom Navigation ──────────────────────────────────────────────────
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

    // ── Auto Refresh every 30s ─────────────────────────────────────────────
    private fun startLiveRefresh() {
        activityScope.launch {
            while (isActive) {
                delay(30_000)
                // TODO: Replace with real API call when Laravel is ready
                // val response = ApiClient.queueService.getQueueStatus()
                // updateQueueFromApi(response)
            }
        }
    }

    // ── Cleanup ────────────────────────────────────────────────────────────
    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}