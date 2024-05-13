package fr.richoux.pobo.screens.gamescreen

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as CColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.richoux.pobo.R
import fr.richoux.pobo.engine.*
import fr.richoux.pobo.ui.LockScreenOrientation
import fr.richoux.pobo.engine.Color as EColor

private const val TAG = "pobotag GameView"

@Composable
fun GameActions(viewModel: GameViewModel = viewModel()) {
  val canGoBack by viewModel.canGoBack.collectAsState()
  val canGoForward by viewModel.canGoForward.collectAsState()
  IconButton(
    onClick = { viewModel.goBackMove() },
    enabled = canGoBack
  ) {
    Icon(Icons.Filled.ArrowBack, contentDescription = "Undo Move")
  }
  IconButton(
    onClick = { viewModel.goForwardMove() },
    enabled = canGoForward
  ) {
    Icon(Icons.Filled.ArrowForward, contentDescription = "Redo Move")
  }
}

@Composable
fun MainView(
  viewModel: GameViewModel,
  onTap: (Position) -> Unit = { _ -> },
  displayGameState: String = ""
) {
  val board = viewModel.getBoard()
  val promotionable = viewModel.getFlatPromotionable()
  val lastMove = viewModel.lastMovePosition
  val selected = viewModel.piecesToPromote.toList()
  val configuration = LocalConfiguration.current
//  var landscapeMode: Boolean
  when(configuration.orientation) {
    Configuration.ORIENTATION_PORTRAIT -> {
//      landscapeMode = false
      Column(
        Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        BoardView(
          board = board,
          lastMove = lastMove,
          onTap = onTap,
          promotionable = promotionable,
          selected = selected
        )
        Spacer(modifier = Modifier.height(8.dp))
        columnAllMode(viewModel, displayGameState)
      }
    }
//    Configuration.ORIENTATION_LANDSCAPE -> {
//      landscapeMode = true
//      Row(
//        modifier = Modifier.fillMaxWidth(),
//      ) {
//
//        Column(
//          Modifier
//            .fillMaxHeight()
//            .weight(1f),
//          horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//          columnAllMode(viewModel, displayGameState, landscapeMode)
//        }
//
//        BoardView(
//          modifier = Modifier.fillMaxHeight(),
//          board = board,
//          lastMove = lastMove,
//          onTap = onTap,
//          promotionable = promotionable,
//          selected = selected
//        )
//
//        Column(
//          Modifier
//            .fillMaxHeight()
//            .weight(1f),
//          horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//          columnAllMode(viewModel, displayGameState, landscapeMode)
//        }
//      }
//    }
  }
}

@Composable
fun columnAllMode(
  viewModel: GameViewModel = viewModel(),
  displayGameState: String,
  landscapeMode: Boolean = false
) {
  val board = viewModel.getBoard()
  val player = viewModel.getPlayer()
  val modifier = if(landscapeMode) Modifier else Modifier.fillMaxWidth()

  PiecesStocksView(
    pool = board.getPlayerPool(EColor.Blue),
    color = EColor.Blue,
    modifier = modifier
  )
  Spacer(modifier = Modifier.height(8.dp))
  PiecesStocksView(
    pool = board.getPlayerPool(EColor.Red),
    color = EColor.Red,
    modifier = modifier
  )
  Spacer(modifier = Modifier.height(32.dp))
  Row(
    modifier = Modifier.fillMaxWidth(0.5f),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = stringResource(id = R.string.turn),
      style = MaterialTheme.typography.body1,
    )
    Text(
      text = when(player) {
        EColor.Blue -> stringResource(id = R.string.blue)
        else -> stringResource(id = R.string.red)
      },
      style = TextStyle(
        color = when(player) {
          EColor.Blue -> CColor.Blue
          else -> CColor.Red
        },
        fontSize = MaterialTheme.typography.body1.fontSize,
        fontWeight = FontWeight.Bold,
        fontStyle = MaterialTheme.typography.body1.fontStyle
      ),
    )
  }
  Spacer(modifier = Modifier.height(16.dp))
  Text(
    text = displayGameState,
    style = MaterialTheme.typography.body1,
    modifier = Modifier.padding(horizontal = 2.dp)
  )
  Spacer(modifier = Modifier.height(16.dp))
  val hasChoiceOfPiece = viewModel.twoTypesInPool()
  if(hasChoiceOfPiece) {
    Row(
      horizontalArrangement = Arrangement.Center
    ) {
      RadioButtonPoBo(player, viewModel)
    }
  } else {
    val gameState by viewModel.gameState.collectAsState()
    val completeSelectionForRemoval =
      gameState == GameState.SELECTPROMOTIONS
      && (((viewModel.stateSelection == GameViewModelState.SELECT3 || viewModel.stateSelection == GameViewModelState.SELECT1OR3) && viewModel.piecesToPromote.size == 3)
      || ((viewModel.stateSelection == GameViewModelState.SELECT1 || viewModel.stateSelection == GameViewModelState.SELECT1OR3) && viewModel.piecesToPromote.size == 1))
    if(gameState == GameState.SELECTPROMOTIONS) {
      Button(
        onClick = { viewModel.validatePromotionsSelection() },
        enabled = completeSelectionForRemoval
      ) {
        Text(
          text = stringResource(id = R.string.promotion),
          style = MaterialTheme.typography.body1
        )
      }
    }
  }
}

@Composable
fun GameView(viewModel: GameViewModel = viewModel()) {
  LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
  val gameState by viewModel.gameState.collectAsState()
  val player = viewModel.getPlayer()

  when(gameState) {
    GameState.INIT -> {
//      Log.d(TAG, "INIT ${player}")
      MainView(
        viewModel
      )
      viewModel.goToNextState()
    }
    GameState.HISTORY -> {
      MainView(
        viewModel
      )
      viewModel.goToNextState()
    }
    GameState.SELECTPIECE -> {
//      Log.d(TAG, "SELECTPIECE ${player}")
      MainView(
        viewModel,
        displayGameState = stringResource(id = R.string.select_piece)
      )
    }
    GameState.SELECTPOSITION -> {
//      Log.d(TAG, "SELECTPOSITION ${player}")
      val onSelect: (Position) -> Unit = {
        if(viewModel.canPlayAt(it)) {
          viewModel.playAt(it)
        }
      }
      MainView(
        viewModel,
        onTap = onSelect
      )
      if(viewModel.IsAIToPLay()) {
        if(player == EColor.Blue)
          viewModel.makeP1AIMove()
        else
          viewModel.makeP2AIMove()
      }
    }
    GameState.CHECKPROMOTIONS -> {
//      Log.d(TAG, "CHECKPROMOTIONS ${player}")
      MainView(
        viewModel
      )
      viewModel.checkPromotions()
    }
    GameState.AUTOPROMOTIONS -> {
//      Log.d(TAG, "AUTOPROMOTIONS ${player}")
      MainView(
        viewModel
      )
      viewModel.autopromotions()
    }
    GameState.SELECTPROMOTIONS -> {
//      Log.d(TAG, "SELECTPROMOTIONS ${player}")
      val onSelect: (Position) -> Unit = {
        viewModel.selectForPromotionOrCancel(it)
      }
      MainView(
        viewModel,
        onTap = onSelect,
        displayGameState = stringResource(id = R.string.select_promotion)
      )
    }
    GameState.REFRESHSELECTPROMOTIONS -> {
//      Log.d(TAG, "REFRESHSELECTPROMOTIONS ${player}")
      MainView(
        viewModel
      )
      viewModel.goToNextState()
    }
    GameState.END -> {
      if(viewModel.xp)
        Log.d(TAG, "Winner ${viewModel.countNumberGames}: ${player}")
      else
      {
        Log.d(TAG, "Winner: ${player}")
        if(viewModel.p1IsAI)
        {
          if(viewModel.p2IsAI)
            Log.d(TAG, "Blue: ${viewModel.aiP1()}, Red: ${viewModel.aiP2()}")
          else
            Log.d(TAG, "Blue: ${viewModel.aiP1()}")
        }
        else
          if(viewModel.p2IsAI)
            Log.d(TAG, "Red: ${viewModel.aiP2()}")
      }
      MainView(
        viewModel
      )
      val style = TextStyle(
        color = if(player == EColor.Blue) CColor.Blue else CColor.Red,
        fontSize = MaterialTheme.typography.body1.fontSize,
        fontWeight = FontWeight.Bold,
        fontStyle = MaterialTheme.typography.body1.fontStyle
      )
      EndOfGameDialog(player, style, viewModel)
    }
  }
}

fun Modifier.customDialogModifier() = layout { measurable, constraints ->
  val placeable = measurable.measure(constraints);
  layout(constraints.maxWidth, constraints.maxHeight) {
    placeable.place(
      (constraints.maxWidth - placeable.width) / 2,
      9 * (constraints.maxHeight - placeable.height) / 10,
      10f
    )
  }
}

@Composable
fun EndOfGameDialog(
  player: EColor,
  style: TextStyle,
  viewModel: GameViewModel
) {
  val openAlertDialog = remember { mutableStateOf(true) }
  if(viewModel.xp && viewModel.countNumberGames < 100) {
    openAlertDialog.value = false
    viewModel.newGame(viewModel.p1IsAI, viewModel.p2IsAI, true)
  }
  else {
    when {
      openAlertDialog.value -> {
        AlertDialog(
          modifier = Modifier
            .customDialogModifier()
            .background(CColor.Transparent)
            .padding(8.dp),
          onDismissRequest = { openAlertDialog.value = false },
          title = {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = stringResource(
                  id = R.string.win, when(player) {
                    EColor.Red -> stringResource(id = R.string.red)
                    else -> stringResource(id = R.string.blue)
                  }
                ),
                style = style
              )
            }
          },
          text = {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = stringResource(id = R.string.newgame),
                style = MaterialTheme.typography.h6
              )
            }
          },
          buttons = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Button(
                {
                  openAlertDialog.value = false
                  viewModel.newGame(viewModel.p1IsAI, viewModel.p2IsAI, false)
                },
                modifier = Modifier.padding(bottom = 12.dp)
              ) {
                Text(text = stringResource(id = R.string.sure))
              }
              Button(
                { openAlertDialog.value = false },
                modifier = Modifier.padding(bottom = 12.dp)
              ) {
                Text(text = stringResource(id = R.string.next_time))
              }
            }
          }
        )
      }
    }
  }
}

@Composable
fun RadioButtonPoBo(player: EColor, viewModel: GameViewModel) {
  val iconPo = when(player) {
    EColor.Blue -> R.drawable.blue_po
    EColor.Red -> R.drawable.red_po
  }
  val iconBo = when(player) {
    EColor.Blue -> R.drawable.blue_bo
    EColor.Red -> R.drawable.red_bo
  }

  val selectedValue by viewModel.selectedValue.collectAsState()
  val items = listOf("Po", "Bo")
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
            selected = (selectedValue == item),
            onClick = {
              viewModel.cancelPieceSelection()
              when(item) {
                "Po" -> viewModel.selectPo()
                else -> viewModel.selectBo()
              }
            },
            role = Role.RadioButton
          )
          .padding(8.dp)
      ) {
        IconToggleButton(
          checked = selectedValue == item,
          onCheckedChange = {
            viewModel.cancelPieceSelection()
            when(item) {
              "Po" -> viewModel.selectPo()
              else -> viewModel.selectBo()
            }
          },
          modifier = Modifier.size(24.dp)
        ) {
          Icon(
            painter = painterResource(
              if(selectedValue == item) {
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
              "Po" -> iconPo
              else -> iconBo
            }
          ),
          contentDescription = "",
          modifier = Modifier.size(92.dp)
        )
      }
    }
  }
}

@Preview(locale = "fr")
@Composable
private fun EndOfGameDialogPreview(
  player: EColor = EColor.Red,
  style: TextStyle = TextStyle(
    color = CColor.Red,
    fontSize = MaterialTheme.typography.h5.fontSize,
    fontWeight = FontWeight.Bold,
    fontStyle = MaterialTheme.typography.body1.fontStyle
  ),
  xp: Boolean = false,
  countNumberGames: Int = 0
) {
  val openAlertDialog = remember { mutableStateOf(true) }
  if(xp && countNumberGames < 100) {
    openAlertDialog.value = false
  }
  else {
    when {
      openAlertDialog.value -> {
        AlertDialog(
          modifier = Modifier
            .customDialogModifier()
            .background(CColor.Transparent)
            .padding(8.dp),
          onDismissRequest = { openAlertDialog.value = false },
          title = {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ){
              Text(
                text = stringResource(
                  id = R.string.win, when(player) {
                    EColor.Red -> stringResource(id = R.string.red)
                    else -> stringResource(id = R.string.blue)
                  }
                ),
                style = style
              )
            }
          },
          text = {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = stringResource(id = R.string.newgame),
                style = MaterialTheme.typography.h6
              )
            }
          },
          buttons = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Button(
                {
                  openAlertDialog.value = false
                },
                modifier = Modifier.padding(bottom = 12.dp)
              ) {
                Text(text = stringResource(id = R.string.sure))
              }
              Button(
                { openAlertDialog.value = false },
                modifier = Modifier.padding( bottom = 12.dp)
              ) {
                Text(text = stringResource(id = R.string.next_time))
              }
            }
          }
        )
      }
    }
  }
}
