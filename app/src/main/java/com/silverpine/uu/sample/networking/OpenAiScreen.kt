package com.silverpine.uu.sample.networking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.silverpine.uu.core.UUResultBlock
import kotlinx.coroutines.launch

data class TableItem(
    val id: Int,
    val prompt: String,
    val answer: String)

@Composable
fun OpenAiScreen(
    loading: Boolean = false,
    prompt: String = "",
    results: List<TableItem> = listOf()
)
{
    val tableData = remember(results) {
        results.toMutableStateList()
    }

    var prompt by remember { mutableStateOf(prompt) }
    var loading by remember { mutableStateOf(loading) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            PromptInput(
                prompt = prompt,
                onPromptChange = { prompt = it },
                onSubmit = {
                    loading = true

                    askOpenAi(prompt)
                    { result ->

                        result.onFailure()
                        {
                            val answer = result.errorOrNull()?.toString() ?: "Unexpected error"
                            tableData.add(TableItem(tableData.size, prompt, answer))
                        }

                        result.onSuccess()
                        {
                            val answer = result.getOrNull() ?: "Null success"
                            tableData.add(TableItem(tableData.size, prompt, answer))
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
                TableItemView(item = item)
            }
        }
    }

    /*
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            verticalArrangement = Arrangement.Top
        ) {
            items(tableData) { item ->
                TableItemView(item = item)
                HorizontalDivider()
            }
        }

        // 👇 Here’s where you wire up onSubmit
        PromptInput(
            prompt = prompt,
            loading = loading,
            onPromptChange = { prompt = it },
            onSubmit = {
                loading = true

                askOpenAi(prompt)
                { result ->

                    result.onFailure()
                    {
                        val answer = result.errorOrNull()?.toString() ?: "Unexpected error"
                        tableData.add(TableItem(tableData.size, prompt, answer))
                    }

                    result.onSuccess()
                    {
                        val answer = result.getOrNull() ?: "Null success"
                        tableData.add(TableItem(tableData.size, prompt, answer))
                    }

                    loading = false
                    prompt = ""
                }
            }
        )
    }*/
}

fun askOpenAi(prompt: String, completion: UUResultBlock<String>)
{
    val api = OpenAiApi("Input your Open AI SDK Key here")
    api.session.logResponses = true
    api.askSomething(prompt, completion)
}

@Preview("Loading", apiLevel = 35, showBackground = true)
@Composable
fun PreviewScreen()
{
    OpenAiScreen(
        loading = true,
        prompt = "This is a question for Open AI",
        results = listOf()
    )
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


@Composable
fun TableItemView(item: TableItem, modifier: Modifier = Modifier)
{
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "ID: ${item.id}",
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.prompt,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.answer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}