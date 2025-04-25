package com.example.personalfinancetracker

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.personalfinancetracker.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPreferences = UserPreferences(this)

        // Check if user is already logged in
        if (userPreferences.isLoggedIn()) {
            navigateToMainActivity()
            finish()
            return
        }

        setupClickListeners()
        setupLoginPromptText()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }

        binding.tvLoginPrompt.setOnClickListener {
            navigateToLogin()
        }
    }

    // Validate user registration inputs
    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Reset errors
        binding.tilEmail.error = null
        binding.tilUsername.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        // Validate email
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            return false
        }

        // Validate username
        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            return false
        }
        if (username.length < 4) {
            binding.tilUsername.error = "Username must be at least 4 characters"
            return false
        }

        // Check if username already exists
        val existingUsers = userPreferences.getUsers()
        if (existingUsers.any { it.username == username }) {
            binding.tilUsername.error = "Username already exists"
            return false
        }

        // Validate password
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return false
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            return false
        }
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return false
        }

        return true
    }

    private fun registerUser() {
        val email = binding.etEmail.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Create user object
        val user = User(email, username, password)

        // Save user to SharedPreferences
        userPreferences.saveUser(user)

        // Do NOT set login state here, user needs to log in
        // userPreferences.saveLoginState(true, username)

        Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_SHORT).show()

        // Navigate to login activity
        navigateToLogin()
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setupLoginPromptText() {
        val fullText = "Already have an account? Login"
        val spannableString = SpannableString(fullText)

        // Set the "Login" part to primary color
        val loginStart = fullText.indexOf("Login")
        val loginEnd = loginStart + "Login".length
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, android.R.color.holo_blue_dark)),
            loginStart,
            loginEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvLoginPrompt.text = spannableString
    }
}