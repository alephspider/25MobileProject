package com.example.wordmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
//import androidx.compose.ui.semantics.text
import androidx.fragment.app.Fragment
//import androidx.glance.visibility
import com.example.wordmap.databinding.FragmentProblemBinding
import java.util.Random

class ProblemGroupFragment : Fragment() {

    private var _binding: FragmentProblemBinding? = null
    private val binding get() = _binding!!
    private var mainActivity: MainActivity? = null
    private var currentWord: String = ""

    companion object {
        private const val ARG_START_INDEX = "start_index"
        const val LAST_PROBLEM_DIRECTION = "마지막" // 마지막 문제 방향 식별자

        fun newInstance(startIndex: Int): ProblemGroupFragment {
            val fragment = ProblemGroupFragment()
            val args = Bundle()
            args.putInt(ARG_START_INDEX, startIndex)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity is MainActivity) {
            mainActivity = activity as MainActivity
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProblemBinding.inflate(inflater, container, false)
        val startIndex = arguments?.getInt(ARG_START_INDEX) ?: 0

        // 현재 문제 번호 (0부터 시작한다고 가정)
        val currentProblemNumber = mainActivity?.problemCount ?: startIndex
        currentWord = "example word ${currentProblemNumber + 1}" // 실제 단어 데이터로 대체
        binding.wordTextView.text = currentWord

        setupProblems(currentProblemNumber) // 현재 문제 번호(또는 인덱스) 전달
        setupNavigationButtons()
        setupFinishButton()
        checkAndUpdateFinishButtonVisibility()
        return binding.root
    }

    // startIndex 대신 currentProblemIndex 또는 유사한 이름 사용 고려
    private fun setupProblems(currentProblemIndex: Int) {
        // ... (기존 setupProblems 로직 유지) ...
        // 예시:
        val optionButtonIds = listOf(
            binding.option1.id, binding.option2.id, binding.option3.id, binding.option4.id
        )
        val random = Random()
        val correctAnswerButtonId = optionButtonIds[random.nextInt(optionButtonIds.size)]
        val sentenceMeaning = "word ${currentProblemIndex + 1} sentence" // 실제 의미 데이터로 대체

        val listener = View.OnClickListener { view ->
            val clickedButton = view as Button
            checkAnswer(
                clickedButton.id == correctAnswerButtonId,
                sentenceMeaning,
                clickedButton.id
            )
        }
        binding.option1.setOnClickListener(listener)
        binding.option2.setOnClickListener(listener)
        binding.option3.setOnClickListener(listener)
        binding.option4.setOnClickListener(listener)
    }


    private fun checkAnswer(isCorrect: Boolean, sentenceMeaning: String, clickedButtonId: Int) {
        if (isCorrect) {
            binding.meaningTextView.text = sentenceMeaning
            binding.meaningTextView.visibility = View.VISIBLE
            binding.option1.visibility = View.GONE
            binding.option2.visibility = View.GONE
            binding.option3.visibility = View.GONE
            binding.option4.visibility = View.GONE

            // 중요: 아직 다음 문제로 넘어가지 않았으므로, 여기서 problemCount를 증가시키고
            //       리스트에 추가하는 것은 navigateToNextProblem에서만 수행하도록 한다.
            //       여기서는 단순히 UI 업데이트 및 완료 상태만 체크한다.
            // mainActivity?.incrementProblemCount() // 여기서 호출하지 않음

            checkAndUpdateFinishButtonVisibility() // UI 상태만 업데이트
        } else {
            val buttonNumber = when (clickedButtonId) {
                binding.option1.id -> 1; binding.option2.id -> 2; binding.option3.id -> 3; binding.option4.id -> 4
                else -> 0
            }
            Toast.makeText(context, "${buttonNumber}번은 오답입니다!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndUpdateFinishButtonVisibility() {
        mainActivity?.let { activity ->
            // problemCount는 0부터 9까지 (총 10문제)
            // 9번째 문제를 풀고 나면 problemCount가 9가 됨.
            // 마지막 10번째 문제를 풀었을 때 (즉, problemCount가 9인 상태에서 정답을 맞혔을 때)
            // 완료 버튼이 보여야 한다.
            if (activity.problemCount >= 9 && binding.meaningTextView.visibility == View.VISIBLE) { // 9는 10문제일 경우 마지막 인덱스
                binding.btnFinish.visibility = View.VISIBLE
                binding.btnUp.visibility = View.GONE
                binding.btnLeft.visibility = View.GONE
                binding.btnRight.visibility = View.GONE
                // 옵션 버튼 등은 이미 checkAnswer에서 GONE 처리됨
                if(activity.problemCount == 9) { // 정확히 마지막 문제를 푼 직후
                    binding.wordTextView.text = "모든 문제를 다 푸셨습니다!"
                    binding.meaningTextView.visibility = View.GONE
                } else { // 혹시 모를 다른 상황 (이미 10개를 넘겼을 경우)
                    binding.wordTextView.text = "모든 문제를 다 푸셨습니다!"
                }
            } else {
                binding.btnFinish.visibility = View.GONE
                if (binding.meaningTextView.visibility == View.VISIBLE) { // 정답을 맞힌 상태라면
                    binding.btnUp.visibility = View.VISIBLE
                    binding.btnLeft.visibility = View.VISIBLE
                    binding.btnRight.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupNavigationButtons() {
        binding.btnUp.setOnClickListener {
            // 정답을 맞히고 다음으로 넘어갈 때 problemCount 증가 및 데이터 추가
            mainActivity?.incrementProblemCount()
            navigateToNextProblem("상단")
        }
        binding.btnLeft.setOnClickListener {
            mainActivity?.incrementProblemCount()
            navigateToNextProblem("좌측")
        }
        binding.btnRight.setOnClickListener {
            mainActivity?.incrementProblemCount()
            navigateToNextProblem("우측")
        }
    }

    private fun navigateToNextProblem(direction: String) {
        mainActivity?.addSolvedProblem(currentWord, direction) // 현재 단어와 방향 저장

        resetProblemUI()
        val newProblemIndex = mainActivity?.problemCount ?: 0
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, newInstance(newProblemIndex))
            .addToBackStack(null)
            .commit()
    }

    private fun resetProblemUI() {
        binding.meaningTextView.visibility = View.GONE
        binding.option1.visibility = View.VISIBLE
        binding.option2.visibility = View.VISIBLE
        binding.option3.visibility = View.VISIBLE
        binding.option4.visibility = View.VISIBLE
        binding.btnUp.visibility = View.GONE
        binding.btnLeft.visibility = View.GONE
        binding.btnRight.visibility = View.GONE
    }

    private fun setupFinishButton() {
        binding.btnFinish.setOnClickListener {
            // "완료" 버튼 클릭 시, 마지막으로 푼 문제의 단어를 리스트에 추가
            // 이때 direction은 특별한 값으로 지정 (예: "마지막")
            // 마지막 문제의 currentWord가 정확해야 함
            if (mainActivity?.problemCount == 9 && binding.meaningTextView.visibility == View.VISIBLE) { // 10번째 문제를 풀고 완료 버튼을 누르는 경우
                mainActivity?.addSolvedProblem(currentWord, LAST_PROBLEM_DIRECTION)
                mainActivity?.incrementProblemCount() // 총 10개가 되도록 카운트 증가
            }
            mainActivity?.navigateToCompletion()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}