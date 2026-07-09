package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StyledMarkdownCard(markdownText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EtherealCard),
        border = BorderStroke(1.dp, EtherealCardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Very basic but visually superb markdown parsing
            val lines = markdownText.lines()
            var currentSectionTitle = ""
            var currentSectionBody = StringBuilder()

            for (line in lines) {
                if (line.startsWith("###")) {
                    // Flush previous section
                    if (currentSectionTitle.isNotBlank()) {
                        InterpretationSection(title = currentSectionTitle, body = currentSectionBody.toString())
                        Spacer(modifier = Modifier.height(16.dp))
                        currentSectionBody = StringBuilder()
                    }
                    currentSectionTitle = line.removePrefix("###").trim()
                } else if (line.startsWith("##") || line.startsWith("#")) {
                    // Flush
                    if (currentSectionTitle.isNotBlank()) {
                        InterpretationSection(title = currentSectionTitle, body = currentSectionBody.toString())
                        Spacer(modifier = Modifier.height(16.dp))
                        currentSectionBody = StringBuilder()
                    }
                    currentSectionTitle = line.removePrefix("##").removePrefix("#").trim()
                } else {
                    currentSectionBody.append(line).append("\n")
                }
            }

            // Flush final section
            if (currentSectionTitle.isNotBlank()) {
                InterpretationSection(title = currentSectionTitle, body = currentSectionBody.toString())
            } else if (markdownText.isNotBlank()) {
                // If parsing fails or doesn't use standard headers, output raw
                Text(
                    text = markdownText,
                    fontSize = 14.sp,
                    color = NebulaLavender,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

