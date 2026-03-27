package com.ucbanilad.waitwise

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etStudentId: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spCourse: Spinner
    private lateinit var spYearLevel: Spinner
    private lateinit var cbTerms: CheckBox
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLogin: TextView
    private lateinit var tvPasswordStrength: TextView
    private lateinit var tvPasswordMatch: TextView
    private lateinit var tvErrorMessage: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupSpinners()
        setupPasswordValidation()

        btnRegister.setOnClickListener { registerUser() }
        tvLogin.setOnClickListener { finish() }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Goes back to login screen
        }
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etStudentId = findViewById(R.id.etStudentId)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        spCourse = findViewById(R.id.spCourse)
        spYearLevel = findViewById(R.id.spYearLevel)
        cbTerms = findViewById(R.id.cbTerms)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvLogin = findViewById(R.id.tvLogin)
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength)
        tvPasswordMatch = findViewById(R.id.tvPasswordMatch)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
    }

    private fun setupSpinners() {
        // Course options
        val courses = arrayOf(
            "Select Course", "BSIT - Information Technology",
            "BSCS - Computer Science", "BSIS - Information Systems",
            "BSBA - Business Administration", "BSHM - Hospitality Management",
            "BSED - Education", "BSPSY - Psychology"
        )
        val courseAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courses)
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCourse.adapter = courseAdapter

        // Year Level options
        val yearLevels = arrayOf("Select Year", "1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year")
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, yearLevels)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spYearLevel.adapter = yearAdapter
    }

    private fun setupPasswordValidation() {
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                // Check password strength
                val strength = checkPasswordStrength(password)
                tvPasswordStrength.text = strength
                tvPasswordStrength.setTextColor(getStrengthColor(strength))
                // Check if passwords match
                checkPasswordMatch()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { checkPasswordMatch() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun checkPasswordStrength(password: String): String {
        return when {
            password.isEmpty() -> ""
            password.length < 6 -> "Weak - Use at least 6 characters"
            password.length in 6..7 -> "Fair - Add more characters"
            password.matches(Regex(".*[A-Z].*")) &&
                    password.matches(Regex(".*[0-9].*")) &&
                    password.length >= 8 -> "Strong ✓"
            else -> "Medium - Add numbers and uppercase"
        }
    }

    private fun getStrengthColor(strength: String): Int {
        return when {
            strength.contains("Strong") -> 0xFF16A34A.toInt()
            strength.contains("Medium") -> 0xFFF59E0B.toInt()
            strength.contains("Fair") -> 0xFFF97316.toInt()
            strength.contains("Weak") -> 0xFFEF4444.toInt()
            else -> 0xFF9CA3AF.toInt()
        }
    }

    private fun checkPasswordMatch() {
        val password = etPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()

        if (confirm.isNotEmpty()) {
            if (password == confirm) {
                tvPasswordMatch.text = "✓ Passwords match"
                tvPasswordMatch.setTextColor(0xFF16A34A.toInt())
            } else {
                tvPasswordMatch.text = "✗ Passwords do not match"
                tvPasswordMatch.setTextColor(0xFFEF4444.toInt())
            }
        } else {
            tvPasswordMatch.text = ""
        }
    }

    private fun registerUser() {
        val name = etName.text.toString().trim()
        val studentId = etStudentId.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val course = spCourse.selectedItem.toString()
        val yearLevel = spYearLevel.selectedItem.toString()

        // Clear previous error
        tvErrorMessage.visibility = android.view.View.GONE

        // Validation
        if (name.isEmpty()) {
            showError("Full Name is required")
            return
        }

        if (studentId.isEmpty()) {
            showError("Student ID is required")
            return
        }

        if (!studentId.matches(Regex("^\\d+\$"))) {
            showError("Student ID must contain only numbers");
            return
        }

        if (email.isEmpty()) {
            showError("Email is required")
            return
        }


        if (course == "Select Course") {
            showError("Please select your course")
            return
        }

        if (yearLevel == "Select Year") {
            showError("Please select your year level")
            return
        }

        if (password.isEmpty()) {
            showError("Password is required")
            return
        }

        if (password.length < 6) {
            showError("Password must be at least 6 characters")
            return
        }

        if (password != confirmPassword) {
            showError("Passwords do not match")
            return
        }

        if (!cbTerms.isChecked) {
            showError("You must agree to the Terms & Conditions")
            return
        }

        // Show loading
        progressBar.visibility = android.view.View.VISIBLE
        btnRegister.isEnabled = false

        // Create user in Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                progressBar.visibility = android.view.View.GONE
                btnRegister.isEnabled = true

                if (task.isSuccessful) {
                    // Save user data to Firestore
                    val userId = auth.currentUser?.uid
                    val userData = hashMapOf(
                        "name" to name,
                        "studentId" to studentId,
                        "email" to email,
                        "course" to course,
                        "yearLevel" to yearLevel,
                        "role" to "student",
                        "signInMethod" to "email",
                        "profileCompleted" to true,
                        "createdAt" to System.currentTimeMillis()
                    )

                    if (userId != null) {
                        db.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "✅ Registration successful!", Toast.LENGTH_LONG).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "✅ Registration successful!", Toast.LENGTH_LONG).show()
                                finish()
                            }
                    } else {
                        Toast.makeText(this, "✅ Registration successful!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Registration failed"
                    showError(errorMessage)
                }
            }
    }

    private fun showError(message: String) {
        tvErrorMessage.text = message
        tvErrorMessage.visibility = android.view.View.VISIBLE
    }
}