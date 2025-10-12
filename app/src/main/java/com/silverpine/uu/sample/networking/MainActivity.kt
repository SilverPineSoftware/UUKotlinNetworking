package com.silverpine.uu.sample.networking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.silverpine.uu.sample.networking.openai.OpenAiScreen
import com.silverpine.uu.sample.networking.openai.OpenAiSettingsScreen
import com.silverpine.uu.sample.networking.openai.SettingsRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

sealed class AppScreen(val label: String, val icon: ImageVector)
{
    object Home : AppScreen("Home", Icons.Default.Home)
    object Settings : AppScreen("Settings", Icons.Default.Settings)
    object OpenAi : AppScreen("Open AI", Icons.Default.Home)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen()
{
    val repository = remember { SettingsRepository() }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    //var showSettings by remember { mutableStateOf(false) }
    //val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

    val drawerItems = listOf(AppScreen.Home, AppScreen.Settings, AppScreen.OpenAi)

    // State that controls whether the OpenAI settings sheet should be visible
    var showOpenAiSettings by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentScreen.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        when (currentScreen)
                        {
                            is AppScreen.Home -> {
                                /*IconButton(onClick = { /* do something */ }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Home action")
                                }*/
                            }
                            is AppScreen.Settings -> {
                                /*IconButton(onClick = { /* do something */ }) {
                                    Icon(Icons.Default.Home, contentDescription = "Settings action")
                                }*/
                            }
                            is AppScreen.OpenAi -> {
                                IconButton(onClick = {
                                    showOpenAiSettings = true
                                })
                                {
                                    Icon(Icons.Default.Settings, contentDescription = "Open AI Settings")
                                }
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding))
            {
                when (currentScreen)
                {
                    is AppScreen.Home -> HomeScreen()
                    is AppScreen.Settings -> SettingsScreen()
                    is AppScreen.OpenAi -> OpenAiScreen(
                        repository = repository,
                        showSettings = showOpenAiSettings,
                        onSettingsDismissed = { showOpenAiSettings = false }
                    )
                }
            }

            /*if (showSettings)
            {
                ModalBottomSheet(
                    onDismissRequest = { showSettings = false },
                    sheetState = sheetState
                ) {
                    OpenAiSettingsScreen(
                        repository = repository,
                        onClose = {
                            scope.launch { sheetState.hide() }
                                .invokeOnCompletion { showSettings = false }
                        }
                    )
                }
            }*/
        }
    }
}

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("This is the Home Screen")
    }
}

@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("This is the Settings Screen")
    }
}