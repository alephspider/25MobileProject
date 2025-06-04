package com.example.wordmap

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wordmap.databinding.FragmentCompletionBinding

class CompletionFragment : Fragment() {

    private var _binding: FragmentCompletionBinding? = null
    private val binding get() = _binding!!

    private var solvedProblems: ArrayList<SolvedProblemData>? = null
    private var mainActivity: MainActivity? = null // MainActivity 참조 추가

    companion object {
        private const val ARG_SOLVED_PROBLEMS = "solved_problems"

        fun newInstance(solvedProblems: ArrayList<SolvedProblemData>): CompletionFragment {
            val fragment = CompletionFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_SOLVED_PROBLEMS, solvedProblems)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) { // onAttach에서 MainActivity 참조 가져오기
        super.onAttach(context)
        if (context is MainActivity) {
            mainActivity = context
        } else {
            throw RuntimeException("$context must implement MainActivity")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // ... (기존 코드 유지) ...
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                solvedProblems = it.getParcelableArrayList(ARG_SOLVED_PROBLEMS, SolvedProblemData::class.java)
            } else {
                @Suppress("DEPRECATION")
                solvedProblems = it.getParcelableArrayList(ARG_SOLVED_PROBLEMS)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompletionBinding.inflate(inflater, container, false)

        solvedProblems?.let {
            if (it.isNotEmpty()) {
                binding.pathDrawingView.setSolvedProblems(it)
            } else {
                binding.completionTitleTextView.text = "푼 문제가 없습니다."
                binding.btnRestart.visibility = View.GONE // 푼 문제가 없으면 다시 시작 버튼도 숨김 (선택 사항)
            }
        }

        binding.btnRestart.setOnClickListener {
            mainActivity?.restartProblems() // MainActivity의 메서드 호출
        }

        return binding.root
    }

    override fun onDetach() { // onDetach에서 MainActivity 참조 해제
        super.onDetach()
        mainActivity = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}