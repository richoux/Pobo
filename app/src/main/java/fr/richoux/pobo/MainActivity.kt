package fr.richoux.pobo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.richoux.pobo.screens.AboutView
import fr.richoux.pobo.screens.HowToPlayView
import fr.richoux.pobo.screens.TitleView
import fr.richoux.pobo.screens.gamescreen.GameActions
import fr.richoux.pobo.screens.gamescreen.GameView
import fr.richoux.pobo.screens.gamescreen.GameViewModel
import fr.richoux.pobo.screens.gamescreen.customDialogModifier
import fr.richoux.pobo.ui.PoboTheme
import kotlinx.coroutines.launch
import java.util.Locale


private val showLanguages = mutableStateOf(false)
// Arabic not ready
private val locale = mutableStateOf<LocaleListCompat>(
  when(LocaleListCompat.getDefault()) {
    LocaleListCompat.forLanguageTags("ar") -> LocaleListCompat.forLanguageTags("en")
    else -> LocaleListCompat.getDefault()
  }
)

// from https://stackoverflow.com/questions/68611320/remember-lazycolumn-scroll-position-jetpack-compose
private val SaveMap = mutableMapOf<String, KeyParams>()

private data class KeyParams(
  val params: String = "",
  val index: Int,
  val scrollOffset: Int
)
@Composable
fun rememberForeverLazyListState(
  key: String,
  params: String = "",
  initialFirstVisibleItemIndex: Int = 0,
  initialFirstVisibleItemScrollOffset: Int = 0
): LazyListState {
  val scrollState = rememberSaveable(saver = LazyListState.Saver) {
    var savedValue = SaveMap[key]
    if (savedValue?.params != params) savedValue = null
    val savedIndex = savedValue?.index ?: initialFirstVisibleItemIndex
    val savedOffset = savedValue?.scrollOffset ?: initialFirstVisibleItemScrollOffset
    LazyListState(
      savedIndex,
      savedOffset
    )
  }
  DisposableEffect(Unit) {
    onDispose {
      val lastIndex = scrollState.firstVisibleItemIndex
      val lastOffset = scrollState.firstVisibleItemScrollOffset
      SaveMap[key] = KeyParams(params, lastIndex, lastOffset)
    }
  }
  return scrollState
}

@Composable
private fun RowMenu( text: String, content: String, icon: ImageVector, onClick: () -> Unit ) {
  Row(
    modifier= Modifier
      .fillMaxWidth()
      //.height(64.dp)
      .padding(vertical = 8.dp)
      .clickable(
        enabled = true,
        onClickLabel = null,
        role = null,
        onClick = onClick
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      icon,
      modifier = Modifier
        .size(36.dp)
        .padding(start = 8.dp),
      contentDescription = content)
    Text(
      text = text,
      style = MaterialTheme.typography.h5,
      modifier = Modifier.padding(start = 30.dp)
    )
  }
}

@Composable
private fun ItemLanguage( language: String, code: String ) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(4.dp),
    horizontalArrangement = Arrangement.Center
  ) {
    Button(
      modifier = Modifier
        .width(200.dp)
        .background(MaterialTheme.colors.primary, RoundedCornerShape(12)),
      onClick = {
        locale.value = LocaleListCompat.forLanguageTags(code)
        AppCompatDelegate.setApplicationLocales(locale.value)
        showLanguages.value = false
      }
    ) {
      Text(
        text = language,
        color = MaterialTheme.colors.onPrimary,
        style = MaterialTheme.typography.h5
      )
    }
    Spacer(modifier = Modifier.height(48.dp))
  }
}

@Composable
private fun RowMenuPopup( text: String, content: String, icon: Int, onClick: () -> Unit ) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      //.height(64.dp)
      .padding(vertical = 8.dp)
      .clickable(
        enabled = true,
        onClickLabel = null,
        role = null,
        onClick = onClick
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      //icon,
      painter =
      painterResource(id = icon),
      modifier = Modifier
        .size(36.dp)
        .padding(start = 8.dp),
      contentDescription = content
    )
    Text(
      text = text,
      style = MaterialTheme.typography.h5,
      modifier = Modifier.padding(start = 30.dp)
    )
    if(showLanguages.value) {
      AlertDialog(
        onDismissRequest = { showLanguages.value = false },
        buttons = {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colors.primaryVariant)
              .weight(9f)
          ) {
            item {
              ItemLanguage("English", "en")
            }
//            item {
//              ItemLanguage("اللغة العربية", "ar")
//            }
            item {
              ItemLanguage("Český", "cs")
            }
            item {
              ItemLanguage("Deutsche", "de")
            }
            item {
              ItemLanguage("Ελληνική", "el")
            }
            item {
              ItemLanguage("Español", "es")
            }
            item {
              ItemLanguage("Français", "fr")
            }
            item {
              ItemLanguage("Italiana", "it")
            }
            item {
              ItemLanguage("日本語", "ja")
            }
            item {
              ItemLanguage("Polski", "pl")
            }
            item {
              ItemLanguage("Portugues", "pt")
            }
            item {
              ItemLanguage("Slovenský", "sk")
            }
            item {
              ItemLanguage("Tiếng Việt", "vi")
            }
            item {
              ItemLanguage("简体中文", "zh")
            }
            item {
              ItemLanguage("繁體中文", "zh-Hant-TW")
            }
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(4.dp)
              .weight(1f)
          ) {
              Text(
                text = stringResource(id = R.string.cancel),
                color = MaterialTheme.colors.onSecondary,
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                  .fillMaxSize()
                  .wrapContentHeight()
                  .clickable(
                  onClick = {
                    showLanguages.value = false
                  }
                ),
                textAlign = TextAlign.Center
              )
//            }
          }
        }
      )
    }
  }
}

@Composable
private fun RowMenuPaint( text: String, content: String, icon: Int, onClick: () -> Unit ) {
  Row(
    modifier= Modifier
      .fillMaxWidth()
      //.height(64.dp)
      .padding(vertical = 8.dp)
      .clickable(
        enabled = true,
        onClickLabel = null,
        role = null,
        onClick = onClick
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      painter =
      painterResource(id = icon),
      modifier = Modifier
        .size(36.dp)
        .padding(start = 8.dp),
      contentDescription = content
    )
    Text(
      text = text,
      style = MaterialTheme.typography.h5,
      modifier = Modifier.padding(start = 30.dp)
    )
  }
}

@Composable
private fun RowMenuPaintNoTint( text: String, content: String, icon: Int, onClick: () -> Unit ) {
  Row(
    modifier= Modifier
      .fillMaxWidth()
      //.height(64.dp)
      .padding(vertical = 8.dp)
      .clickable(
        enabled = true,
        onClickLabel = null,
        role = null,
        onClick = onClick
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      painter =
      painterResource(id = icon),
      modifier = Modifier
        .size(36.dp)
        .padding(start = 8.dp),
      contentDescription = content,
      tint= Color.Unspecified
    )
    Text(
      text = text,
      style = MaterialTheme.typography.h5,
      modifier = Modifier.padding(start = 30.dp)
    )
  }
}

class MainActivity : AppCompatActivity() {
  @OptIn(ExperimentalAnimationApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      PoboTheme {
        val navController = rememberNavController()
        val gameViewModel: GameViewModel = viewModel()
        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()

        Scaffold(
          scaffoldState = scaffoldState,
          topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val canPop = navController.previousBackStackEntry != null
            val screen = currentDestination?.route?.let { route ->
              Screen.allMap[route]
            }

            val titleText = screen?.title ?: ""
            val actions = screen?.actions ?: {}

            TopAppBar(
              title = { Text(titleText) },
              navigationIcon = {
                if(canPop) {
                  IconButton(onClick = {
                    navController.popBackStack()
                  }) {
                    Icon(Icons.Outlined.Home, contentDescription = "Home")
                  }
                }
                else {
                  IconButton(onClick = {
                    scope.launch {
                      scaffoldState.drawerState.open()
                    }
                  }) {
                    Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                  }
                }
              },
              backgroundColor = MaterialTheme.colors.primary,
              actions = actions
            )
          },
          drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
          drawerBackgroundColor = MaterialTheme.colors.primary,
          drawerContent = {
            Column() {
              RowMenuPaint(
                text = stringResource(R.string.how_to_play),
                content = "HowTo",
                icon = R.drawable.question_mark,
                onClick = {
                  scope.launch {
                    scaffoldState.drawerState.close()
                  }
                  navController.navigate(Screen.HowToPlay.route)
                }
              )
              RowMenu(
                text = stringResource(R.string.about),
                content = "About",
                icon = Icons.Outlined.Info,
                onClick = {
                  scope.launch {
                    scaffoldState.drawerState.close()
                  }
                  navController.navigate(Screen.About.route)
                }
              )
              RowMenuPopup(
                text = stringResource(R.string.language),
                content = "Language",
                icon = R.drawable.language_icon,
//                icon = Icons.Outlined.Settings,
                onClick = {
                  showLanguages.value = true
                }
              )
              RowMenuPaintNoTint(
                text = stringResource(R.string.buy_me_a_coffee),
                content = "Coffee",
                icon = R.drawable.buy_me_a_coffee,
                onClick = {
                  val url = "https://buymeacoffee.com/richoux/"
                  val i = Intent(Intent.ACTION_VIEW)
                  i.setData(Uri.parse(url))
                  startActivity(i)
                }
              )
              RowMenuPaintNoTint(
                text = stringResource(R.string.rate_it),
                content = "GooglePlay",
                icon = R.drawable.google_play,
                onClick = {
                  val url = "https://play.google.com/store/apps/details?id=fr.richoux.pobo"
                  val i = Intent(Intent.ACTION_VIEW)
                  i.setData(Uri.parse(url))
                  startActivity(i)
                }
              )
              RowMenuPaint(
                text = stringResource(R.string.github),
                content = "Github",
                icon = R.drawable.github,
                onClick = {
                  val url = "https://github.com/richoux/Pobo/"
                  val i = Intent(Intent.ACTION_VIEW)
                  i.setData(Uri.parse(url))
                  startActivity(i)
                }
              )
            }
          }
        ) { innerPadding ->
          NavHost(
            navController = navController,
            startDestination = Screen.Title.route,
            modifier = Modifier.padding(innerPadding)
          ) {
            composable(Screen.Title.route) { TitleView(navController, gameViewModel) }
            composable(
              Screen.Game.route,
              enterTransition = { ->
                slideInHorizontally(
                  initialOffsetX = { 1000 },
                  animationSpec = tween(
                    transitionTime
                  )
                )
              },
              exitTransition = { ->
                slideOutHorizontally(
                  targetOffsetX = { -1000 },
                  animationSpec = tween(transitionTime)
                )
              },
              popExitTransition = { ->
                slideOutHorizontally(
                  targetOffsetX = { 1100 },
                  animationSpec = tween(transitionTime)
                )
              },
            ) {
              GameView(gameViewModel)
            }
            composable(Screen.HowToPlay.route) { HowToPlayView() }
            composable(Screen.About.route) { AboutView() }
          }
        }
      }
    }
  }
}

private const val transitionTime = 333

sealed class Screen(
  val route: String,
  val title: String,
  val actions: @Composable RowScope.() -> Unit
) {
  object Title : Screen("title", "", actions = {})
  object Game : Screen("game", "", actions = { GameActions() })
  object HowToPlay : Screen("how_to_play", "", actions = {})
  object About : Screen("about", "", actions = {})

  companion object {
    private val allList by lazy { listOf(Title, Game) }
    val allMap by lazy { allList.associateBy { it.route } }
  }
}
