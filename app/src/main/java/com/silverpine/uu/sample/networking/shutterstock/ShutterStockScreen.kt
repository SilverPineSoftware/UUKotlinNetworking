package com.silverpine.uu.sample.networking.shutterstock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.silverpine.uu.sample.networking.PreviewPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

fun createAppDownloadsSubfolder(context: Context, folderName: String): File?
{
    val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    val subfolder = File(baseDir, folderName)
    if (!subfolder.exists())
    {
        subfolder.mkdirs() // Creates the folder and any missing parent folders
    }

    return subfolder.takeIf { it.exists() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShutterstockScreen(
    repository: ShutterstockPrefsRepository = ShutterstockPrefsRepository(),
    showSettings: Boolean,
    onSettingsDismissed: () -> Unit
) {
    var searchTerm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    val images = remember { mutableStateListOf<String>() }

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val api = remember { ShutterstockApi() }
    val context = LocalContext.current
    val downloadFolder = remember { createAppDownloadsSubfolder(context, "Shutterstock") }
    val repository = remember { repository }

    fun updateApiVars()
    {
        api.username = repository.userName
        api.password = repository.password
    }

    fun search(reset: Boolean = false) {
        if (loading) return
        loading = true

        val currentPage = if (reset) 1 else page
        api.searchImages(searchTerm, page = currentPage, perPage = 30) { result ->
            result.onSuccess { response ->
                if (reset) {
                    images.clear()
                }
                images.addAll(
                    response.data.mapNotNull { it.assets?.preview?.url }
                )
                page = currentPage + 1
            }
            result.onFailure {
                // TODO: show error UI
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        updateApiVars()
    }

    // Trigger pagination when scrolled near bottom
    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.layoutInfo.totalItemsCount) {
        val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (!loading && lastVisible >= images.size - 6 && searchTerm.isNotBlank()) {
            search(reset = false)
        }
    }

    Scaffold(
        topBar = {
            SearchInput(
                searchTerm = searchTerm,
                loading = loading,
                onSearchTermChange = { searchTerm = it },
                onSubmit = {
                    page = 1
                    search(reset = true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(images.size)
            { index ->
                val url = images[index]
                RemoteImage(url, downloadFolder!!) // hack!!
            }
        }

        if (showSettings) {
            DisposableEffect(Unit) {
                onDispose { updateApiVars() }
            }
            ModalBottomSheet(
                onDismissRequest = { onSettingsDismissed() },
                sheetState = sheetState
            ) {
                ShutterstockSettingsScreen(
                    repository = repository,
                    onClose = {
                        coroutineScope.launch { sheetState.hide() }
                            .invokeOnCompletion { onSettingsDismissed() }
                    }
                )
            }
        }
    }
}

@Composable
fun SearchInput(
    searchTerm: String,
    loading: Boolean,
    onSearchTermChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchTerm,
        onValueChange = onSearchTermChange,
        label = { Text("Search Shutterstock") },
        modifier = modifier,
        enabled = !loading,
        trailingIcon = {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = onSubmit,
                    enabled = searchTerm.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                }
            }
        }
    )
}

@Composable
fun RemoteImage(
    url: String,
    outputDir: File,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Trigger download once when this composable is first created
    LaunchedEffect(url) {
        bitmap = downloadBitmap(url, outputDir)
        loading = false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Downloaded image",
                    modifier = Modifier.fillMaxSize()
                )
            }
            loading -> {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
        }
    }
}


private suspend fun downloadBitmap(url: String, outputDir: File): Bitmap?
{
    return withContext(Dispatchers.IO) {
        try {
            val fileName = url.substringAfterLast('/')
            val outFile = File(outputDir, fileName)

            if (!outFile.exists()) {
                URL(url).openStream().use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            BitmapFactory.decodeFile(outFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewShutterstockScreen() {
    ShutterstockScreen(
        repository = ShutterstockPrefsRepository(PreviewPrefs()),
        showSettings = false,
        onSettingsDismissed = {}
    )
}