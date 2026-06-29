package com.example.realkick

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.realkick.ui.TeamSelectionScreen
import com.example.realkick.ui.ARGameScreen
import com.example.realkick.ui.HistoryARScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(TeamSelection)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<TeamSelection> {
          TeamSelectionScreen(
            onTeamSelected = { teamName ->
              backStack.add(ARGame(teamName))
            },
            onHistorySelected = { modelId ->
              backStack.add(HistoryAR(modelId))
            }
          )
        }
        entry<ARGame> { key ->
          ARGameScreen(
            teamName = key.teamName,
            onBack = {
              backStack.removeLastOrNull()
            }
          )
        }
        entry<HistoryAR> { key ->
          HistoryARScreen(
            modelId = key.modelId,
            onBack = {
              backStack.removeLastOrNull()
            }
          )
        }
      },
  )
}
