package fr.richoux.pobo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import fr.richoux.pobo.gamescreen.GameActions
import fr.richoux.pobo.gamescreen.GameView
import fr.richoux.pobo.gamescreen.GameViewModel
import fr.richoux.pobo.titlescreen.TitleView
import fr.richoux.pobo.ui.PoboTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PoboTheme {
                val navController = rememberAnimatedNavController()
                val gameViewModel: GameViewModel = viewModel()

                Scaffold(
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
                                if (canPop) {
                                    IconButton(onClick = {
                                        navController.popBackStack()
                                    }) {
                                        Icon(Icons.Outlined.Home, contentDescription = "Home")
                                    }
                                }
                            },
                            backgroundColor = MaterialTheme.colors.primary,
                            actions = actions
                        )
                    }
                ) {
                    AnimatedNavHost(
                        navController = navController,
                        startDestination = Screen.Title.route,
                    ) {
                        composable(Screen.Title.route) { TitleView(navController, gameViewModel) }
                        composable(
                            Screen.Game.route,
                            enterTransition = { _, _ ->
                                slideInHorizontally(
                                    initialOffsetX = { 1000 },
                                    animationSpec = tween(
                                        transitionTime
                                    )
                                )
                            },
                            exitTransition = { _, _ -> slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(transitionTime)) },
                            popExitTransition = { _, _ -> slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(transitionTime)) },
                        ) {
                            GameView(gameViewModel)
                        }
                    }
                }
            }
        }
    }
}

private val transitionTime = 333

sealed class Screen(
    val route: String,
    val title: String,
    val actions: @Composable RowScope.() -> Unit
) {
    object Title : Screen("title", "", actions = {})
    object Game : Screen("game", "", actions = { GameActions() })

    companion object {
        val allList by lazy { listOf(Title, Game) }
        val allMap by lazy { allList.associateBy { it.route } }
    }
}
