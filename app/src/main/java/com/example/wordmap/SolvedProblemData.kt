package com.example.wordmap

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SolvedProblemData(
    val word: String,
    val direction: String
) : Parcelable