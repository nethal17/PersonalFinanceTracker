package com.example.personalfinancetracker

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.personalfinancetracker.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPreferences = UserPreferences(this)

        // Check if user is already logged in
        if (userPreferences.isLoggedIn()) {
            navigateToMainActivity()
            finish()
            return
        }

        setupClickListeners()
        setupRegisterPromptText()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                attemptLogin()
            }
        }

        binding.tvRegisterPrompt.setOnClickListener {
            navigateToRegister()
        }
    }

    // Validate Login inputs
    private fun validateInputs(): Boolean {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Reset errors
        binding.tilUsername.error = null
        binding.tilPassword.error = null

        // Validate username
        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            return false
        }

        // Validate password
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }

        return true
    }

    private fun attemptLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (userPreferences.validateUser(username, password)) {
            // Set login state
            userPreferences.saveLoginState(true, username)

            // Navigate to main activity
            navigateToMainActivity()
            finish()
        } else {
            showLoginError()
        }
    }

    private fun showLoginError() {
        AlertDialog.Builder(this)
            .setTitle("Login Failed")
            .setMessage("Invalid username or password. Please try again.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
        finish()
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setupRegisterPromptText() {
        val fullText = "Didn't have an account? Register"
        val spannableString = SpannableString(fullText)

        val registerStart = fullText.indexOf("Register")
        val registerEnd = registerStart + "Register".length
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, android.R.color.holo_blue_dark)),
            registerStart,
            registerEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        binding.tvRegisterPrompt.text = spannableString
    }
}