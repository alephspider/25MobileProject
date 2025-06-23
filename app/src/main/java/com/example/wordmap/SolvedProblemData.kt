package com.example.wordmap

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 사용자가 푼 문제와 선택한 방향을 담은 데이터 클래스
 */

@Parcelize
data class SolvedProblemData(
    val word: String,
    val direction: String
) : Parcelable