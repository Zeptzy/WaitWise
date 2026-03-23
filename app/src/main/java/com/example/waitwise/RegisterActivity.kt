package com.example.waitwise

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.addCallback
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var tilEmail:        TextInputLayout
    private lateinit var etEmail:         TextInputEditText
    private lateinit var btnSendReset:    android.widget.Button
    private lateinit var tvBackToLogin:   TextView
    private lateinit var progressBar:     ProgressBar
    private lateinit var tvErrorMessage:  TextView
    private lateinit var tvSuccessMessage: TextView

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupListeners()

        onBackPressedDispatcher.addCallback(this) {
            navigateToLogin()
        }
    }

    private fun initViews() {
        tilEmail         = findViewById(R.id.tilEmail)
        etEmail          = findViewById(R.id.etEmail)
        btnSendReset     = findViewById(R.id.btnSendReset)
        tvBackToLogin    = findViewById(R.id.tvBackToLogin)
        progressBar      = findViewById(R.id.progressBar)
        tvErrorMessage   = findViewById(R.id.tvErrorMessage)
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage)
    }

    private fun setupListeners() {
        btnSendReset.setOnClickListener  { attemptReset() }
        tvBackToLogin.setOnClickListener { navigateToLogin() }

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                tilEmail.error = null
                hideMessages()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun attemptReset() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            tilEmail.error = "Email is required"; return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"; return
        }

        showLoading(true)
        hideMessages()

        scope.launch {
            try {
                delay(1500) // simulate API call

                // TODO: Replace with real Retrofit API call
                // ApiClient.authService.forgotPassword(ForgotPasswordRequest(email))

                showSuccess("Reset link sent to $email. Please check your inbox.")

            } catch (e: Exception) {
                showError("Failed to send reset link. Please try again.")
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
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSendReset.isEnabled = !isLoading
        btnSendReset.text      = if (isLoading) "" else "Send Reset Link"
    }

    private fun showError(message: String) {
        tvErrorMessage.text       = message
        tvErrorMessage.visibility = View.VISIBLE
        tvSuccessMessage.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        tvSuccessMessage.text       = message
        tvSuccessMessage.visibility = View.VISIBLE
        tvErrorMessage.visibility   = View.GONE
    }

    private fun hideMessages() {
        tvErrorMessage.visibility   = View.GONE
        tvSuccessMessage.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}