package com.example.wordmap

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.wordmap.databinding.ActivityMainBinding
// import com.example.wordmap.SolvedProblemData // 이미 import 되어 있을 것임

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var problemCount = 0 // 0부터 시작, 0~9까지 총 10문제
    val solvedProblemsList = mutableListOf<SolvedProblemData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StartFragment.newInstance()) // newInstance 사용 권장
                .commit()
        }
    }

    fun incrementProblemCount() {
        problemCount++
        // Log.d("MainActivity", "Problem count incremented: $problemCount")
    }

    fun addSolvedProblem(word: String, direction: String) {
        solvedProblemsList.add(SolvedProblemData(word, direction))
        // Log.d("MainActivity", "Added: $word, $direction, Current List Size: ${solvedProblemsList.size}")
    }

    fun navigateToCompletion() {
        // Log.d("MainActivity", "Navigating to completion. Total items: ${solvedProblemsList.size}")
        val fragment = CompletionFragment.newInstance(ArrayList(solvedProblemsList))
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            // .addToBackStack(null) // 완료 화면에서 뒤로가기 시 이전 문제 화면으로 가지 않도록 addToBackStack 제거 또는 관리 필요
            .commit()
    }

    fun restartProblems() {
        // 이전 문제 풀이 데이터 초기화
        problemCount = 0
        solvedProblemsList.clear()
        Log.d("MainActivity", "Problems restarted. Count and list cleared.")

        // StartFragment로 이동
        // 기존 프래그먼트 스택을 모두 제거하고 StartFragment를 새로 표시할 수도 있습니다.
        // 예를 들어, popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE) 사용
        supportFragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE) // 모든 백스택 제거
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, StartFragment.newInstance()) // newInstance 사용 권장
            .commit()
    }
}