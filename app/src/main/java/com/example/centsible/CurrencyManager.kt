package com.example.centsible

import android.content.Context
//This is a Singleton object that manages the selected currency.
object CurrencyManager {
    private const val PREF_CURRENCY_KEY = "selected_currency"
    fun getCurrencySymbol(context: Context): String {
        val sharedPref = context.getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE)
        val stored = sharedPref.getString(PREF_CURRENCY_KEY, "LKR (Rs)")
        stored?.let {
            val regex = "\\((.*?)\\)".toRegex()  //get Regular Expression
            val match = regex.find(it)
            if (match != null && match.groups.size >= 2) {
                return match.groups[1]?.value ?: "Rs"
            }
        }
        return "Rs"
    }
}