package com.enterprise.meetai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enterprise.meetai.model.MeetingItem
import com.enterprise.meetai.model.ProcessingStatus
import com.enterprise.meetai.ui.components.DocumentsViewerModal
import com.enterprise.meetai.ui.components.WaveformVisualizer
import com.enterprise.meetai.ui.theme.AmberPending
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
import com.enterprise.meetai.utils.PdfExportUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MeetingViewModel,
    onRequestPermissionAndRecord: () -> Unit,
    onPickAudioFile: () -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val currentAmplitude by viewModel.currentAmplitude.collectAsState()
    val meetings by viewModel.meetings.collectAsState()
    val selectedMeetingForDocs by viewModel.selectedMeetingForDocs.collectAsState()

    var newMeetingTitleInput by remember { mutableStateOf("") }
    var showStartDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedBottomNavIndex by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    var serverUrlInput by remember {
        mutableStateOf(com.enterprise.meetai.worker.UploadMeetingWorker.getServerUrl(context))
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = BgDark,
        topBar = {
            HomeTopBar(onOpenSettings = { showSettingsDialog = true })
        },
        bottomBar = {
            MeetAiBottomNavigation(
                selectedIndex = selectedBottomNavIndex,
                onSelectIndex = { index ->
                    selectedBottomNavIndex = index
                    if (index == 2) {
                        if (isRecording) viewModel.stopRecording() else showStartDialog = true
                    } else if (index == 3) {
                        meetings.firstOrNull { it.status == ProcessingStatus.COMPLETED }?.let {
                            viewModel.selectMeetingForDocs(it)
                        }
                    } else if (index == 4) {
                        showSettingsDialog = true
                    }
                },
                isRecording = isRecording
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Quick Action Cards (Record & Upload Audio File)
            item {
                QuickActionBanners(
                    isRecording = isRecording,
                    onStartClick = { showStartDialog = true },
                    onUploadFileClick = onPickAudioFile
                )
            }

            // Banner status perekaman aktif (Pause, Resume, Stop)
            item {
                AnimatedVisibility(
                    visible = isRecording,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ActiveRecordingCard(
                        elapsedSeconds = elapsedSeconds,
                        amplitude = currentAmplitude,
                        isPaused = isPaused,
                        onPauseClick = { viewModel.pauseRecording() },
                        onResumeClick = { viewModel.resumeRecording() },
                        onStopClick = { viewModel.stopRecording() }
                    )
                }
            }

            // Section Header: Recent Meetings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Meeting",
                        color = TextWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${meetings.size} Rapat Tersimpan",
                        color = TextDim,
                        fontSize = 12.sp
                    )
                }
            }

            // List Kartu Rapat Permanen
            items(meetings, key = { it.id }) { meeting ->
                MeetingItemCard(
                    meeting = meeting,
                    onClick = {
                        if (meeting.status == ProcessingStatus.COMPLETED) {
                            viewModel.selectMeetingForDocs(meeting)
                        } else if (meeting.status == ProcessingStatus.FAILED) {
                            viewModel.uploadMeetingToAi(meeting)
                        }
                    },
                    onExportPdf = {
                        PdfExportUtils.exportAndSharePdf(context, meeting)
                    },
                    onRetryUpload = {
                        viewModel.uploadMeetingToAi(meeting)
                    },
                    onDelete = {
                        viewModel.deleteMeeting(meeting.id)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Dialog Input Judul Meeting sebelum Mulai Rekam
        if (showStartDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showStartDialog = false }) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "Mulai Rekam Rapat Baru",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Audio direkam dengan kompresi AAC hardware 16kHz hemat daya. Mendukung pause/resume dan auto-pause saat telepon.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
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
                                Text("Mulai Rekam", color = Color.White, fontWeight = FontWeight.SemiBold)
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

        // Dialog Pengaturan IP Server Backend
        if (showSettingsDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showSettingsDialog = false }) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "Pengaturan Server Backend",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Masukkan alamat IP backend laptop di jaringan Wi-Fi lokal. Format: http://<IP>:8000",
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
                                Text("Simpan URL", color = Color.White, fontWeight = FontWeight.SemiBold)
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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PurplePrimary, PinkSparkle))),
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
                    fontSize = 11.sp
                )
                Text(
                    text = "Internal Meeting Engine",
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
                    .size(40.dp)
                    .background(CardDark, shape = CircleShape)
                    .border(1.dp, CardBorder, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = PurpleGlow, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(40.dp)
                    .background(CardDark, shape = CircleShape)
                    .border(1.dp, CardBorder, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = "Alerts", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun QuickActionBanners(
    isRecording: Boolean,
    onStartClick: () -> Unit,
    onUploadFileClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Card Kiri (AI Banner Gradient)
        Box(
            modifier = Modifier
                .weight(1.2f)
                .height(138.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C2454),
                            Color(0xFF1D183B)
                        )
                    )
                )
                .border(1.2.dp, PurpleGlow.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
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
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "AI Ekstrak 4 Dokumen\nTanpa Plot Hole",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp
                )
            }
        }

        // Dua Tombol Kanan: Rekam Rapat & Upload Audio
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tombol 1: Rekam Suara Langsung
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isRecording) Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C)))
                        else Brush.horizontalGradient(listOf(PurplePrimary, PurpleAccent))
                    )
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tombol 2: Upload File Audio dari HP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .clickable { onUploadFileClick() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = PinkSparkle,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload Audio",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
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
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isPaused) AmberPending.copy(alpha = 0.8f) else Color(0xFFE11D48).copy(alpha = 0.8f),
                RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = if (isPaused) AmberPending else Color(0xFFEF4444),
                        modifier = Modifier
                            .size(16.dp)
                            .then(if (!isPaused) Modifier.alpha(alphaAnim) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPaused) "REKAMAN DIJEDA (PAUSED)" else "SEDANG MEREKAM RAPAT",
                        color = if (isPaused) AmberPending else Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = timeFormatted,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            WaveformVisualizer(
                amplitude = if (isPaused) 0 else amplitude,
                isRecording = !isPaused,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { if (isPaused) onResumeClick() else onPauseClick() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) EmeraldSuccess else Color(0xFF332D56)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPaused) "Lanjutkan" else "Jeda",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onStopClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Selesai (Stop)",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MeetingItemCard(
    meeting: MeetingItem,
    onClick: () -> Unit,
    onExportPdf: () -> Unit,
    onRetryUpload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Tanggal, Durasi & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${meeting.dateFormatted} • ${meeting.durationFormatted}",
                    color = TextDim,
                    fontSize = 11.sp
                )

                StatusBadge(status = meeting.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Judul Rapat
            Text(
                text = meeting.title,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Ringkasan Snippet
            Text(
                text = meeting.summarySnippet,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Chip Indikator Dokumen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DocumentChip(name = "1. Notulen", ready = meeting.status == ProcessingStatus.COMPLETED)
                DocumentChip(name = "2. Ringkasan", ready = meeting.status == ProcessingStatus.COMPLETED)
                DocumentChip(name = "3. Poin", ready = meeting.status == ProcessingStatus.COMPLETED)
                DocumentChip(name = "4. Q&A", ready = meeting.status == ProcessingStatus.COMPLETED)
            }

            if (meeting.status == ProcessingStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(14.dp))
                // Toolbar Aksi Cepat: Baca Dokumen, Ekspor PDF, Hapus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(PurpleAccent, CardBorder)))
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = PurpleGlow, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baca Dokumen", color = TextWhite, fontSize = 11.sp)
                    }

                    Button(
                        onClick = onExportPdf,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF221F38), shape = CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = TextDim, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (meeting.status == ProcessingStatus.FAILED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onRetryUpload,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Coba Upload Ulang", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ProcessingStatus) {
    when (status) {
        ProcessingStatus.COMPLETED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(EmeraldSuccess.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("4 Dokumen Siap", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        ProcessingStatus.AI_PROCESSING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(AmberPending.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                CircularProgressIndicator(
                    color = AmberPending,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Menganalisis...", color = AmberPending, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        ProcessingStatus.UPLOADING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(BlueInfo.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                CircularProgressIndicator(
                    color = BlueInfo,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mengunggah...", color = BlueInfo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        ProcessingStatus.FAILED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFFE11D48).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gagal • Coba Lagi", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        ProcessingStatus.RECORDED_LOCAL -> {
            Text(
                text = "Tersimpan Lokal",
                color = TextDim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DocumentChip(name: String, ready: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (ready) PurpleAccent.copy(alpha = 0.25f) else Color(0xFF221F38),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (ready) PurpleAccent else Color(0xFF332F52),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = name,
            color = if (ready) PurpleGlow else TextDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Bottom Navigation Bar modern sesuai referensi desain UI asli
 */
@Composable
private fun MeetAiBottomNavigation(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    isRecording: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: Home
            IconButton(onClick = { onSelectIndex(0) }) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Beranda",
                    tint = if (selectedIndex == 0) PurpleGlow else TextDim
                )
            }

            // Tab 1: History
            IconButton(onClick = { onSelectIndex(1) }) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Riwayat",
                    tint = if (selectedIndex == 1) PurpleGlow else TextDim
                )
            }

            // Tab 2: Floating Sparkle AI Center Action Button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C)))
                        else Brush.linearGradient(listOf(PurplePrimary, PinkSparkle))
                    )
                    .clickable { onSelectIndex(2) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.AutoAwesome,
                    contentDescription = "Rekam AI",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Tab 3: Documents
            IconButton(onClick = { onSelectIndex(3) }) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Dokumen",
                    tint = if (selectedIndex == 3) PurpleGlow else TextDim
                )
            }

            // Tab 4: Settings
            IconButton(onClick = { onSelectIndex(4) }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    tint = if (selectedIndex == 4) PurpleGlow else TextDim
                )
            }
        }
    }
}
