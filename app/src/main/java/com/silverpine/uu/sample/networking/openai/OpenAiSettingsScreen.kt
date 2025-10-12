package com.silverpine.uu.sample.networking.openai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAiSettingsScreen(
    repository: SettingsRepository = SettingsRepository(),
    onClose: () -> Unit)
{
    val repository = remember { repository }
    var apiKey by remember { mutableStateOf(repository.loadApiKey()) }
    var selectedModel by remember { mutableStateOf(repository.loadModelChoice()) }
    var expanded by remember { mutableStateOf(false) }
    val modelOptions = listOf("gpt-5", "gpt-5-mini", "gpt-5-nano", "gpt-4", "gpt-4o-mini", "gpt-3.5-turbo", "o3", "o3-mini", "o4-mini")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OpenAI Settings", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                repository.saveApiKey(it)
            },
            label = { Text("OpenAI SDK Key") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            singleLine = false,
            maxLines = 5
        )

        Spacer(Modifier.height(24.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                modelOptions.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            selectedModel = model
                            repository.saveModelChoice(model)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}