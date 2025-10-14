package com.silverpine.uu.sample.networking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.silverpine.uu.sample.networking.openai.OpenAiPrefsRepository
import com.silverpine.uu.sample.networking.openai.OpenAiScreen
import com.silverpine.uu.sample.networking.shutterstock.ShutterstockPrefsRepository
import com.silverpine.uu.sample.networking.shutterstock.ShutterstockScreen
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
    object OpenAi : AppScreen("Open AI", Icons.Default.ChatBubble)
    object ShutterStock : AppScreen("Shutterstock", Icons.Default.Image)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen()
{
    val openAiRepository = remember { OpenAiPrefsRepository() }
    val shutterstockRepository = remember { ShutterstockPrefsRepository() }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.OpenAi) }

    val drawerItems = listOf(AppScreen.OpenAi, AppScreen.ShutterStock)

    var showOpenAiSettings by remember { mutableStateOf(false) }
    var showShutterstockSettings by remember { mutableStateOf(false) }

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
                            is AppScreen.OpenAi -> {
                                IconButton(onClick = {
                                    showOpenAiSettings = true
                                })
                                {
                                    Icon(Icons.Default.Settings, contentDescription = "Open AI Settings")
                                }
                            }
                            is AppScreen.ShutterStock -> {
                                IconButton(onClick = {
                                    showShutterstockSettings = true
                                })
                                {
                                    Icon(Icons.Default.Settings, contentDescription = "Shutterstock Settings")
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
                    is AppScreen.OpenAi -> OpenAiScreen(
                        repository = openAiRepository,
                        showSettings = showOpenAiSettings,
                        onSettingsDismissed = { showOpenAiSettings = false }
                    )
                    is AppScreen.ShutterStock -> ShutterstockScreen(
                        repository = shutterstockRepository,
                        showSettings = showShutterstockSettings,
                        onSettingsDismissed = { showShutterstockSettings = false }
                    )
                }
            }
        }
    }
}
