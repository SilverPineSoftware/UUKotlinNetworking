package com.silverpine.uu.sample.networking.openai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.silverpine.uu.sample.networking.PreviewPrefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAiScreen(
    loading: Boolean = false,
    prompt: String = "",
    results: List<OpenAiTableItem> = listOf(),
    repository: OpenAiPrefsRepository = OpenAiPrefsRepository(),
    showSettings: Boolean,
    onSettingsDismissed: () -> Unit
)
{
    val tableData = remember(results) {
        results.toMutableStateList()
    }

    var prompt by remember { mutableStateOf(prompt) }
    var loading by remember { mutableStateOf(loading) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val api = remember()
    {
        OpenAiApi()
    }

    val repository = remember { repository }

    fun updateApiVars()
    {
        api.sdkKey = repository.loadApiKey()
        api.model = repository.loadModelChoice()
    }

    LaunchedEffect(Unit)
    {
        updateApiVars()
    }

    Scaffold(
        bottomBar = {
            PromptInput(
                prompt = prompt,
                onPromptChange = { prompt = it },
                onSubmit = {
                    loading = true

                    api.askSomething(prompt)
                    { result ->

                        result.onFailure()
                        {
                            val answer = result.errorOrNull()?.toString() ?: "Unexpected error"
                            tableData.add(OpenAiTableItem(tableData.size, prompt, answer))
                        }

                        result.onSuccess()
                        {
                            val answer = result.getOrNull() ?: "Null success"
                            tableData.add(OpenAiTableItem(tableData.size, prompt, answer))
                        }

                        loading = false
                        prompt = ""

                        coroutineScope.launch {
                            listState.animateScrollToItem(tableData.lastIndex)
                        }
                    }
                },
                loading = loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(8.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top
        ) {
            items(tableData, key = { it.id }) { item ->
                OpenAiTableItemRow(item = item)
            }
        }

        if (showSettings)
        {
            DisposableEffect(Unit) {
                onDispose {
                    updateApiVars()
                }
            }
            ModalBottomSheet(
                onDismissRequest = { onSettingsDismissed() },
                sheetState = sheetState
            ) {
                OpenAiSettingsScreen(
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
fun PromptInput(
    prompt: String,
    loading: Boolean,
    onPromptChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = prompt,
        onValueChange = onPromptChange,
        label = { Text("Enter a prompt") },
        modifier = modifier.fillMaxWidth(),
        enabled = !loading,
        trailingIcon =
        {
            if (loading)
            {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
            else
            {
                IconButton(
                    onClick = onSubmit,
                    enabled = prompt.isNotBlank()
                )
                {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Submit"
                    )
                }
            }
        }
    )
}


@Preview("Loading", apiLevel = 35, showBackground = true)
@Composable
fun PreviewScreen()
{
    OpenAiScreen(
        loading = true,
        prompt = "This is a question for Open AI",
        results = listOf(),
        repository = OpenAiPrefsRepository(PreviewPrefs()),
        false,
        { }
    )
}
