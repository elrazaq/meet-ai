package com.enterprise.meetai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enterprise.meetai.model.MeetingItem
import com.enterprise.meetai.ui.theme.LocalMeetAiColors
import com.enterprise.meetai.utils.PdfExportUtils
import java.util.regex.Pattern

data class DocTab(val index: Int, val title: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsViewerModal(
    meeting: MeetingItem,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalMeetAiColors.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        DocTab(0, "Notulen Formal", Icons.Default.Description),
        DocTab(1, "Ringkasan", Icons.Default.Summarize),
        DocTab(2, "Poin Kunci", Icons.Default.Lightbulb),
        DocTab(3, "Anti-Plot-Hole", Icons.Default.HelpOutline)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meeting.title,
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${meeting.dateFormatted} • 4 Dokumen Terstruktur AI",
                        color = colors.textDim,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(colors.cardBorder.copy(alpha = 0.5f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Ekspor PDF & Salin Teks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        PdfExportUtils.exportAndShareCorporatePdf(context, meeting)
                    },
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor PDF Resmi", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        val allDocsCombined = buildString {
                            appendLine("=== ${meeting.title.uppercase()} ===")
                            appendLine("Tanggal: ${meeting.dateFormatted} | Durasi: ${meeting.durationFormatted}")
                            appendLine("\n1. NOTULEN RAPAT:")
                            appendLine(meeting.documents.notulenContent ?: "-")
                            appendLine("\n2. RINGKASAN EKSEKUTIF:")
                            appendLine(meeting.documents.ringkasanContent ?: "-")
                            appendLine("\n3. POIN-POIN PENTING & DATA KUNCI:")
                            appendLine(meeting.documents.poinPentingContent ?: "-")
                            appendLine("\n4. REKAP TANYA JAWAB & ANTI-PLOT-HOLE:")
                            appendLine(meeting.documents.tanyaJawabContent ?: "-")
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("MeetAI Meeting Docs", allDocsCombined)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Seluruh 4 dokumen berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salin Teks", color = colors.textPrimary, fontSize = 12.sp, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab bar horizontal untuk 4 Dokumen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab.index,
                        onClick = { selectedTab = tab.index },
                        label = {
                            Text(
                                tab.title,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = Color.White,
                            containerColor = colors.card,
                            labelColor = colors.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Dokumen Terformat Rapi
            val content = when (selectedTab) {
                0 -> meeting.documents.notulenContent ?: "Notulen belum tersedia untuk rapat ini."
                1 -> meeting.documents.ringkasanContent ?: "Ringkasan belum tersedia untuk rapat ini."
                2 -> meeting.documents.poinPentingContent ?: "Poin penting belum tersedia untuk rapat ini."
                3 -> meeting.documents.tanyaJawabContent ?: "Rekap tanya-jawab & anti-plot-hole belum tersedia."
                else -> ""
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colors.card, shape = RoundedCornerShape(18.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormattedMarkdownViewer(rawMarkdown = content)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * Parsing teks Markdown untuk mengubah simbol **bold** menjadi teks tebal sungguhan
 * dan membersihkan simbol bintang agar rapi seperti di editor profesional.
 */
fun parseMarkdownText(
    text: String,
    defaultColor: Color,
    boldColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        val pattern = Pattern.compile("(\\*{2}(.+?)\\*{2})|(\\*(.+?)\\*)|(`(.+?)`)")
        val matcher = pattern.matcher(text)
        var lastIdx = 0

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > lastIdx) {
                append(text.substring(lastIdx, start))
            }

            when {
                matcher.group(1) != null -> {
                    // Bold: **content**
                    val content = matcher.group(2) ?: ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                        append(content)
                    }
                }
                matcher.group(3) != null -> {
                    // Italic: *content*
                    val content = matcher.group(4) ?: ""
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor)) {
                        append(content)
                    }
                }
                matcher.group(5) != null -> {
                    // Inline code: `content`
                    val content = matcher.group(6) ?: ""
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22888888))) {
                        append(" $content ")
                    }
                }
            }
            lastIdx = end
        }

        if (lastIdx < text.length) {
            append(text.substring(lastIdx))
        }
    }
}

/**
 * Renderer Markdown canggih:
 * 1. Mengubah **bold** menjadi format tebal tanpa simbol bintang.
 * 2. Merender tabel Markdown (| ... |) menjadi tabel HTML-style responsif dengan garis border & scroll horizontal.
 */
@Composable
fun FormattedMarkdownViewer(rawMarkdown: String) {
    val colors = LocalMeetAiColors.current
    val lines = rawMarkdown.split("\n")

    var i = 0
    while (i < lines.size) {
        val rawLine = lines[i]
        val line = rawLine.trim()

        if (line.isBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            i++
            continue
        }

        // Cek jika baris ini merupakan awal dari Tabel Markdown (| Kolom 1 | Kolom 2 |)
        if (line.startsWith("|") && line.endsWith("|")) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                tableLines.add(lines[i].trim())
                i++
            }
            RenderMarkdownTable(tableLines = tableLines)
            continue
        }

        when {
            line.startsWith("### ") -> {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = line.removePrefix("### ").trim(),
                    color = colors.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            line.startsWith("## ") -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = line.removePrefix("## ").trim(),
                    color = colors.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            line.startsWith("# ") -> {
                Text(
                    text = line.removePrefix("# ").trim(),
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                val bulletContent = line.substring(2).trim()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier
                            .size(8.dp)
                            .padding(top = 5.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = parseMarkdownText(bulletContent, colors.textPrimary, colors.primary),
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
            else -> {
                // Paragraf biasa dengan parsing bold **
                Text(
                    text = parseMarkdownText(line, colors.textSecondary, colors.textPrimary),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
        i++
    }
}

/**
 * Komponen Tabel Nyata Bergaris (HTML-style Data Table) dengan dukungan scroll horizontal
 */
@Composable
fun RenderMarkdownTable(tableLines: List<String>) {
    val colors = LocalMeetAiColors.current

    // Ekstrak baris tabel
    val rows = tableLines.filterNot { it.contains("---") }.map { rowLine ->
        rowLine.trim().removeSurrounding("|").split("|").map { it.trim() }
    }

    if (rows.isEmpty()) return

    val headers = rows.first()
    val bodyRows = if (rows.size > 1) rows.subList(1, rows.size) else emptyList()

    Spacer(modifier = Modifier.height(6.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
            .background(colors.surface, shape = RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            // Header Tabel
            Row(
                modifier = Modifier
                    .background(colors.primary.copy(alpha = 0.12f))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headers.forEachIndexed { index, headerText ->
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = headerText,
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                    }
                    if (index < headers.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(colors.cardBorder)
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.cardBorder, thickness = 1.dp)

            // Baris Data Tabel dengan efek zebra striping
            bodyRows.forEachIndexed { rowIndex, rowData ->
                val rowBg = if (rowIndex % 2 == 1) colors.card.copy(alpha = 0.6f) else Color.Transparent
                Row(
                    modifier = Modifier
                        .background(rowBg)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headers.indices.forEach { colIndex ->
                        val cellText = rowData.getOrNull(colIndex) ?: ""
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = parseMarkdownText(cellText, colors.textSecondary, colors.textPrimary),
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                        if (colIndex < headers.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(colors.cardBorder.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
                if (rowIndex < bodyRows.size - 1) {
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}
