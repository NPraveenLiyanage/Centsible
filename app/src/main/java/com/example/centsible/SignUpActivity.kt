package com.example.centsible

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.centsible.databinding.ActivitySignupBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val PREFS_NAME = "MyAppPrefs"
    private val KEY_USERS = "users"
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button functionality: Navigate to the LoginActivity.
        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val usersJson = sharedPref.getString(KEY_USERS, null)
            val users: MutableList<User> = if (usersJson != null) {
                gson.fromJson(usersJson, object : TypeToken<MutableList<User>>() {}.type)
            } else {
                mutableListOf()
            }

            if (users.any { it.email.equals(email, ignoreCase = true) }) {
                Toast.makeText(this, "User with this email already exists", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!binding.cbTerms.isChecked) {
                Toast.makeText(
                    this,
                    "Please agree to the Terms of Service and Privacy Policy",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }


            // Add new user.
            val newUser = User(email, password)
            users.add(newUser)
            sharedPref.edit().putString(KEY_USERS, gson.toJson(users)).apply()

            Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()

            // Optionally log in automatically.
            sharedPref.edit().putString("current_user", newUser.email).apply()
            startActivity(Intent(this, OnBoardingActivity::class.java))
            finish()
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}