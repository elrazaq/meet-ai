package com.enterprise.meetai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enterprise.meetai.model.MeetingItem
import com.enterprise.meetai.ui.theme.CardBorder
import com.enterprise.meetai.ui.theme.CardDark
import com.enterprise.meetai.ui.theme.EmeraldSuccess
import com.enterprise.meetai.ui.theme.PinkSparkle
import com.enterprise.meetai.ui.theme.PurpleAccent
import com.enterprise.meetai.ui.theme.PurpleGlow
import com.enterprise.meetai.ui.theme.PurplePrimary
import com.enterprise.meetai.ui.theme.SurfaceDark
import com.enterprise.meetai.ui.theme.TextDim
import com.enterprise.meetai.ui.theme.TextMuted
import com.enterprise.meetai.ui.theme.TextWhite
import com.enterprise.meetai.utils.PdfExportUtils

data class DocTab(val index: Int, val title: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsViewerModal(
    meeting: MeetingItem,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
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
                        color = TextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${meeting.dateFormatted} • 4 Dokumen Terstruktur AI",
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = TextWhite)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Toolbar (Tombol Export PDF & Salin Teks)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tombol Export PDF Lengkap
                Button(
                    onClick = {
                        PdfExportUtils.exportAndSharePdf(context, meeting)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Tombol Bagikan Langsung
                OutlinedButton(
                    onClick = {
                        val currentText = when (selectedTab) {
                            0 -> meeting.documents.notulenContent ?: ""
                            1 -> meeting.documents.ringkasanContent ?: ""
                            2 -> meeting.documents.poinPentingContent ?: ""
                            3 -> meeting.documents.tanyaJawabContent ?: ""
                            else -> ""
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("MeetAI Doc", currentText))
                        Toast.makeText(context, "Teks dokumen disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(PurpleAccent, CardBorder)))
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = PurpleGlow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salin Teks", color = TextWhite, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                            selectedContainerColor = PurpleAccent,
                            selectedLabelColor = TextWhite,
                            containerColor = CardDark,
                            labelColor = TextMuted
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    .background(CardDark, shape = RoundedCornerShape(18.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormattedMarkdownViewer(rawMarkdown = content)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Renderer Markdown sederhana untuk menampilkan heading, bullet points, dan card tabel rapi
 */
@Composable
fun FormattedMarkdownViewer(rawMarkdown: String) {
    val lines = rawMarkdown.split("\n")

    for (rawLine in lines) {
        val line = rawLine.trim()
        if (line.isBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            continue
        }

        when {
            line.startsWith("### ") -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = line.removePrefix("### ").trim(),
                    color = PurpleGlow,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            line.startsWith("## ") -> {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = line.removePrefix("## ").trim(),
                    color = Color(0xFFA78BFA),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            line.startsWith("# ") -> {
                Text(
                    text = line.removePrefix("# ").trim(),
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, top = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier
                            .size(10.dp)
                            .padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = line.substring(2).trim(),
                        color = TextWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            line.startsWith("|") -> {
                // Baris Tabel Action Items / Format Khusus
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = line.replace("|", "  ").trim(),
                        color = if (line.contains("---")) TextDim else TextWhite,
                        fontSize = 11.sp,
                        fontWeight = if (line.contains("Tugas") || line.contains("PIC")) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            else -> {
                Text(
                    text = line,
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
