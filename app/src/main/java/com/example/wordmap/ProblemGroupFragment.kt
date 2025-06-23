package com.example.wordmap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.wordmap.databinding.FragmentProblemBinding
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * 어플리케이션 내의 주요 기능인 단어 문제를 푸는 기능을 담당하는 Fragment.
 * AI로부터 단어와 뜻, 오답 목록을 받아와 사용자에게 제시하고, 사용자의 버튼 클릭으로 오답을 체크한다.
 * 문제 풀이 진행 상태에 따라 다음 문제로 넘어가거나 완료 화면으로 이동한다.
 */

// AI로부터 파싱된 문제 정보를 담는 데이터 클래스
data class ParsedAiProblem(
    val word: String, // 단어
    val meaning: String, // 단어 해석 뜻
    val incorrectOptions: List<String> // 오답 버튼에 답을 단어를 위한 랜덤 리스트
)

class ProblemGroupFragment : Fragment() {

    private var _binding: FragmentProblemBinding? = null
    private val binding get() = _binding!!
    private var mainActivity: MainActivity? = null

    // AI ViewModel 연결
    private val problemAiViewModel: ProblemAiViewmodel by viewModels()

    // AI로부터 받은 문제 목록
    private val parsedProblemList = mutableListOf<ParsedAiProblem>()
    private var currentProblemDisplayIndex = 0 // AI가 생성한 문제 목록 내 현재 인덱스


    private var currentActualWord: String = "" // 현재 화면에 표시된 단어 (AI 제공)
    private var currentActualMeaning: String = "" // 현재 문제의 실제 의미 (AI 제공, 정답확인시 사용)


    companion object {
        private const val ARG_START_INDEX = "start_index" // 이 인자는 AI가 문제를 가져오면 의미가 달라질 수 있음
        const val LAST_PROBLEM_DIRECTION = "마지막"

        // newInstance는 유지하되, startIndex의 역할이 AI 문제 로딩 방식에 따라 달라질 수 있음을 인지
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

        // AI 문제 로딩 전에는 UI를 비워두거나 로딩 상태 표시
        binding.wordTextView.text = "AI 문제 로딩 중..."
        hideOptionButtons() // 초기에 옵션 버튼 숨김

        setupNavigationButtonsAndFinishButtonState(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAiData()
        setupNavigationButtons()
        setupFinishButton()

        // 예시: AI에게 문제 요청 (예: 10문제)
        if (parsedProblemList.isEmpty()) { // 아직 문제가 로드되지 않았다면 요청
            problemAiViewModel.generateProblems(wordCount = 10) // 예: 항상 10개의 새로운 문제 세트를 가져옴
        }
    }

    /**
     * AI ViewModel로부터 로딩 상태, 생성된 문제 목록, 발생한 오류 메시지를 관찰하고 UI에 반영.
     */

    private fun observeAiData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    problemAiViewModel.isLoading.collect { isLoading ->
//                        binding.problemLoadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE // 로딩바 ID 필요
                        if (!isLoading && problemAiViewModel.problemList.value.isNotBlank()) {
                            parseAndSetupProblems(problemAiViewModel.problemList.value)
                        } else if (!isLoading && parsedProblemList.isEmpty()) { // 로딩이 끝났는데 파싱된 문제가 없다면 : 오류일 경우
                            binding.wordTextView.text = "문제 로딩 실패."
                            hideOptionButtons()
                            setupNavigationButtonsAndFinishButtonState(false)
                        }
                    }
                }
                launch {
                    problemAiViewModel.errorMessage.collect { error ->
                        error?.let {
                            Toast.makeText(context, "AI Error: $it", Toast.LENGTH_LONG).show()
                            Log.e("ProblemGroupFragment", "AI Error: $it")
                            binding.wordTextView.text = "오류 발생: $it"
                            problemAiViewModel.errorMessageShown() // ViewModel에 오류 확인 알림
                        }
                    }
                }
            }
        }
    }

    /**
     * AI로부터 받은 원시 텍스트(rawText)를 파싱하여 ParsedAiProblem 객체 리스트(parsedProblemList)로 변환하고, 첫 번째 문제를 화면에 표시합니다.
     * @param rawText AI가 생성한 문제들이 포함된 원시 텍스트 문자열 "---" 로 작성방식을 지정하여 분할점을 잡음.
     */

    private fun parseAndSetupProblems(rawText: String) {
        parsedProblemList.clear()
        val problemBlocks = rawText.split("---").filter { it.trim().isNotEmpty() }

        problemBlocks.forEach { block ->
            val wordRegex = Pattern.compile("""Word:\s*(.*)""", Pattern.CASE_INSENSITIVE)
            val meaningRegex = Pattern.compile("""Meaning:\s*(.*)""", Pattern.CASE_INSENSITIVE)
            val incorrectOptionRegex = Pattern.compile("""Incorrect_Option_\d+:\s*(.*)""", Pattern.CASE_INSENSITIVE)


            var word: String? = null
            var meaning: String? = null
            val incorrectOptions = mutableListOf<String>()

            block.lines().forEach { line ->
                val trimmedLine = line.trim()
                var matcher = wordRegex.matcher(trimmedLine)
                if (matcher.find()) word = matcher.group(1)?.trim()
                matcher = meaningRegex.matcher(trimmedLine)
                if (matcher.find()) meaning = matcher.group(1)?.trim()
                matcher = incorrectOptionRegex.matcher(trimmedLine)
                if (matcher.find()) {
                    matcher.group(1)?.trim()?.let { incorrectOptions.add(it) }
                }
            }

            if (word != null && meaning != null && incorrectOptions.size >= 3) {
                // 실제 앱에서는 정확히 3개가 필요한지, 아니면 최소 3개인지 등에 따라 조건 변경 가능
                parsedProblemList.add(ParsedAiProblem(word!!, meaning!!, incorrectOptions.take(3))) // 최대 3개만 사용
            } else {
                Log.w("ProblemGroupFragment", "Problem block parsing incomplete or not enough incorrect options: $block")
            }
        }

        if (parsedProblemList.isNotEmpty()) {
            // mainActivity의 problemCount는 현재 "풀고 있는" 문제의 인덱스로 사용될 수 있음
            // AI가 10개를 주면, 이 10개 내에서 problemCount가 0부터 9까지 증가하도록 관리
            currentProblemDisplayIndex = mainActivity?.problemCount ?: 0 // MainActivity의 진행 상황에 맞춤
            if (currentProblemDisplayIndex >= parsedProblemList.size) { // 혹시 모를 범위 초과 방지
                currentProblemDisplayIndex = 0
            }
            displayCurrentAiProblem()
        } else {
            binding.wordTextView.text = "파싱된 문제가 없습니다."
            hideOptionButtons()
            setupNavigationButtonsAndFinishButtonState(false)
        }
    }

    /**
     * 현재 문제 인덱스(currentProblemDisplayIndex)에 해당하는 AI 문제를 화면에 표시한다.
     * 단어, 보기(정답 포함)를 설정하고, UI 상태를 초기화한다.
     */
    private fun displayCurrentAiProblem() {
        if (currentProblemDisplayIndex < 0 || currentProblemDisplayIndex >= parsedProblemList.size) {
            Log.e("ProblemGroupFragment", "Invalid currentProblemDisplayIndex: $currentProblemDisplayIndex")
            binding.wordTextView.text = "문제를 표시할 수 없습니다."
            hideOptionButtons()
            setupNavigationButtonsAndFinishButtonState(false)
            return
        }

        val problem = parsedProblemList[currentProblemDisplayIndex]
        currentActualWord = problem.word    // AI가 제공한 단어를 currentWord 변수에 할당
        currentActualMeaning = problem.meaning // AI가 제공한 의미를 저장 (checkAnswer에서 사용)
        Log.d("ProblemGroupFragment", "Displaying word: ${problem.word}")

        binding.wordTextView.text = currentActualWord // AI가 제공한 단어를 TextView에 표시

        // setupProblems는 이제 AI가 제공한 의미로 정답을 설정하고 보기 버튼 리스너를 설정
        setupProblemOptionsAndListeners(currentActualMeaning, problem.incorrectOptions) // 의미 전달

        // UI 상태 업데이트
        resetProblemUIStateForNewProblem() // 새 문제 표시 전 UI 초기화 (옵션버튼 보이기, 의미 숨기기 등)
        checkAndUpdateFinishButtonVisibility() // 네비게이션 및 완료 버튼 상태 업데이트
    }

    /**
     * 주어진 정답(correctMeaning)과 AI가 제공한 오답 목록(incorrectMeaningsFromAI)을 조합하여
     * 4개의 보기 버튼에 텍스트를 설정하고, 각 버튼 클릭 시 정답 여부를 확인하는 리스너를 설정한다.
     * @param correctMeaning 현재 문제의 정답 뜻
     * @param incorrectMeaningsFromAI AI가 제공한 오답 목록
     */
    private fun setupProblemOptionsAndListeners(correctMeaning: String, incorrectMeaningsFromAI: List<String>) {
        val options = mutableListOf<String>()
        options.add(correctMeaning) // 정답 추가
        options.addAll(incorrectMeaningsFromAI) // AI가 제공한 오답들 추가

        // 옵션이 4개가 되도록 보장 (만약 AI가 3개 미만의 오답을 제공했을 경우 대비 - 현재 로직상으론 필터링됨)
        val fixedIncorrectOptions = listOf("다른 의미 1", "다른 의미 2", "틀린 보기 더미")
        var fixedOptionIdx = 0
        while (options.size < 4 && fixedOptionIdx < fixedIncorrectOptions.size) {
            if (!options.contains(fixedIncorrectOptions[fixedOptionIdx])) {
                options.add(fixedIncorrectOptions[fixedOptionIdx])
            }
            fixedOptionIdx++
        }

        // 최종적으로 옵션 개수가 4개를 넘을 경우 : 4개만 사용
        val finalOptions = options.take(4).toMutableList()
        finalOptions.shuffle() // 옵션 순서 섞기

        val optionButtons = listOf(binding.option1, binding.option2, binding.option3, binding.option4)
        optionButtons.forEachIndexed { index, button ->
            if (index < finalOptions.size) {
                button.text = finalOptions[index]
                button.visibility = View.VISIBLE
                button.setOnClickListener {
                    // isCorrect 비교 시에는 항상 원본 정답(correctMeaning)과 비교해야 합니다.
                    checkAnswer(finalOptions[index] == correctMeaning, correctMeaning)
                }
            } else {
                button.visibility = View.GONE // 옵션이 4개 미만일 경우 (이론상 발생 안 함)
            }
        }
    }

    /**
     * 사용자가 선택한 답이 정답인지 확인하고, 그에 따른 UI 변경 및 처리를 수행한다.
     * @param isCorrect 사용자의 선택이 정답인지 여부
     * @param sentenceMeaningFromAI 현재 문제의 실제 정답 뜻 (정답일 경우 화면에 표시됨)
     */
    private fun checkAnswer(isCorrect: Boolean, sentenceMeaningFromAI: String) {
        if (isCorrect) {
            binding.meaningTextView.text = sentenceMeaningFromAI // AI가 제공한 정확한 의미를 표시
            binding.meaningTextView.visibility = View.VISIBLE
            hideOptionButtons()

            checkAndUpdateFinishButtonVisibility() // 네비게이션 버튼 상태 업데이트 호출

            // mainActivity?.incrementProblemCount() // 정답을 맞혔다고 바로 카운트 증가하지 않음. 네비게이션 시 증가.
        } else {
            Toast.makeText(context, "오답입니다!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideOptionButtons() {
        binding.option1.visibility = View.GONE
        binding.option2.visibility = View.GONE
        binding.option3.visibility = View.GONE
        binding.option4.visibility = View.GONE
    }

    private fun showOptionButtons() {
        binding.option1.visibility = View.VISIBLE
        binding.option2.visibility = View.VISIBLE
        binding.option3.visibility = View.VISIBLE
        binding.option4.visibility = View.VISIBLE
    }

    // 새 문제가 표시될 때 UI 상태를 초기화하는 함수
    private fun resetProblemUIStateForNewProblem() {
        binding.meaningTextView.visibility = View.GONE // 의미 숨기기
        showOptionButtons() // 옵션 버튼 다시 보이기
        // 네비게이션 버튼은 checkAndUpdateFinishButtonVisibility에서 관리
    }

    private fun checkAndUpdateFinishButtonVisibility() {
        mainActivity?.let { activity ->
            val currentSolvedCount = activity.problemCount // MainActivity에서 관리하는 "푼" 문제 수
            val totalProblemsFromAI = parsedProblemList.size // AI가 제공한 전체 문제 수

            if (totalProblemsFromAI == 0) { // AI 문제가 없으면 아무것도 안함
                setupNavigationButtonsAndFinishButtonState(false)
                binding.btnFinish.visibility = View.GONE
                return
            }

            val isAnswerRevealed = binding.meaningTextView.visibility == View.VISIBLE

            if (currentProblemDisplayIndex == totalProblemsFromAI - 1 && isAnswerRevealed) {
                binding.wordTextView.text = "모든 문제를 다 푸셨습니다!"
                binding.meaningTextView.visibility = View.GONE
                binding.btnFinish.visibility = View.VISIBLE
                setupNavigationButtonsAndFinishButtonState(false)
            } else if (isAnswerRevealed) {
                binding.btnFinish.visibility = View.GONE
                setupNavigationButtonsAndFinishButtonState(true)
            } else {
                binding.btnFinish.visibility = View.GONE
                setupNavigationButtonsAndFinishButtonState(false)
            }
        } ?: run {
            setupNavigationButtonsAndFinishButtonState(false)
            binding.btnFinish.visibility = View.GONE
        }
    }

    // 네비게이션 버튼들과 완료 버튼의 기본 상태를 설정하는 함수
    private fun setupNavigationButtonsAndFinishButtonState(enableNavigation: Boolean) {
        binding.btnUp.visibility = if (enableNavigation) View.VISIBLE else View.GONE
        binding.btnLeft.visibility = if (enableNavigation) View.VISIBLE else View.GONE
        binding.btnRight.visibility = if (enableNavigation) View.VISIBLE else View.GONE
    }


    private fun setupNavigationButtons() {
        // 이 리스너들은 AI 문제가 로드되고, 정답을 맞힌 후에만 활성화되어야 함
        binding.btnUp.setOnClickListener { navigateToNextProblemWithAI("상단") }
        binding.btnLeft.setOnClickListener { navigateToNextProblemWithAI("좌측") }
        binding.btnRight.setOnClickListener { navigateToNextProblemWithAI("우측") }
    }

    /**
     * 사용자가 선택한 방향(direction)으로 다음 AI 문제로 이동합니다.
     * MainActivity에 현재 푼 단어와 방향을 저장하고, 푼 문제 수를 증가시킨 후 다음 문제를 표시합니다.
     * @param direction 사용자가 선택한 이동 방향 ("상단", "좌측", "우측")
     */
    private fun navigateToNextProblemWithAI(direction: String) {
        // MainActivity에 현재 "푼" 단어와 방향 저장
        mainActivity?.addSolvedProblem(currentActualWord, direction)
        mainActivity?.incrementProblemCount() // MainActivity의 "푼 문제 수" 증가

        // 다음 AI 문제로 이동
        if (currentProblemDisplayIndex < parsedProblemList.size - 1) {
            currentProblemDisplayIndex++
            Log.d("ProblemGroupFragment", "New currentProblemDisplayIndex: $currentProblemDisplayIndex")
            displayCurrentAiProblem()
        } else {
            // 모든 AI 문제를 다 풀었을 경우의 로직 (이론상 checkAndUpdateFinishButtonVisibility에서 완료버튼이 떠야 함)
            Log.d("ProblemGroupFragment", "All AI problems solved. Finish button should be visible.")
        }
    }


    // 기존 resetProblemUI는 resetProblemUIStateForNewProblem으로 대체 및 확장됨

    private fun setupFinishButton() {
        binding.btnFinish.setOnClickListener {
            if (currentProblemDisplayIndex == parsedProblemList.size - 1 && binding.meaningTextView.visibility == View.GONE) { // 모든 문제를 풀고 완료버튼 텍스트가 표시된 후
                mainActivity?.addSolvedProblem(currentActualWord, LAST_PROBLEM_DIRECTION)
            } else if (parsedProblemList.isEmpty() && mainActivity?.problemCount == 0) {
                mainActivity?.addSolvedProblem("N/A", LAST_PROBLEM_DIRECTION)
            }
            mainActivity?.navigateToCompletion()
        }
    }

    //메모리 누수 방지
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}