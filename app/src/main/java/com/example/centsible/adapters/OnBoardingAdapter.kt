package com.example.centsible.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.centsible.R
import com.example.centsible.databinding.ItemOnboardingBinding

class OnBoardingAdapter : RecyclerView.Adapter<OnBoardingAdapter.OnBoardingViewHolder>() {

    // Hardcoded arrays for the three onboarding pages.
    private val titles = arrayOf(
        "Welcome",
        "Track Expenses",
        "Get Insights"
    )
    private val descriptions = arrayOf(
        "Manage your finances effortlessly.",
        "Keep an eye on your daily expenses.",
        "See analytics and take control of your money."
    )
    private val imageResIds = intArrayOf(
        R.drawable.onboarding_image1,
        R.drawable.onboarding_image2,
        R.drawable.onboarding_image3
    )

    inner class OnBoardingViewHolder(val binding: ItemOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnBoardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnBoardingViewHolder(binding)
    }

    override fun getItemCount(): Int = titles.size

    override fun onBindViewHolder(holder: OnBoardingViewHolder, position: Int) {
        holder.binding.tvTitle.text = titles[position]
        holder.binding.tvDescription.text = descriptions[position]
        holder.binding.ivOnboarding.setImageResource(imageResIds[position])
    }
}