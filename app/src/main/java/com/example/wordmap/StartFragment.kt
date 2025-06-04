package com.example.wordmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class StartFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = StartFragment() // 간단한 newInstance
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_start, container, false)

        val startButton: Button = view.findViewById(R.id.startButton)
        startButton.setOnClickListener {
            // 문제 풀이 시작
            navigateToProblem()
        }

        return view
    }

    private fun navigateToProblem() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ProblemGroupFragment.newInstance(0))
            .commit()
    }
}
