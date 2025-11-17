package com.silverpine.uu.sample.networking.openai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.uuFormatDate

@Composable
fun OpenAiTableItemRow(item: OpenAiTableItem, modifier: Modifier = Modifier)
{
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = item.timestamp.uuFormatDate(UUDate.Formats.ISO_8601_DATE_AND_TIME),
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

@Preview("Loading", apiLevel = 35, showBackground = true)
@Composable
fun OpenAiTableItemPreviewScreen()
{
    OpenAiTableItemRow(OpenAiTableItem(0,
        "What is the airspeed velocity of an unladen swallow?",
        "What do you mean? An African or European swallow?")
    )
}