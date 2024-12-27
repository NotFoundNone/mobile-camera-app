package com.example.mobilecourcework

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobilecourcework.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // `NavHostFragment` автоматически загрузит `PhotoFragment` как стартовый
    }
}
