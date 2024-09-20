package fr.richoux.pobo.screens.gamescreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.richoux.pobo.R
import fr.richoux.pobo.engine.Color
import fr.richoux.pobo.engine.PieceType
import kotlin.math.max

@Composable
fun BelowBoardView(
  viewModel: GameViewModel = viewModel()
) {
  val gameViewState by viewModel.poolViewState.collectAsState()
  val length_text = (stringResource(id = R.string.turn).length
  + max(stringResource(id = R.string.blue).length, stringResource(id = R.string.red).length))
  val messageID = gameViewState.messageID
  val displayGameState = if(messageID >= 0) stringResource(id = messageID) else ""

  PiecesStocksView(
    numberPo = gameViewState.numberBluePo,
    numberBo = gameViewState.numberBlueBo,
    color = Color.Blue
  )
  Spacer(modifier = Modifier.height(8.dp))
  PiecesStocksView(
    numberPo = gameViewState.numberRedPo,
    numberBo = gameViewState.numberRedBo,
    color = Color.Red
  )
  Spacer(modifier = Modifier.height(32.dp))
  Row(
    modifier = Modifier.fillMaxWidth(
      if( length_text <= 10 ) {
        0.4f
      }
      else if( length_text <= 28 ) {
        0.6f
      }
      else {
        0.9f
      }
    ),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = stringResource(id = R.string.turn),
      style = MaterialTheme.typography.body1,
    )
    Text(
      text = when(gameViewState.currentPlayer) {
        Color.Blue -> stringResource(id = R.string.blue)
        else -> stringResource(id = R.string.red)
      },
      style = TextStyle(
        color = when(gameViewState.currentPlayer) {
          Color.Blue -> androidx.compose.ui.graphics.Color.Blue
          else -> androidx.compose.ui.graphics.Color.Red
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

  if( viewModel.hasPromotables() && viewModel.promotionType != PromotionType.NONE ) {
    Button(
      onClick = { viewModel.validatePromotionsSelection() },
      enabled = gameViewState.canValidatePromotion
    ) {
      Text(
        text = stringResource(id = R.string.promotion),
        style = MaterialTheme.typography.body1
      )
    }
  } else {
    if( viewModel.twoTypesInPool() ) {
      Row(
        horizontalArrangement = Arrangement.Center
      ) {
        RadioButtonPoBo(gameViewState.currentPlayer, gameViewState.selectedPieceType, viewModel)
      }
    }
  }
}

@Composable
fun RadioButtonPoBo(
  player: Color,
  selectedValue: PieceType?,
  viewModel: GameViewModel
) {
  val iconPo = when(player) {
    Color.Blue -> R.drawable.blue_po
    Color.Red -> R.drawable.red_po
  }
  val iconBo = when(player) {
    Color.Blue -> R.drawable.blue_bo
    Color.Red -> R.drawable.red_bo
  }

  val items = listOf(PieceType.Po, PieceType.Bo)
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
              when(item) {
                PieceType.Po -> viewModel.selectPo()
                PieceType.Bo -> viewModel.selectBo()
              }
            },
            role = Role.RadioButton
          )
          .padding(8.dp)
      ) {
        IconToggleButton(
          checked = selectedValue == item,
          onCheckedChange = {
            when(item) {
              PieceType.Po -> viewModel.selectPo()
              PieceType.Bo -> viewModel.selectBo()
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
              PieceType.Po -> iconPo
              PieceType.Bo -> iconBo
            }
          ),
          contentDescription = "",
          modifier = Modifier.size(92.dp)
        )
      }
    }
  }
}
