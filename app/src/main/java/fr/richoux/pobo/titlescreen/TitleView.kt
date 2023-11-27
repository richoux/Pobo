package fr.richoux.pobo.titlescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.richoux.pobo.Screen
import fr.richoux.pobo.gamescreen.GameViewModel
import fr.richoux.pobo.ui.PoboTheme
import fr.richoux.pobo.R

@Composable
fun TitleView(navController: NavController, gameViewModel: GameViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.primaryVariant).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = context.getResources().getString(R.string.app_name_jp), style = MaterialTheme.typography.h2, color = MaterialTheme.colors.onPrimary)
        Spacer(modifier = Modifier.height(32.dp))
        GameButton(
            onClick = { resume(navController, gameViewModel) },
            enabled = gameViewModel.hasStarted,
            text = context.getResources().getString(R.string.resume_fr)
        )
        Spacer(modifier = Modifier.height(16.dp))
        GameButton(
            onClick = { newGame(navController, gameViewModel, p1IsAI = false, p2IsAI = false) },
            text = context.getResources().getString(R.string.human_game_fr)
        )
        Spacer(modifier = Modifier.height(16.dp))
        GameButton(
            onClick = { newGame(navController, gameViewModel, p1IsAI = false, p2IsAI = true) },
            text = context.getResources().getString(R.string.ai_game_fr)
        )
        Spacer(modifier = Modifier.height(16.dp))
        GameButton(
            onClick = { newGame(navController, gameViewModel, p1IsAI = true, p2IsAI = true) },
            text = context.getResources().getString(R.string.ai_vs_ai_game_fr)
        )
        Spacer(
            modifier = Modifier.weight(1f)
        )
        Text(
            "v0.5.0",
            color = MaterialTheme.colors.onPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun GameButton(onClick: () -> Unit, text: String, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = text, style = MaterialTheme.typography.h4)
    }
}

@Preview
@Composable
private fun GameButtonPreview() {
    PoboTheme {
        GameButton(onClick = { }, text = "Two Players")
    }
}

private fun newGame(
    navController: NavController,
    gameViewModel: GameViewModel,
    p1IsAI: Boolean,
    p2IsAI: Boolean
) {
    gameViewModel.newGame(p1IsAI, p2IsAI)
    navController.navigate(Screen.Game.route)
}

private fun resume(
    navController: NavController,
    gameViewModel: GameViewModel
) {
    gameViewModel.resume()
    navController.navigate(Screen.Game.route)
}