package com.enterprise.meetai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enterprise.meetai.model.MeetingItem
import com.enterprise.meetai.model.ProcessingStatus
import com.enterprise.meetai.ui.components.DocumentsViewerModal
import com.enterprise.meetai.ui.components.WaveformVisualizer
import com.enterprise.meetai.ui.theme.BgDark
import com.enterprise.meetai.ui.theme.BlueInfo
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MeetingViewModel,
    onRequestPermissionAndRecord: () -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val currentAmplitude by viewModel.currentAmplitude.collectAsState()
    val meetings by viewModel.meetings.collectAsState()
    val selectedMeetingForDocs by viewModel.selectedMeetingForDocs.collectAsState()

    var newMeetingTitleInput by remember { mutableStateOf("") }
    var showStartDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var serverUrlInput by remember {
        mutableStateOf(com.enterprise.meetai.worker.UploadMeetingWorker.getServerUrl(context))
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = BgDark,
        topBar = {
            HomeTopBar(
                onOpenSettings = { showSettingsDialog = true }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                QuickActionBanners(
                    isRecording = isRecording,
                    onStartClick = {
                        showStartDialog = true
                    }
                )
            }

            // Banner status perekaman aktif (jika sedang merekam)
            item {
                AnimatedVisibility(
                    visible = isRecording,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ActiveRecordingCard(
                        elapsedSeconds = elapsedSeconds,
                        amplitude = currentAmplitude,
                        onStopClick = { viewModel.stopRecording() }
                    )
                }
            }

            // Judul Section Rapat Terakhir
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Meeting",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${meetings.size} Tersimpan",
                        color = TextDim,
                        fontSize = 13.sp
                    )
                }
            }

            // List Kartu Rapat
            items(meetings) { meeting ->
                MeetingItemCard(
                    meeting = meeting,
                    onClick = {
                        viewModel.selectMeetingForDocs(meeting)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Dialog Input Judul Meeting sebelum Mulai Rekam
        if (showStartDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showStartDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Mulai Rekam Percakapan Rapat",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Audio akan direkam dengan akselerasi AAC 16kHz hemat baterai di latar belakang.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = newMeetingTitleInput,
                            onValueChange = { newMeetingTitleInput = it },
                            label = { Text("Agenda / Judul Rapat") },
                            placeholder = { Text("Contoh: Rapat Evaluasi Q3") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = CardBorder
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { showStartDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Text("Batal", color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val title = newMeetingTitleInput.ifBlank { "Meeting Internal" }
                                    viewModel.startRecording(title)
                                    showStartDialog = false
                                    onRequestPermissionAndRecord()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                            ) {
                                Text("Mulai Rekam", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sheet 4 Dokumen Hasil AI
        selectedMeetingForDocs?.let { meeting ->
            DocumentsViewerModal(
                meeting = meeting,
                sheetState = sheetState,
                onDismiss = { viewModel.selectMeetingForDocs(null) }
            )
        }

        // Dialog Pengaturan IP Server Backend (Wi-Fi Local Testing)
        if (showSettingsDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showSettingsDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Pengaturan Server Backend",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Masukkan alamat URL / IP backend di jaringan Wi-Fi lokal komputer Anda. Format: http://<IP-KOMPUTER>:8000",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = { serverUrlInput = it },
                            label = { Text("URL Server Backend") },
                            placeholder = { Text("http://192.168.1.5:8000") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = CardBorder
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { showSettingsDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Text("Tutup", color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val cleanedUrl = serverUrlInput.trim().trimEnd('/')
                                    com.enterprise.meetai.worker.UploadMeetingWorker.saveServerUrl(context, cleanedUrl)
                                    showSettingsDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                            ) {
                                Text("Simpan URL", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar profile
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PurplePrimary, PinkSparkle))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "MeetAI Workspace 🏢",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = "Internal Meeting",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .background(CardDark, shape = CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Server Settings", tint = PurpleGlow)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(38.dp)
                    .background(CardDark, shape = CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = "Alerts", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun QuickActionBanners(
    isRecording: Boolean,
    onStartClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Kartu Utama Kiri: AI Auto-Extract Highlight
        Box(
            modifier = Modifier
                .weight(1.2f)
                .height(130.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2B2553),
                            Color(0xFF1E1A3C)
                        )
                    )
                )
                .border(1.dp, PurpleGlow.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(PurplePrimary.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PurpleGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "AI auto-extracts 4 dokumen tanpa plot hole",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp
                )
            }
        }

        // Kartu Kanan: Tombol Rekam & Tombol Status
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tombol Record Audio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isRecording) Color(0xFFE11D48) else PurplePrimary)
                    .clickable { onStartClick() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "Merekam..." else "Rekam Rapat",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tombol 4 Dokumen Output
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = PinkSparkle,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "4 Output AI",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveRecordingCard(
    elapsedSeconds: Long,
    amplitude: Int,
    onStopClick: () -> Unit
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Tracker Recording (AAC 32kbps)",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = timeFormatted,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Waveform visualizer real-time
            WaveformVisualizer(
                amplitude = amplitude,
                isRecording = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layar HP dapat dimatikan, rekaman aman berjalan.",
                    color = TextDim,
                    fontSize = 11.sp
                )

                Button(
                    onClick = onStopClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Selesai", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MeetingItemCard(
    meeting: MeetingItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header tanggal & durasi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${meeting.dateFormatted} • ${meeting.durationFormatted}",
                    color = TextDim,
                    fontSize = 12.sp
                )
                Text(
                    text = meeting.fileSizeFormatted,
                    color = TextDim,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Judul
            Text(
                text = meeting.title,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Ringkasan singkat
            Text(
                text = meeting.summarySnippet,
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Chip Indikator Dokumen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DocumentChip(name = "1. Notulen", ready = meeting.status == ProcessingStatus.COMPLETED)
                DocumentChip(name = "2. Ringkasan", ready = meeting.status == ProcessingStatus.COMPLETED)
                DocumentChip(name = "3. Poin", ready = meeting.status == ProcessingStatus.COMPLETED)
                DocumentChip(name = "4. Q&A", ready = meeting.status == ProcessingStatus.COMPLETED)
            }
        }
    }
}

@Composable
private fun DocumentChip(name: String, ready: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (ready) PurpleAccent.copy(alpha = 0.25f) else Color(0xFF27273A),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (ready) PurpleAccent else Color(0xFF383852),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = name,
            color = if (ready) PurpleGlow else TextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
