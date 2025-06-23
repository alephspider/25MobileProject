package com.example.wordmap

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.wordmap.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var problemCount = 0 // 0부터 시작, 0~9까지 총 10문제
    val solvedProblemsList = mutableListOf<SolvedProblemData>() // 경로를 그리기 위해 필요한 문제 내용과 위치 이동 리스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StartFragment.newInstance()) // 시작 프래그먼트 설정
                .commit()
        }
    }

    // 문제 수 카운트 증가
    fun incrementProblemCount() {
        problemCount++

    }

    // 사용자가 푼 단어와 방향을 리스트에 추가
    fun addSolvedProblem(word: String, direction: String) {
        solvedProblemsList.add(SolvedProblemData(word, direction))

    }

    fun navigateToCompletion() {
        // Log.d("MainActivity", "Navigating to completion. Total items: ${solvedProblemsList.size}")
        val fragment = CompletionFragment.newInstance(ArrayList(solvedProblemsList))
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    //시작화면으로 돌아오기
    fun restartProblems() {
        // 이전 문제 풀이 데이터 초기화
        problemCount = 0
        solvedProblemsList.clear()
        Log.d("MainActivity", "Problems restarted. Count and list cleared.")

        // StartFragment로 이동
        supportFragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE) // 모든 백스택 제거
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, StartFragment.newInstance()) // newInstance 사용 권장
            .commit()
    }
}