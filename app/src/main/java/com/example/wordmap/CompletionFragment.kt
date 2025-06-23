package com.example.wordmap

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wordmap.databinding.FragmentCompletionBinding

/**
 * 완료 화면을 표시하는 Fragment.
 * PathDrawingView에서 구현한 로직을 가져와서 이미지를 사용자에게 보여준다.
 */


class CompletionFragment : Fragment() {

    private var _binding: FragmentCompletionBinding? = null
    private val binding get() = _binding!!

    private var solvedProblems: ArrayList<SolvedProblemData>? = null // 이전에서 푼 문제를 저장하는 리스트
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

        //PathDraingView에서 작성한 함수를 호출하여 최종 경로를 그림
        solvedProblems?.let {
            if (it.isNotEmpty()) {
                binding.pathDrawingView.setSolvedProblems(it)
            } else {
                binding.completionTitleTextView.text = "푼 문제가 없습니다."
                binding.btnRestart.visibility = View.GONE
            }
        }
        //다시시작 버튼 호출
        binding.btnRestart.setOnClickListener {
            mainActivity?.restartProblems()
        }

        return binding.root
    }


    //메모리 누수 방지
    override fun onDetach() {
        super.onDetach()
        mainActivity = null
    }

    //메모리 누수 방지
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}