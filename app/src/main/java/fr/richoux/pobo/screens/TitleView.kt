package fr.richoux.pobo.screens

import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import fr.richoux.pobo.Screen
import fr.richoux.pobo.screens.gamescreen.GameViewModel
import fr.richoux.pobo.R
import fr.richoux.pobo.ui.LockScreenOrientation
import fr.richoux.pobo.ui.lightgreen
import kotlin.random.Random

@Composable
fun TitleView(navController: NavController, gameViewModel: GameViewModel) {
  LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.primaryVariant)
      .padding(16.dp)
      .disableSplitMotionEvents(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = stringResource(id = R.string.app_name),
      style = MaterialTheme.typography.h2,
      color = MaterialTheme.colors.onPrimary
    )
    Spacer(modifier = Modifier.height(32.dp))
    GameButton(
//      onClick = { resume(navController, gameViewModel) },
      onClick = { resume(navController) },
      enabled = gameViewModel.hasStarted,
      text = stringResource(id = R.string.resume)
    )
    Spacer(modifier = Modifier.height(16.dp))
    DropMenuButton(
      navController,
      gameViewModel,
      text = stringResource(id = R.string.ai_game)
    )
    Spacer(modifier = Modifier.height(16.dp))
    GameButtonWithIcon(
      onClick = { newGame(navController, gameViewModel, p1IsAI = false, p2IsAI = false) },
      text = stringResource(id = R.string.human_game),
      icon = R.drawable.two_players_icon
    )
//    GameButton(
//      onClick = { newGame(navController, gameViewModel, p1IsAI = true, p2IsAI = true) },
//      text = stringResource(id = R.string.ai_vs_ai_game)
//    )
//    Spacer(modifier = Modifier.height(16.dp))
//    GameButton(
//      onClick = { newGame(navController, gameViewModel, p1IsAI = true, p2IsAI = true, xp = true) },
//      text = "Run XP"
//    )
    Spacer(
      modifier = Modifier.weight(1f)
    )
    Text(
      text = "v1.1.2",
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
private fun GameButtonWithIcon(onClick: () -> Unit, text: String, icon: Int, enabled: Boolean = true) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Image(
        painter = painterResource(id = icon),
        modifier = Modifier
          .padding(4.dp)
          .size(42.dp),
        contentDescription = "2 Players"
      )
      Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
      Text(
        text = text,
        style = MaterialTheme.typography.h4
      )

    }
  }
}

@Composable
private fun DropMenuButton(
  navController: NavController,
  viewModel: GameViewModel,
  text: String
) {
  val soloGame by viewModel.soloGame.collectAsStateWithLifecycle()
  var expanded by remember { mutableStateOf(false) }
  BoxWithConstraints(
    modifier = Modifier.fillMaxWidth()
  ) {
    val width = maxWidth
    GameButtonWithIcon(
      onClick = { expanded = !expanded },
      text = text,
      icon = R.drawable.one_player_icon
    )
    DropdownMenu(
      expanded = expanded,
      properties = PopupProperties(focusable = true),
      onDismissRequest = { expanded = false },
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colors.primaryVariant)
          .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        SelectionRow(
          text = stringResource(id = R.string.select_first_player),
          width = width,
          items = listOf(0, 1, 2), // Blue, Red, Random
          selection = soloGame.playAs,
          action_1 = { viewModel.playAsBlue() },
          action_2 = { viewModel.playAsRed() },
          action_3 = { viewModel.playAsRandom() },
          id_icon_1 = R.drawable.blue_bo,
          id_icon_2 = R.drawable.red_bo,
          id_icon_3 = R.drawable.random
        )
        SelectionRow(
          text = stringResource(id = R.string.ai_level),
          width = width,
          items = listOf(2, 1, 0), // easy, medium, hard
          selection = soloGame.aiLevel,
          action_1 = { viewModel.ai_hard() },
          action_2 = { viewModel.ai_medium() },
          action_3 = { viewModel.ai_easy() },
          id_icon_1 = R.drawable.ai_hard,
          id_icon_2 = R.drawable.ai_medium,
          id_icon_3 = R.drawable.ai_easy
        )
          Button(
            onClick = {
              when(soloGame.playAs) {
                0 -> newGame(
                  navController,
                  viewModel,
                  p1IsAI = false,
                  p2IsAI = true,
                  aiLevel = soloGame.aiLevel
                ) //play as Blue
                1 -> newGame(
                  navController,
                  viewModel,
                  p1IsAI = true,
                  p2IsAI = false,
                  aiLevel = soloGame.aiLevel
                ) //play as Red
                2 -> newGame(
                  navController,
                  viewModel,
                  p1IsAI = false,
                  p2IsAI = false,
                  random = true,
                  aiLevel = soloGame.aiLevel
                ) //play Random
                else -> {}
              }
            },
            enabled = (soloGame.playAs >= 0 && soloGame.aiLevel >= 0),
            modifier = Modifier.padding(12.dp)
          ) {
            Text(
              text = stringResource(id = R.string.launch_solo_game),
              style = MaterialTheme.typography.h4
            )
          }
        }
      }
    }
}

@Composable
fun SelectionRow(
  text: String,
  width: Dp,
  items: List<Int>,
  selection: Int,
  action_1: () -> Unit,
  action_2: () -> Unit,
  action_3: () -> Unit,
  id_icon_1: Int,
  id_icon_2: Int,
  id_icon_3: Int
) {
  Text(
    text = text,
    style = MaterialTheme.typography.h4,
    color = MaterialTheme.colors.onPrimary
  )
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
  ) {
    items.forEach { item ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
          .selectable(
            selected = (selection == item),
            onClick = {
              when(item) {
                0 -> action_1()
                1 -> action_2()
                2 -> action_3()
              }
            },
            role = Role.RadioButton
          )
          .size(width/3)
      ) {
        IconToggleButton(
          checked = (selection == item),
          onCheckedChange = {
            when(item) {
              0 -> action_1()
              1 -> action_2()
              2 -> action_3()
            }
          },
          modifier = Modifier.size( min((width/16), 32.dp) )
        ) {
          Icon(
            painter = painterResource(
              if(selection == item) {
                R.drawable.baseline_check_circle_outline_24
              } else {
                R.drawable.ic_baseline_circle_24
              }
            ),
            contentDescription = null,
            tint = if(selection == item) {
              lightgreen
            } else {
              MaterialTheme.colors.primary
            }
          )
        }
        Image(
          painter = painterResource(
            id = when(item) {
              0 -> id_icon_1
              1 -> id_icon_2
              else -> id_icon_3
            }
          ),
          contentDescription = "",
          modifier = Modifier
            .size( min((width/4), 120.dp) )
        )
      }
    }
  }
}

private fun newGame(
  navController: NavController,
  gameViewModel: GameViewModel,
  p1IsAI: Boolean,
  p2IsAI: Boolean,
  xp: Boolean = false,
  random: Boolean = false,
  aiLevel: Int = -1
) {
  gameViewModel.newGame(navController, p1IsAI, p2IsAI, xp, random, aiLevel)
  navController.navigate(Screen.Game.route)
}

private fun resume(
  navController: NavController,
) {
  navController.navigate(Screen.Game.route)
}

//@Preview(locale = "ja")
//@Composable
//private fun TitleViewPreview(hasStarted: Boolean = false) {
//  Column(
//    modifier = Modifier
//      .fillMaxSize()
//      .background(MaterialTheme.colors.primaryVariant)
//      .padding(16.dp),
//    horizontalAlignment = Alignment.CenterHorizontally
//  ) {
//    Text(
//      text = stringResource(id = R.string.app_name),
//      style = MaterialTheme.typography.h2,
//      color = MaterialTheme.colors.onPrimary
//    )
//    Spacer(modifier = Modifier.height(32.dp))
//    GameButton(
//      onClick = {},
//      enabled = hasStarted,
//      text = stringResource(id = R.string.resume)
//    )
//    Spacer(modifier = Modifier.height(16.dp))
//    DropMenuButtonPreview(
//      text = stringResource(id = R.string.ai_game)
//    )
//    Spacer(modifier = Modifier.height(16.dp))
//    GameButtonWithIcon(
//      onClick = { },
//      text = stringResource(id = R.string.human_game),
//      icon = R.drawable.two_players_icon
//    )
//    Spacer(
//      modifier = Modifier.weight(1f)
//    )
//    Text(
//      "vx.y.z",
//      color = MaterialTheme.colors.onPrimary,
//      modifier = Modifier.align(Alignment.CenterHorizontally)
//    )
//  }
//}

@Preview
@Composable
private fun DropMenuButtonPreview(
  text: String = stringResource(id = R.string.ai_game)
) {
  BoxWithConstraints(
    modifier = Modifier.fillMaxWidth()
  ) {
    val width = maxWidth
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.primaryVariant)
      .padding(vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    SelectionRow(
      text = stringResource(id = R.string.select_first_player),
      width = width,
      items = listOf(0,1,2), // Blue, Red, Random
      selection = 0,
      action_1 = {},
      action_2 = {},
      action_3 = {},
      id_icon_1 = R.drawable.blue_bo,
      id_icon_2 = R.drawable.red_bo,
      id_icon_3 = R.drawable.random
    )
    SelectionRow(
      text = stringResource(id = R.string.ai_level),
      width = width,
      items = listOf(2,1,0), // easy, medium, hard
      selection = 0,
      action_1 = {},
      action_2 = {},
      action_3 = {},
      id_icon_1 = R.drawable.ai_hard,
      id_icon_2 = R.drawable.ai_medium,
      id_icon_3 = R.drawable.ai_easy
    )
    Button(
      onClick = {},
      enabled = false,
      modifier = Modifier.padding(12.dp)
    ) {
      Text(
        text = stringResource(id = R.string.launch_solo_game),
        style = MaterialTheme.typography.h4
      )
    }
    Text(
      text = "${width}"
    )
  }
}
}