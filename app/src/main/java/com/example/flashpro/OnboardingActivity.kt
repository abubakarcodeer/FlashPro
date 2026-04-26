package com.example.flashpro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var btnSkip: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        val onboardingItems = listOf(
            OnboardingItem(
                getString(R.string.welcome_title),
                getString(R.string.welcome_desc),
                R.mipmap.ic_launcher_foreground
            ),
            OnboardingItem(
                getString(R.string.alerts_title),
                getString(R.string.alerts_desc),
                R.drawable.sms
            ),
            OnboardingItem(
                getString(R.string.custom_title),
                getString(R.string.custom_desc),
                R.mipmap.ic_launcher_foreground
            )
        )

        val adapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        btnNext.setOnClickListener {
            if (viewPager.currentItem < onboardingItems.size - 1) {
                viewPager.currentItem += 1
            } else {
                completeOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            completeOnboarding()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == onboardingItems.size - 1) {
                    btnNext.text = getString(R.string.get_started)
                } else {
                    btnNext.text = getString(R.string.next)
                }
            }
        })
    }

    private fun completeOnboarding() {
        val sharedPreferences = getSharedPreferences("FlashProPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isFirstRun", false).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    data class OnboardingItem(val title: String, val description: String, val imageRes: Int)

    inner class OnboardingAdapter(private val items: List<OnboardingItem>) :
        RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return OnboardingViewHolder(view)
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int = items.size

        inner class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val title: TextView = itemView.findViewById(R.id.tvTitle)
            private val description: TextView = itemView.findViewById(R.id.tvDescription)
            private val image: ImageView = itemView.findViewById(R.id.ivOnboarding)

            fun bind(item: OnboardingItem) {
                title.text = item.title
                description.text = item.description
                image.setImageResource(item.imageRes)
            }
        }
    }
}
