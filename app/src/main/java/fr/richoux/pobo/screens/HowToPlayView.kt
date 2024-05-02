package fr.richoux.pobo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.richoux.pobo.R

@Composable
fun  HowToPlayView() {
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
  }
}