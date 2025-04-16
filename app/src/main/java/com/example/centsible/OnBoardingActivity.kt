package com.example.centsible

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.example.centsible.LoginActivity
import com.example.centsible.adapters.OnBoardingAdapter
import com.example.centsible.databinding.ActivityOnboardingBinding

class OnBoardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val PREFS_NAME = "MyAppPrefs"
    private val KEY_ONBOARDING_COMPLETED = "onBoardingCompleted"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if onboarding was already completed.
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (sharedPref.getBoolean(KEY_ONBOARDING_COMPLETED, false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnBoardingAdapter()
        binding.viewPager.adapter = adapter

        // Show the "Get Started" button only on the last page.
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.btnGetStarted.visibility =
                    if (position == adapter.itemCount - 1) View.VISIBLE else View.GONE
            }
        })

        binding.btnGetStarted.setOnClickListener {
            // Mark onboarding as complete and navigate to the login page.
            sharedPref.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}