package fr.richoux.pobo.screens

import android.content.pm.ActivityInfo
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import fr.richoux.pobo.R
import fr.richoux.pobo.ui.LockScreenOrientation

@Composable
fun  HowToPlayView() {
  LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
  var componentWidth by remember { mutableStateOf(0.dp) }
  val density = LocalDensity.current
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.primaryVariant)
      .padding(16.dp)
      .onGloballyPositioned {
        componentWidth = with(density) {
          it.size.width.toDp()
        }
      },
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(id = R.string.how_to_play_1),
          color = MaterialTheme.colors.onPrimary
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(id = R.string.how_to_play_2),
          modifier = Modifier.requiredWidth(componentWidth - 200.dp),
          color = MaterialTheme.colors.onPrimary
        )
        Image(
          painter = rememberDrawablePainter(
            drawable = getDrawable(
              LocalContext.current,
              R.drawable.pobo_push_01
            )
          ),
          modifier = Modifier.size(190.dp),
          contentDescription = "Push 1",
          contentScale = ContentScale.FillWidth
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {

        Text(
          text = stringResource(id = R.string.how_to_play_3),
          modifier = Modifier.requiredWidth(componentWidth - 200.dp),
          color = MaterialTheme.colors.onPrimary
        )
        Image(
          painter = rememberDrawablePainter(
            drawable = getDrawable(
              LocalContext.current,
              R.drawable.pobo_push_02
            )
          ),
          modifier = Modifier.size(190.dp),
          contentDescription = "Push 2",
          contentScale = ContentScale.FillWidth
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(id = R.string.how_to_play_4),
          color = MaterialTheme.colors.onPrimary
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(id = R.string.how_to_play_5),
          color = MaterialTheme.colors.onPrimary
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(id = R.string.how_to_play_6),
          color = MaterialTheme.colors.onPrimary
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(id = R.string.how_to_play_7),
          modifier = Modifier.requiredWidth(componentWidth - 200.dp),
          color = MaterialTheme.colors.onPrimary
        )
        Image(
          painter = painterResource(id = R.drawable.board_victory1),
          modifier = Modifier.size(190.dp).weight(1f),
          contentDescription = "Victory 1"
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(id = R.string.how_to_play_8),
          modifier = Modifier.requiredWidth(componentWidth - 200.dp),
          color = MaterialTheme.colors.onPrimary
        )
        Image(
          painter = painterResource(id = R.drawable.board_victory2),
          modifier = Modifier.size(190.dp),
          contentDescription = "Victory 2"
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(id = R.string.how_to_play_9),
          color = MaterialTheme.colors.onPrimary
        )
      }
    }
  }
}