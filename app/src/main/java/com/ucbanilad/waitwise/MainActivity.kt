package com.ucbanilad.waitwise

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    // ── Views ──────────────────────────────────────────────────────────────
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnSignIn: Button
    private lateinit var btnGoogleSignIn: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvErrorMessage: TextView
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    // ── Firebase ───────────────────────────────────────────────────────────
    private lateinit var auth: FirebaseAuth

    // ── Session & Coroutine ────────────────────────────────────────────────
    private lateinit var session: SessionManager
    private val loginScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── Lifecycle ──────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        session = SessionManager(this)

        // Check if already logged in
        if (auth.currentUser != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

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
        btnGoogleSignIn  = findViewById(R.id.btnGoogleSignIn)  // ← ADD THIS!
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

        btnGoogleSignIn.setOnClickListener { googleSignIn() }

        // ── Sign In (Firebase) ────────────────────────────────────────────
        btnSignIn.setOnClickListener { attemptLogin() }

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
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"
            isValid = false
        }
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }
        return isValid
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                showError("Google Sign-In failed: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?) {
        if (account == null) {
            showError("Google account not found")
            return
        }

        showLoading(true)

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userName = user?.displayName ?: user?.email?.split("@")?.first() ?: "User"

                    // Save user data to Firestore
                    val userId = user?.uid
                    if (userId != null) {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val userData = hashMapOf(
                            "name" to userName,
                            "email" to user.email,
                            "role" to "student",
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("users").document(userId)
                            .set(userData)
                    }

                    Toast.makeText(this, "Welcome, $userName!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    showError("Google Sign-In failed: ${task.exception?.message}")
                }
            }
    }

    // ── Firebase Login ─────────────────────────────────────────────────────
    private fun attemptLogin() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (!validateInputs(email, password)) return

        saveRememberMe(email, cbRememberMe.isChecked)
        showLoading(true)
        hideError()

        // Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    // Login success
                    val user = auth.currentUser
                    val userName = user?.email?.split("@")?.first() ?: "User"

                    // Save session
                    session.saveSession(
                        token = "firebase_token",
                        name = userName,
                        email = user?.email ?: "",
                        role = if (email.contains("staff") || email.contains("admin")) "staff" else "student"
                    )

                    Toast.makeText(this, "Welcome, $userName!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()

                } else {
                    // Login failed
                    val errorMessage = task.exception?.message ?: "Authentication failed"
                    showError(errorMessage)
                }
            }
    }

    // ── UI Helpers ─────────────────────────────────────────────────────────
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSignIn.isEnabled    = !isLoading
        btnSignIn.text         = if (isLoading) "" else "Sign In"
        btnGoogleSignIn.isEnabled = !isLoading  // ← Add this
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