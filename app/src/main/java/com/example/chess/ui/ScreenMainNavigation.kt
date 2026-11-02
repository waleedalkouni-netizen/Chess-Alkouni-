package com.example.chess.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.chess.ui.screens.*

@Composable
fun ScreenMainNavigation(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F0B1A)
    ) {
        when (val screen = uiState.currentScreen) {
            is CurrentScreen.Home -> {
                HomeScreen(viewModel = viewModel, uiState = uiState)
            }
            is CurrentScreen.Game3D -> {
                BackHandler { viewModel.navigateTo(CurrentScreen.Home) }
                Chess3DScreen(viewModel = viewModel, uiState = uiState)
            }
            is CurrentScreen.Puzzles -> {
                BackHandler { viewModel.navigateTo(CurrentScreen.Home) }
                PuzzlesScreen(viewModel = viewModel)
            }
            is CurrentScreen.History -> {
                BackHandler { viewModel.navigateTo(CurrentScreen.Home) }
                HistoryScreen(viewModel = viewModel, uiState = uiState)
            }
            is CurrentScreen.Settings -> {
                BackHandler { viewModel.navigateTo(CurrentScreen.Home) }
                SettingsScreen(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}
