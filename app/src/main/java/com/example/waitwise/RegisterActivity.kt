package com.example.waitwise

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.addCallback
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var tilName:            TextInputLayout
    private lateinit var tilStudentId:       TextInputLayout
    private lateinit var tilEmail:           TextInputLayout
    private lateinit var tilPassword:        TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etName:             TextInputEditText
    private lateinit var etStudentId:        TextInputEditText
    private lateinit var etEmail:            TextInputEditText
    private lateinit var etPassword:         TextInputEditText
    private lateinit var etConfirmPassword:  TextInputEditText
    private lateinit var btnRegister:        android.widget.Button
    private lateinit var btnBack:            ImageButton
    private lateinit var tvLogin:            TextView
    private lateinit var tvPickPhoto:        TextView
    private lateinit var btnPickPhoto:       ImageView
    private lateinit var ivAvatar:           ImageView
    private lateinit var progressBar:        ProgressBar
    private lateinit var tvErrorMessage:     TextView

    private val registerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Photo picker
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            ivAvatar.setImageURI(it)
            ivAvatar.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupListeners()

        onBackPressedDispatcher.addCallback(this) {
            navigateToLogin()
        }
    }

    private fun initViews() {
        tilName            = findViewById(R.id.tilName)
        tilStudentId       = findViewById(R.id.tilStudentId)
        tilEmail           = findViewById(R.id.tilEmail)
        tilPassword        = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etName             = findViewById(R.id.etName)
        etStudentId        = findViewById(R.id.etStudentId)
        etEmail            = findViewById(R.id.etEmail)
        etPassword         = findViewById(R.id.etPassword)
        etConfirmPassword  = findViewById(R.id.etConfirmPassword)
        btnRegister        = findViewById(R.id.btnRegister)
        btnBack            = findViewById(R.id.btnBack)
        tvLogin            = findViewById(R.id.tvLogin)
        tvPickPhoto        = findViewById(R.id.tvPickPhoto)
        btnPickPhoto       = findViewById(R.id.btnPickPhoto)
        ivAvatar           = findViewById(R.id.ivAvatar)
        progressBar        = findViewById(R.id.progressBar)
        tvErrorMessage     = findViewById(R.id.tvErrorMessage)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener      { navigateToLogin() }
        tvLogin.setOnClickListener      { navigateToLogin() }
        btnPickPhoto.setOnClickListener { pickImage.launch("image/*") }
        tvPickPhoto.setOnClickListener  { pickImage.launch("image/*") }
        btnRegister.setOnClickListener  { attemptRegister() }

        // Clear errors on typing
        listOf(etName, etStudentId, etEmail, etPassword, etConfirmPassword).forEach { field ->
            field.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { hideError() }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    private fun validateInputs(
        name: String, studentId: String,
        email: String, password: String, confirmPassword: String
    ): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            tilName.error = "Full name is required"; isValid = false
        }
        if (studentId.isEmpty()) {
            tilStudentId.error = "Student ID is required"; isValid = false
        }
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"; isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"; isValid = false
        }
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"; isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"; isValid = false
        }
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Please confirm your password"; isValid = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"; isValid = false
        }

        return isValid
    }

    private fun attemptRegister() {
        val name            = etName.text.toString().trim()
        val studentId       = etStudentId.text.toString().trim()
        val email           = etEmail.text.toString().trim()
        val password        = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (!validateInputs(name, studentId, email, password, confirmPassword)) return

        showLoading(true)
        hideError()

        registerScope.launch {
            try {
                delay(1500) // simulate API call

                // TODO: Replace with real Retrofit API call
                // val response = ApiClient.authService.register(RegisterRequest(...))
                // session.saveSession(response.token, response.name, response.email, response.role)

                // Mock success
                val session = SessionManager(this@RegisterActivity)
                session.saveSession(
                    token = "mock_token_${System.currentTimeMillis()}",
                    name  = name,
                    email = email,
                    role  = "student"
                )

                Toast.makeText(
                    this@RegisterActivity,
                    "Account created! Welcome, $name!",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this@RegisterActivity, DashboardActivity::class.java))
                finish()

            } catch (e: Exception) {
                showError("Registration failed. Please try again.")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility  = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled   = !isLoading
        btnRegister.text        = if (isLoading) "" else "Create Account"
    }

    private fun showError(message: String) {
        tvErrorMessage.text       = message
        tvErrorMessage.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvErrorMessage.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        registerScope.cancel()
    }
}