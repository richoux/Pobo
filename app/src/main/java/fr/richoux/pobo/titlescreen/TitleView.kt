package fr.richoux.pobo.titlescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.richoux.pobo.Screen
import fr.richoux.pobo.gamescreen.GameViewModel
import fr.richoux.pobo.ui.PoboTheme
import fr.richoux.pobo.R
import fr.richoux.pobo.gamescreen.customDialogModifier
import androidx.compose.material.AlertDialog as AlertDialog

@Composable
fun TitleView(navController: NavController, gameViewModel: GameViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.primaryVariant)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = context.getResources().getString(R.string.app_name_jp),
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary
        )
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
        DropMenuButton(
            navController,
            gameViewModel,
            text = context.getResources().getString(R.string.ai_game_fr),
            blue = context.getResources().getString(R.string.blue_fr),
            red = context.getResources().getString(R.string.red_fr)
        )
//        GameButton(
//            onClick = { askForPlayerColor = true },
//            text = context.getResources().getString(R.string.ai_game_fr)
//        )
        Spacer(modifier = Modifier.height(16.dp))
        GameButton(
            onClick = { newGame(navController, gameViewModel, p1IsAI = true, p2IsAI = true) },
            text = context.getResources().getString(R.string.ai_vs_ai_game_fr)
        )
        Spacer(
            modifier = Modifier.weight(1f)
        )
        Text(
            "v0.5.1",
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

@Composable
private fun DropMenuButton(navController: NavController, gameViewModel: GameViewModel, text: String, blue: String, red: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colors.primaryVariant),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = text, style = MaterialTheme.typography.h4)
        }
        Row()
        {
            Button(
                onClick = { newGame(navController, gameViewModel, p1IsAI = false, p2IsAI = true) },
                enabled = expanded,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = blue,
                    style = MaterialTheme.typography.h4,
                    color = Color.Blue
                )
            }
            Button(
                onClick = { newGame(navController, gameViewModel, p1IsAI = true, p2IsAI = false) },
                enabled = expanded,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = red,
                    style = MaterialTheme.typography.h4,
                    color = Color.Red
                )
            }
        }
//
//        DropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { expanded = false },
//            modifier = Modifier
//                .fillMaxWidth()
//        ) {
//            DropdownMenuItem(
//                onClick = { newGame(navController, gameViewModel, p1IsAI = false, p2IsAI = true) }
//            ) {
//                Text("Play Blue")
//            }
//            DropdownMenuItem(
//                onClick = { newGame(navController, gameViewModel, p1IsAI = true, p2IsAI = false) }
//            ) {
//                Text("Play Red")
//            }
//        }
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