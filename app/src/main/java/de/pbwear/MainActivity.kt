package de.pbwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import de.pbwear.ui.ChartScreen
import de.pbwear.ui.DepotScreen
import de.pbwear.ui.HomeScreen
import de.pbwear.ui.LoginScreen
import de.pbwear.ui.MainViewModel
import de.pbwear.ui.OrderScreen
import de.pbwear.ui.OrdersScreen
import de.pbwear.ui.PBTheme
import de.pbwear.ui.PositionScreen
import de.pbwear.ui.RankingScreen
import de.pbwear.ui.SearchScreen
import de.pbwear.ui.WatchlistScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PBTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {
    val vm: MainViewModel = viewModel()
    val nav = rememberSwipeDismissableNavController()
    // Startziel nur einmal festlegen: ändert es sich später, setzt Navigation den ganzen Verlauf zurück
    val start = remember { if (vm.loggedIn) "home" else "login" }

    // Wechsel zwischen angemeldet / abgemeldet (auch bei abgelaufener Sitzung)
    var previous by remember { mutableStateOf(vm.loggedIn) }
    LaunchedEffect(vm.loggedIn) {
        if (vm.loggedIn != previous) {
            previous = vm.loggedIn
            if (vm.loggedIn) {
                nav.navigate("home") { popUpTo("login") { inclusive = true } }
            } else {
                nav.navigate("login") { popUpTo("home") { inclusive = true } }
            }
        }
    }

    SwipeDismissableNavHost(navController = nav, startDestination = start) {
        composable("login") { LoginScreen(vm) }
        composable("home") {
            HomeScreen(
                vm = vm,
                onDepot = { nav.navigate("depot") },
                onWatchlist = { nav.navigate("watchlist") },
                onSearch = { nav.navigate("search") },
                onOrders = { nav.navigate("orders") },
                onRanking = { nav.navigate("ranking") },
            )
        }
        composable("depot") { DepotScreen(vm, onPosition = { nav.navigate("position") }) }
        composable("position") {
            PositionScreen(vm, onOrder = { nav.navigate("order") }, onChart = { nav.navigate("chart") })
        }
        composable("watchlist") { WatchlistScreen(vm, onOrder = { nav.navigate("order") }) }
        composable("search") { SearchScreen(vm, onOrder = { nav.navigate("order") }) }
        composable("orders") { OrdersScreen(vm) }
        composable("ranking") { RankingScreen(vm) }
        composable("chart") { ChartScreen(vm) }
        composable("order") {
            OrderScreen(
                vm,
                onClose = {
                    if (!nav.popBackStack("home", false)) {
                        nav.navigate("home") {
                            popUpTo(nav.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onChart = { nav.navigate("chart") },
            )
        }
    }
}
