package com.example.wordmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.wordmap.databinding.FragmentProblemBinding

class ProblemGroupFragment : Fragment() {

    private lateinit var binding: FragmentProblemBinding

    companion object {
        private const val ARG_START_INDEX = "start_index"

        fun newInstance(startIndex: Int): ProblemGroupFragment {
            val fragment = ProblemGroupFragment()
            val args = Bundle()
            args.putInt(ARG_START_INDEX, startIndex)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProblemBinding.inflate(inflater, container, false)

        val startIndex = arguments?.getInt(ARG_START_INDEX) ?: 0
        setupProblems(startIndex)
        setupNavigationButtons()

        return binding.root
    }

    private fun setupProblems(startIndex: Int) {
        // 1번 버튼을 정답으로 설정합니다.
        val correctAnswerButtonId = R.id.option1
        val sentenceMeaning = "This is the meaning of the sentence."

        val listener = View.OnClickListener { view ->
            val clickedButton = view as Button
            checkAnswer(clickedButton.id == correctAnswerButtonId, sentenceMeaning)
        }

        binding.option1.setOnClickListener(listener)
        binding.option2.setOnClickListener(listener)
        binding.option3.setOnClickListener(listener)
        binding.option4.setOnClickListener(listener)
    }


    private fun checkAnswer(isCorrect: Boolean, sentenceMeaning: String) {
        if (isCorrect) {
            binding.option1.visibility = View.GONE
            binding.option2.visibility = View.GONE
            binding.option3.visibility = View.GONE
            binding.option4.visibility = View.GONE

            binding.meaningTextView.text = sentenceMeaning
            binding.meaningTextView.visibility = View.VISIBLE

            // 문제를 푼 후 이동 버튼을 표시
            binding.btnUp.visibility = View.VISIBLE
            binding.btnLeft.visibility = View.VISIBLE
            binding.btnRight.visibility = View.VISIBLE

            // 문제를 풀고 나서 MainActivity의 problemCount를 증가시킵니다.
            (activity as? MainActivity)?.incrementProblemCount()
        } else {
            Toast.makeText(context, "오답입니다!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigationButtons() {
        val startIndex = arguments?.getInt(ARG_START_INDEX) ?: 0

        binding.btnUp.setOnClickListener {
            // 상단 문제를 위한 새로운 데이터 생성
            val newProblemData = generateProblemData("상단")
            navigateToFragment(ProblemGroupFragment.newInstance(newProblemData))
        }

        binding.btnLeft.setOnClickListener {
            // 좌측 문제를 위한 새로운 데이터 생성
            val newProblemData = generateProblemData("좌측")
            navigateToFragment(ProblemGroupFragment.newInstance(newProblemData))
        }

        binding.btnRight.setOnClickListener {
            // 우측 문제를 위한 새로운 데이터 생성
            val newProblemData = generateProblemData("우측")
            navigateToFragment(ProblemGroupFragment.newInstance(newProblemData))
        }
    }

    private fun generateProblemData(direction: String): Int {
        // AI를 통해 즉각적으로 문제 데이터를 생성하는 로직
        // 방향에 따라 다른 문제 데이터를 생성
        // 여기서는 예시로 무작위 인덱스를 반환
        return (0..100).random() // 예시: 0부터 100 사이의 임의의 인덱스 생성
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
