package com.example.wordmap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wordmap.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var problemCount = 0 // 문제 풀이 진행을 추적

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StartFragment())
                .commit()
        }
    }

    fun incrementProblemCount() {
        problemCount++
        if (problemCount >= 10) {
            navigateToCompletion()
        }
    }

    private fun navigateToCompletion() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CompletionFragment())
            .commit()
    }
}
