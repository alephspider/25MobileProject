package com.example.wordmap

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.vertexai.type.asTextOrNull
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch


/**
 * google gemini API를 사용하기 위한 ViewModel
 * Firebase에서 제공해주는 backend를 사용하여 AI와의 통신을 담당
 * 기본으로 제공해주는 크래딧을 사용하기에 한계가 있으며 , 9월 23일 이후 유료 전환
 *
 */

class ProblemAiViewmodel : ViewModel() {
    // 문제 목록 또는 AI 응답을 저장할 StateFlow
    private val _problemList = MutableStateFlow("")
    val problemList: MutableStateFlow<String> = _problemList

    // 로딩 상태를 알리는 StateFlow
    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 오류 메시지를 전달할 StateFlow
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val vertexAI = Firebase.vertexAI
    private val model = vertexAI.generativeModel("gemini-2.0-flash")

    /**
     * 지정된 조건으로 AI에게 문제 생성을 요청합니다.
     * @param wordCount 생성할 단어/문제의 개수
     * @param topic 문제의 주제 (선택 사항)
     * @param pos 원하는 품사 (선택 사항)
     * @param excludeWords 제외할 단어 목록 (선택 사항)
     */
    fun generateProblems(
        wordCount: Int = 10,
        topic: String? = null,
        pos: String? = null,
        excludeWords: List<String>? = null
    ) {

        viewModelScope.launch {
            _isLoading.value = true
            _problemList.value = "" // 이전 결과 초기화
            _errorMessage.value = null    // 이전 오류 초기화

            // 고등학생 수준 영단어 추출 프롬프트 (이전 답변 내용 활용)
            val prompt = """
                너는 지금부터 고등학생 수준의 영어 어휘 학습을 돕는 AI 어시스턴트야.
                다음 조건에 맞춰서 영단어 목록을 생성해줘.

                **조건:**
                1. 수준: 대한민국 고등학교 영어 교육과정에서 다루는 수준의 단어 (고1 ~ 고3 공통 및 심화 어휘 포함)
                2. 개수: ${wordCount}개
                3. 형식:
                    * 영단어
                    * 한국어 뜻 (가장 대표적인 뜻 1개)
                    * 오답 선택지 3개 (문맥에 맞는 그럴듯한 오답)
                4. 주제 (선택 사항): ${topic ?: "다양한 주제"}
                5. 품사 (선택 사항): ${pos ?: "다양하게"}
                6. 제외할 단어 (선택 사항): ${excludeWords?.joinToString(", ") ?: "ubiquitous"}
                7. 출력 형식: 각 단어 정보를 명확히 구분하여 목록으로 제공. 각 단어의 시작은 "---"로 구분해줘.
                   각 항목은 다음 형식을 따라야 해:
                   Word: [단어]
                   Meaning: [뜻]
                   Incorrect_Option_1: [오답1]
                   Incorrect_Option_2: [오답2]
                   Incorrect_Option_3: [오답3]

                **요청:**
                위 조건, 특히 "출력 텍스트 형식"을 정확히 따라서 영단어 목록을 생성해줘.
                예를 들어, 첫 번째 단어가 "apple"이고 뜻이 "사과", 오답이 "바나나", "컴퓨터", "책" 이라면 다음과 같이 출력되어야 해:
                Word: apple
                Meaning: 사과
                Incorrect_Option_1: 바나나 
                Incorrect_Option_2: 컴퓨터
                Incorrect_Option_3: 책
                ---
            """.trimIndent()

            Log.d("ProblemAiVM", "Generated Prompt: \n$prompt")

            try {
                val responseStream = model.generateContentStream(prompt)

                responseStream
                    .catch { e ->
                        Log.e("ProblemAiVM", "Error in stream collection: ${e.message}", e)
                        _errorMessage.value = "AI 응답 스트림 오류: ${e.message}"
                        _isLoading.value = false
                    }
                    .collect { chunk ->
                        val textChunk = chunk.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.asTextOrNull()
                        if (textChunk != null) {
                            _problemList.value += textChunk
                            Log.d("ProblemAiVM", "Received chunk: $textChunk")
                        } else {
                            Log.w("ProblemAiVM", "Received non-text or empty chunk: ${chunk.candidates.firstOrNull()?.content?.parts}")
                        }

                        // 스트림 종료 확인 (선택적 로깅)
                        chunk.candidates.firstOrNull()?.finishReason?.let { reason ->
                            Log.i("ProblemAiVM", "Stream finished with reason: $reason")
                        }
                    }
            } catch (e: Exception) {
                Log.e("ProblemAiVM", "Error calling generateContentStream: ${e.message}", e)
                _errorMessage.value = "AI 요청 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun errorMessageShown() {
        _errorMessage.value = null
    }

}