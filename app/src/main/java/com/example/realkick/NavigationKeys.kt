package com.example.realkick

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object TeamSelection : NavKey

@Serializable
data class ARGame(val teamName: String) : NavKey

@Serializable
data class HistoryAR(val modelId: String) : NavKey
