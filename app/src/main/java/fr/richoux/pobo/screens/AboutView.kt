package fr.richoux.pobo.screens

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.richoux.pobo.R
import fr.richoux.pobo.ui.LockScreenOrientation

@Composable
fun  AboutView() {
  LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.primaryVariant)
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(id = R.string.about_1),
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
          text = stringResource(id = R.string.about_2),
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
          text = stringResource(id = R.string.about_3),
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
          text = stringResource(id = R.string.about_4),
          color = MaterialTheme.colors.onPrimary
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}