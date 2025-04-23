package com.example.centsible

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.centsible.databinding.ActivityMainBinding
import com.example.centsible.fragments.AddTransactionFragment
import com.example.centsible.fragments.AnalyticsFragment
import com.example.centsible.fragments.BackupRestoreFragment
import com.example.centsible.fragments.BudgetUpdateFragment
import com.example.centsible.fragments.RecordsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.selectedItemId = R.id.nav_records
        replaceFragment(RecordsFragment.newInstance())

        binding.bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_records -> {
                    replaceFragment(RecordsFragment.newInstance())
                    true
                }
                R.id.nav_analytics -> {
                    replaceFragment(AnalyticsFragment.newInstance())
                    true
                }
                R.id.nav_add -> {
                    replaceFragment(AddTransactionFragment.newInstance())
                    true
                }
                R.id.nav_budget -> {
                    replaceFragment(BudgetUpdateFragment.newInstance())
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(BackupRestoreFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }


    fun updateBottomNavSelection(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }
}