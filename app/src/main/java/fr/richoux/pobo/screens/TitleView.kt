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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import fr.richoux.pobo.Screen
import fr.richoux.pobo.screens.gamescreen.GameViewModel
import fr.richoux.pobo.R
import fr.richoux.pobo.ui.LockScreenOrientation
import kotlin.random.Random

@Composable
fun TitleView(navController: NavController, gameViewModel: GameViewModel) {
  LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.primaryVariant)
      .padding(16.dp),
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
      "v1.1.0",
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
  Box {
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
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = stringResource(id = R.string.select_first_player),
          style = MaterialTheme.typography.h4,
          color = MaterialTheme.colors.onPrimary
        )
          Row(
            horizontalArrangement = Arrangement.Center
          ) {
            val items = listOf(0,1,2) // Blue, Red, Random
            Row(
              modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              items.forEach { item ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .selectable(
                      selected = (soloGame.playAs == item),
                      onClick = {
                        when(item) {
                          0 -> viewModel.playAsBlue()
                          1 -> viewModel.playAsRed()
                          2 -> viewModel.playAsRandom()
                        }
                      },
                      role = Role.RadioButton
                    )
                    .padding(8.dp)
                ) {
                  IconToggleButton(
                    checked = (soloGame.playAs == item),
                    onCheckedChange = {
                      when(item) {
                        0 -> viewModel.playAsBlue()
                        1 -> viewModel.playAsRed()
                        2 -> viewModel.playAsRandom()
                      }
                    },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(
                      painter = painterResource(
                        if(soloGame.playAs == item) {
                          R.drawable.ic_baseline_check_circle_24
                        } else {
                          R.drawable.ic_baseline_circle_24
                        }
                      ),
                      contentDescription = null,
                      tint = MaterialTheme.colors.primary
                    )
                  }
                  Image(
                    painter = painterResource(
                      id = when(item) {
                        0 -> R.drawable.blue_bo
                        1 -> R.drawable.red_bo
                        else -> R.drawable.random
                      }
                    ),
                    contentDescription = "",
                    modifier = Modifier.size(92.dp)
                  )
                }
              }
            }
          }
        Text(
          text = stringResource(id = R.string.ai_level),
          style = MaterialTheme.typography.h4,
          color = MaterialTheme.colors.onPrimary
        )
        Row(
            horizontalArrangement = Arrangement.Center
          ) {
            val items = listOf(2,1,0) // easy, medium, hard
            Row(
              modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              items.forEach { item ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .selectable(
                      selected = (soloGame.aiLevel == item),
                      onClick = {
                        when(item) {
                          2 -> viewModel.ai_easy()
                          1 -> viewModel.ai_medium()
                          0 -> viewModel.ai_hard()
                        }
                      },
                      role = Role.RadioButton
                    )
                    .padding(8.dp)
                ) {
                  IconToggleButton(
                    checked = (soloGame.aiLevel == item),
                    onCheckedChange = {
                      when(item) {
                        2 -> viewModel.ai_easy()
                        1 -> viewModel.ai_medium()
                        0 -> viewModel.ai_hard()
                      }
                    },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(
                      painter = painterResource(
                        if(soloGame.aiLevel == item) {
                          R.drawable.ic_baseline_check_circle_24
                        } else {
                          R.drawable.ic_baseline_circle_24
                        }
                      ),
                      contentDescription = null,
                      tint = MaterialTheme.colors.primary
                    )
                  }
                  Image(
                    painter = painterResource(
                      id = when(item) {
                        2 -> R.drawable.ai_easy
                        1 -> R.drawable.ai_medium
                        else -> R.drawable.ai_hard
                      }
                    ),
                    contentDescription = "",
                    modifier = Modifier.size(92.dp)
                  )
                }
              }
            }
          }
          Button(
            onClick = {
              when(soloGame.playAs) {
                0 -> newGame(navController, viewModel, p1IsAI = false, p2IsAI = true, aiLevel = soloGame.aiLevel) //play as Blue
                1 -> newGame(navController, viewModel, p1IsAI = true, p2IsAI = false, aiLevel = soloGame.aiLevel) //play as Red
                2 -> newGame( navController, viewModel, p1IsAI = false, p2IsAI = false, random = true, aiLevel = soloGame.aiLevel) //play Random
                else -> {}
              }
            },
            enabled = (soloGame.playAs >= 0 && soloGame.aiLevel >= 0),
            modifier = Modifier.padding(4.dp)
          ) {
            Row() {
              Text(
                text = "Play!",
                style = MaterialTheme.typography.h4
              )
            }

          }


//        Button(
//          onClick = { newGame(navController, viewModel, p1IsAI = false, p2IsAI = true) },
//          enabled = expanded,
//          modifier = Modifier.padding(4.dp)
//        ) {
//          Row(modifier = Modifier.fillMaxWidth()) {
//            Image(
//              painter = painterResource(id = R.drawable.blue_bo),
//              modifier = Modifier
//                .padding(4.dp)
//                .size(42.dp),
//              contentDescription = "Blue"
//            )
//            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
//            Text(
//              text = stringResource(id = R.string.blue),
//              style = MaterialTheme.typography.h4,
//              color = Color(0xFFa8c8ff)
//            )
//          }
//        }
//        Button(
//          onClick = { newGame(navController, viewModel, p1IsAI = true, p2IsAI = false) },
//          enabled = expanded,
//          modifier = Modifier.padding(4.dp)
//        ) {
//          Row(modifier = Modifier.fillMaxWidth()) {
//            Image(
//              painter = painterResource(id = R.drawable.red_bo),
//              modifier = Modifier
//                .padding(4.dp)
//                .size(42.dp),
//              contentDescription = "Red"
//            )
//            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
//            Text(
//              text = stringResource(id = R.string.red),
//              style = MaterialTheme.typography.h4,
//              color = Color.Red
//            )
//          }
//        }
//        Button(
//          onClick = { newGame( navController, viewModel, p1IsAI = false, p2IsAI = false, random = true ) },
//          enabled = expanded,
//          modifier = Modifier.padding(4.dp)
//        ) {
//          Row(modifier = Modifier.fillMaxWidth()) {
//            Image(
//              painter = painterResource(id = R.drawable.random),
//              modifier = Modifier
//                .padding(4.dp)
//                .size(42.dp),
//              contentDescription = "Random"
//            )
//            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
//            Text(
//              text = stringResource(id = R.string.random),
//              style = MaterialTheme.typography.h4,
//              color = Color.White
//            )
//          }
//        }
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
  var p1IsAI_copy = p1IsAI
  var p2IsAI_copy = p2IsAI
  if( random ) {
    if( Random.nextBoolean() ) {
      p1IsAI_copy = true
      p2IsAI_copy = false
    }
    else {
      p1IsAI_copy = false
      p2IsAI_copy = true
    }
  }
  gameViewModel.newGame(navController, p1IsAI_copy, p2IsAI_copy, xp, aiLevel)
  navController.navigate(Screen.Game.route)
}

private fun resume(
  navController: NavController,
//  gameViewModel: GameViewModel
) {
//  gameViewModel.resume()
  navController.navigate(Screen.Game.route)
}

@Preview(locale = "ja")
@Composable
private fun TitleViewPreview(hasStarted: Boolean = false) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.primaryVariant)
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = stringResource(id = R.string.app_name),
      style = MaterialTheme.typography.h2,
      color = MaterialTheme.colors.onPrimary
    )
    Spacer(modifier = Modifier.height(32.dp))
    GameButton(
      onClick = {},
      enabled = hasStarted,
      text = stringResource(id = R.string.resume)
    )
    Spacer(modifier = Modifier.height(16.dp))
    DropMenuButtonPreview(
      text = stringResource(id = R.string.ai_game)
    )
    Spacer(modifier = Modifier.height(16.dp))
    GameButtonWithIcon(
      onClick = { },
      text = stringResource(id = R.string.human_game),
      icon = R.drawable.two_players_icon
    )
    Spacer(
      modifier = Modifier.weight(1f)
    )
    Text(
      "vx.y.z",
      color = MaterialTheme.colors.onPrimary,
      modifier = Modifier.align(Alignment.CenterHorizontally)
    )
  }
}

@Preview
@Composable
private fun DropMenuButtonPreview(
  text: String = stringResource(id = R.string.ai_game)
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
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
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = stringResource(id = R.string.select_first_player),
          style = MaterialTheme.typography.h4,
          color = MaterialTheme.colors.onPrimary
        )
        Button(
          onClick = {  },
          enabled = expanded,
          modifier = Modifier.padding(4.dp)
        ) {
          Row(modifier = Modifier.fillMaxWidth()) {
            Image(
              painter = painterResource(id = R.drawable.blue_bo),
              modifier = Modifier
                .padding(4.dp)
                .size(42.dp),
              contentDescription = "Blue"
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(
              text = stringResource(id = R.string.blue),
              style = MaterialTheme.typography.h4,
              color = Color(0xFFa8c8ff) //Color.Blue
            )
          }
        }
        Button(
          onClick = {  },
          enabled = expanded,
          modifier = Modifier.padding(4.dp)
        ) {
          Row(modifier = Modifier.fillMaxWidth()) {
            Image(
              painter = painterResource(id = R.drawable.red_bo),
              modifier = Modifier
                .padding(4.dp)
                .size(42.dp),
              contentDescription = "Red"
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(
              text = stringResource(id = R.string.red),
              style = MaterialTheme.typography.h4,
              color = Color.Red
            )
          }
        }
        Button(
          onClick = {  },
          enabled = expanded,
          modifier = Modifier.padding(4.dp)
        ) {
          Row(modifier = Modifier.fillMaxWidth()) {
            Image(
              painter = painterResource(id = R.drawable.random),
              modifier = Modifier
                .padding(4.dp)
                .size(42.dp),
              contentDescription = "Random"
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(
              text = stringResource(id = R.string.random),
              style = MaterialTheme.typography.h4,
              color = Color.White
            )
          }
        }
      }
    }
  }
}