package com.example.pametneucionice

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pametneucionice.augmentedreality.AugmentedRealityActivity
import com.example.pametneucionice.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonStartAugmentedReality.setOnClickListener {
            startActivity(Intent(this, AugmentedRealityActivity::class.java))
        }
        binding.buttonInstructions.setOnClickListener {
            startActivity(Intent(this, InstructionsActivity::class.java))
        }
    }
}
