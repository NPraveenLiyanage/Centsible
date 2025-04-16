package com.example.centsible

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat



import android.content.Context
import android.content.Intent

import android.widget.Toast

import com.example.centsible.databinding.ActivityLoginBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Data class for user credentials.
data class User(val email: String, val password: String)

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val PREFS_NAME = "MyAppPrefs"
    private val KEY_USERS = "users"            // stored as a JSON list of User objects
    private val KEY_CURRENT_USER = "current_user"

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Option to navigate to the SignUp screen.
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val usersJson = sharedPref.getString(KEY_USERS, null)
            val users: MutableList<User> = if (usersJson != null) {
                gson.fromJson(usersJson, object : TypeToken<MutableList<User>>() {}.type)
            } else {
                mutableListOf()
            }

            // Check for a matching user (case-insensitive email).
            val user = users.find { it.email.equals(email, ignoreCase = true) }
            if (user == null) {
                Toast.makeText(this, "User not found. Please sign up.", Toast.LENGTH_SHORT).show()
            } else if (user.password != password) {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
            } else {
                // Successful login: store current user and navigate to MainActivity.
                sharedPref.edit().putString(KEY_CURRENT_USER, user.email).apply()
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}