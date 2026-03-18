package com.example.waitwise

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    // ── Views ──────────────────────────────────────────────────────────────
    private lateinit var tilEmail:         TextInputLayout
    private lateinit var tilPassword:      TextInputLayout
    private lateinit var etEmail:          TextInputEditText
    private lateinit var etPassword:       TextInputEditText
    private lateinit var cbRememberMe:     CheckBox
    private lateinit var btnSignIn:        Button
    private lateinit var btnGoogleSignIn:  Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister:       TextView
    private lateinit var progressBar:      ProgressBar
    private lateinit var tvErrorMessage:   TextView

    // ── Session & Coroutine ────────────────────────────────────────────────
    private lateinit var session: SessionManager
    private val loginScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── Mock Users (replace with Retrofit API call later) ─────────────────
    data class MockUser(val email: String, val password: String, val name: String, val role: String)

    private val mockUsers = listOf(
        MockUser("test@uc.edu.ph",    "password123", "Juan dela Cruz", "student"),
        MockUser("admin@uc.edu.ph",   "admin123",    "Admin User",     "admin"),
        MockUser("officer@uc.edu.ph", "officer123",  "Queue Officer",  "officer")
    )

    // ── Lifecycle ──────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session = SessionManager(this)
        initViews()
        loadRememberedEmail()
        setupListeners()
    }

    // ── Init Views ─────────────────────────────────────────────────────────
    private fun initViews() {
        tilEmail         = findViewById(R.id.tilEmail)
        tilPassword      = findViewById(R.id.tilPassword)
        etEmail          = findViewById(R.id.etEmail)
        etPassword       = findViewById(R.id.etPassword)
        cbRememberMe     = findViewById(R.id.cbRememberMe)
        btnSignIn        = findViewById(R.id.btnSignIn)
        btnGoogleSignIn  = findViewById(R.id.btnGoogleSignIn)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegister       = findViewById(R.id.tvRegister)
        progressBar      = findViewById(R.id.progressBar)
        tvErrorMessage   = findViewById(R.id.tvErrorMessage)
    }

    // ── Remember Me ────────────────────────────────────────────────────────
    private fun loadRememberedEmail() {
        val prefs    = getSharedPreferences("WaitWisePrefs", MODE_PRIVATE)
        val remember = prefs.getBoolean("remember_me", false)
        if (remember) {
            etEmail.setText(prefs.getString("saved_email", ""))
            cbRememberMe.isChecked = true
        }
    }

    private fun saveRememberMe(email: String, remember: Boolean) {
        getSharedPreferences("WaitWisePrefs", MODE_PRIVATE).edit().apply {
            putBoolean("remember_me", remember)
            if (remember) putString("saved_email", email) else remove("saved_email")
            apply()
        }
    }

    // ── Listeners ──────────────────────────────────────────────────────────
    private fun setupListeners() {

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { tilEmail.error = null; hideError() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { tilPassword.error = null; hideError() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { attemptLogin(); true } else false
        }

        // ── Sign In ────────────────────────────────────────────────────────
        btnSignIn.setOnClickListener { attemptLogin() }

        // ── Continue with Google ───────────────────────────────────────────
        btnGoogleSignIn.setOnClickListener {
            // TODO: Replace with real Google Sign-In when ready
            Toast.makeText(this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
        }

        // ── Forgot Password ────────────────────────────────────────────────
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // ── Register ───────────────────────────────────────────────────────
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // ── Validation ─────────────────────────────────────────────────────────
    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true
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
        return isValid
    }

    // ── Login Flow ─────────────────────────────────────────────────────────
    private fun attemptLogin() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (!validateInputs(email, password)) return

        saveRememberMe(email, cbRememberMe.isChecked)
        showLoading(true)
        hideError()

        loginScope.launch {
            try {
                delay(1500)

                // TODO: Replace with actual Retrofit API call
                // val response = ApiClient.authService.login(LoginRequest(email, password))
                // session.saveSession(response.token, response.name, response.email, response.role)
                // onLoginSuccess()

                val user = mockUsers.find { it.email == email && it.password == password }

                if (user != null) {
                    session.saveSession(
                        token = "mock_token_${System.currentTimeMillis()}",
                        name  = user.name,
                        email = user.email,
                        role  = user.role
                    )
                    onLoginSuccess()
                } else {
                    showError("Incorrect email or password. Please try again.")
                }

            } catch (e: Exception) {
                showError("An unexpected error occurred. Please try again.")
            } finally {
                showLoading(false)
            }
        }
    }

    // ── Navigate to Dashboard ──────────────────────────────────────────────
    private fun onLoginSuccess() {
        Toast.makeText(this, "Welcome, ${session.getUserName()}!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    // ── UI Helpers ─────────────────────────────────────────────────────────
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSignIn.isEnabled    = !isLoading
        btnSignIn.text         = if (isLoading) "" else "Sign In"
        etEmail.isEnabled      = !isLoading
        etPassword.isEnabled   = !isLoading
    }

    private fun showError(message: String) {
        tvErrorMessage.text       = message
        tvErrorMessage.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvErrorMessage.visibility = View.GONE
    }

    // ── Cleanup ────────────────────────────────────────────────────────────
    override fun onDestroy() {
        super.onDestroy()
        loginScope.cancel()
    }
}