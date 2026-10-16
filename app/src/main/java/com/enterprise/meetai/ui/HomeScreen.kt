package com.enterprise.meetai.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enterprise.meetai.model.MeetingItem
import com.enterprise.meetai.model.ProcessingStatus
import com.enterprise.meetai.model.ScheduledMeeting
import com.enterprise.meetai.ui.components.DocumentsViewerModal
import com.enterprise.meetai.ui.components.WaveformVisualizer
import com.enterprise.meetai.ui.theme.LocalMeetAiColors
import com.enterprise.meetai.utils.PdfExportUtils
import com.enterprise.meetai.worker.UploadMeetingWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MeetingViewModel,
    onRequestPermissionAndRecord: () -> Unit,
    onPickAudioFile: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalMeetAiColors.current

    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val currentAmplitude by viewModel.currentAmplitude.collectAsState()
    val meetings by viewModel.meetings.collectAsState()
    val scheduledMeetings by viewModel.scheduledMeetings.collectAsState()
    val selectedMeetingForDocs by viewModel.selectedMeetingForDocs.collectAsState()
    val pingState by viewModel.pingState.collectAsState()

    var currentTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var meetingToDelete by remember { mutableStateOf<MeetingItem?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            ModernBottomNavBar(
                currentTab = currentTab,
                onTabSelect = { tabIndex ->
                    currentTab = tabIndex
                    if (tabIndex == 4) {
                        showSettingsDialog = true
                    }
                },
                onCenterAiClick = {
                    if (isRecording) {
                        viewModel.stopRecording()
                    } else {
                        onRequestPermissionAndRecord()
                    }
                },
                isRecording = isRecording
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                0, 1 -> {
                    // Home & Recent Meetings Dashboard
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                            CorporateTopBar(
                                onOpenSchedule = { showScheduleDialog = true },
                                onOpenSettings = { showSettingsDialog = true }
                            )
                        }

                        // Mini Dashboard Statistik Eksekutif
                        item {
                            MiniExecutiveDashboard(
                                meetings = meetings,
                                onOpenSchedule = { showScheduleDialog = true }
                            )
                        }

                        // Banner Aksi Cepat (Rekam & Upload Audio)
                        item {
                            QuickActionBanners(
                                isRecording = isRecording,
                                onStartClick = {
                                    if (isRecording) viewModel.stopRecording()
                                    else onRequestPermissionAndRecord()
                                },
                                onUploadFileClick = onPickAudioFile
                            )
                        }

                        // Card Rekaman Berjalan
                        item {
                            AnimatedVisibility(
                                visible = isRecording,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
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

                        // Jadwal Rapat Mendatang (Jika Ada)
                        if (scheduledMeetings.isNotEmpty()) {
                            item {
                                ScheduledMeetingsBar(
                                    schedules = scheduledMeetings,
                                    onDeleteSchedule = { viewModel.deleteScheduledMeeting(it.id) }
                                )
                            }
                        }

                        // Section Title Rapat Terbaru
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daftar Rapat Tersimpan",
                                    color = colors.textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "${meetings.size} Dokumen",
                                    color = colors.textDim,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // List Rapat
                        if (meetings.isEmpty()) {
                            item {
                                EmptyStateCard(onUploadClick = onPickAudioFile)
                            }
                        } else {
                            items(meetings, key = { it.id }) { meeting ->
                                MeetingItemCard(
                                    meeting = meeting,
                                    onClick = { viewModel.selectMeetingForDocs(meeting) },
                                    onExportPdf = { PdfExportUtils.exportAndShareCorporatePdf(context, meeting) },
                                    onRetryUpload = { viewModel.uploadMeetingToAi(meeting) },
                                    onDelete = { meetingToDelete = meeting }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }
                }

                2, 3 -> {
                    // Tampilan Menu PDF Khusus Berbentuk File Manager Rapi
                    PdfFileManagerView(
                        meetings = meetings.filter { it.status == ProcessingStatus.COMPLETED },
                        onViewDocs = { viewModel.selectMeetingForDocs(it) },
                        onExportPdf = { PdfExportUtils.exportAndShareCorporatePdf(context, it) },
                        onCopyText = { meeting ->
                            val fullText = buildString {
                                appendLine("=== ${meeting.title.uppercase()} ===")
                                appendLine("Tanggal: ${meeting.dateFormatted}")
                                appendLine("\n1. NOTULEN RAPAT:")
                                appendLine(meeting.documents.notulenContent ?: "-")
                                appendLine("\n2. RINGKASAN EKSEKUTIF:")
                                appendLine(meeting.documents.ringkasanContent ?: "-")
                                appendLine("\n3. POIN-POIN & DATA KUNCI:")
                                appendLine(meeting.documents.poinPentingContent ?: "-")
                                appendLine("\n4. REKAP TANYA JAWAB & ANTI-PLOT-HOLE:")
                                appendLine(meeting.documents.tanyaJawabContent ?: "-")
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MeetAI Meeting Docs", fullText))
                            Toast.makeText(context, "Seluruh notulen & ringkasan berhasil disalin!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Modal Viewer 4 Dokumen
    selectedMeetingForDocs?.let { meeting ->
        DocumentsViewerModal(
            meeting = meeting,
            sheetState = sheetState,
            onDismiss = { viewModel.selectMeetingForDocs(null) }
        )
    }

    // Dialog Pengaturan Server Backend dengan Test Ping
    if (showSettingsDialog) {
        ServerSettingsDialog(
            currentUrl = UploadMeetingWorker.getServerUrl(context),
            pingState = pingState,
            onPingTest = { url -> viewModel.testServerPing(url) },
            onSave = { newUrl ->
                UploadMeetingWorker.saveServerUrl(context, newUrl)
                viewModel.resetPingState()
                showSettingsDialog = false
                Toast.makeText(context, "Server URL disimpan!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                viewModel.resetPingState()
                showSettingsDialog = false
            }
        )
    }

    // Dialog Penjadwalan Rapat & Google Calendar
    if (showScheduleDialog) {
        ScheduleMeetingDialog(
            onSaveAndSync = { title, notes, timeMillis, formattedTime ->
                viewModel.addScheduledMeeting(title, notes, timeMillis, formattedTime)

                // Buka Google Calendar / Native Calendar dengan Intent ACTION_INSERT
                try {
                    val calIntent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, title)
                        putExtra(CalendarContract.Events.DESCRIPTION, notes)
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, timeMillis)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, timeMillis + (60 * 60 * 1000))
                    }
                    context.startActivity(calIntent)
                    Toast.makeText(context, "Membuka Google Calendar...", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Jadwal tersimpan di aplikasi!", Toast.LENGTH_SHORT).show()
                }
                showScheduleDialog = false
            },
            onDismiss = { showScheduleDialog = false }
        )
    }

    // Dialog Konfirmasi Hapus Rapat (Anti Tidak Sengaja Tertekan)
    meetingToDelete?.let { meeting ->
        AlertDialog(
            onDismissRequest = { meetingToDelete = null },
            title = {
                Text("Hapus Rekaman Rapat?", fontWeight = FontWeight.Bold, color = colors.textPrimary)
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus '${meeting.title}'? File audio dan seluruh 4 dokumen hasil analisis AI akan dihapus permanen.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMeeting(meeting.id)
                        meetingToDelete = null
                        Toast.makeText(context, "Rapat berhasil dihapus.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Hapus Permanen", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { meetingToDelete = null }) {
                    Text("Batal", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }
}

/**
 * Top Bar Korporat Bersih & Elegan
 */
@Composable
private fun CorporateTopBar(
    onOpenSchedule: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = LocalMeetAiColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "MeetAI Workspace 🏢",
                    color = colors.textDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Corporate Meeting Hub",
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Tombol Jadwal Kalender Rapat
            IconButton(
                onClick = onOpenSchedule,
                modifier = Modifier
                    .size(38.dp)
                    .background(colors.card, shape = CircleShape)
                    .border(1.dp, colors.cardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Jadwal Rapat & Kalender",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Tombol Pengaturan Server
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .background(colors.card, shape = CircleShape)
                    .border(1.dp, colors.cardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan Server",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Mini Executive Dashboard: Statistik Rapat & Durasi Rata-rata
 */
@Composable
private fun MiniExecutiveDashboard(
    meetings: List<MeetingItem>,
    onOpenSchedule: () -> Unit
) {
    val colors = LocalMeetAiColors.current

    val totalMeetings = meetings.size
    val uploadedFiles = meetings.count { it.isUploadedFile || it.title.contains("External", ignoreCase = true) }
    val directRecordings = totalMeetings - uploadedFiles

    // Hitung rata-rata durasi
    val completedWithDuration = meetings.filter { it.durationSeconds > 0 }
    val avgMinutes = if (completedWithDuration.isNotEmpty()) {
        (completedWithDuration.map { it.durationSeconds }.average() / 60).toInt()
    } else if (totalMeetings > 0) 35 else 0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Statistik & Kinerja Rapat",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "+ Jadwalkan Rapat",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onOpenSchedule() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItemBox(
                    label = "Total Sesi",
                    value = "$totalMeetings",
                    sub = "Tersimpan",
                    modifier = Modifier.weight(1f)
                )

                StatItemBox(
                    label = "File Upload",
                    value = "$uploadedFiles",
                    sub = "Diimpor",
                    modifier = Modifier.weight(1f)
                )

                StatItemBox(
                    label = "Rekam Mic",
                    value = "$directRecordings",
                    sub = "Langsung",
                    modifier = Modifier.weight(1f)
                )

                StatItemBox(
                    label = "Rata Durasi",
                    value = "$avgMinutes",
                    sub = "Menit/sesi",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItemBox(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeetAiColors.current

    Box(
        modifier = modifier
            .background(colors.surface, shape = RoundedCornerShape(12.dp))
            .border(1.dp, colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = colors.textDim, fontSize = 9.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = sub, color = colors.primary, fontSize = 8.sp, maxLines = 1)
        }
    }
}

/**
 * Banner Aksi Cepat Bersih: Rekam & Upload Audio (Single Line, No Awkward Wrap)
 */
@Composable
private fun QuickActionBanners(
    isRecording: Boolean,
    onStartClick: () -> Unit,
    onUploadFileClick: () -> Unit
) {
    val colors = LocalMeetAiColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Card Kiri (AI Banner Kalem)
        Box(
            modifier = Modifier
                .weight(1.1f)
                .height(130.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.card,
                            colors.surface
                        )
                    )
                )
                .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(colors.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "AI Notulensi Cerdas",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "4 Dokumen Tanpa Plot Hole",
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Dua Tombol Kanan: Rekam Rapat & Upload Audio (Single Line, Clean)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tombol 1: Rekam Suara Langsung
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isRecording) Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C)))
                        else Brush.horizontalGradient(listOf(colors.primary, colors.primaryVariant))
                    )
                    .clickable { onStartClick() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "Berhenti" else "Rekam Rapat",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Tombol 2: Upload File Audio dari HP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                    .clickable { onUploadFileClick() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload Audio",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Baris Jadwal Rapat Mendatang
 */
@Composable
private fun ScheduledMeetingsBar(
    schedules: List<ScheduledMeeting>,
    onDeleteSchedule: (ScheduledMeeting) -> Unit
) {
    val colors = LocalMeetAiColors.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Jadwal Rapat Mendatang",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${schedules.size} Agenda",
                    color = colors.textDim,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            schedules.take(2).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = item.formattedDateTime,
                            color = colors.textDim,
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = { onDeleteSchedule(item) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus Jadwal",
                            tint = colors.textDim,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card Rekaman Sedang Berjalan
 */
@Composable
private fun ActiveRecordingCard(
    elapsedSeconds: Long,
    amplitude: Int,
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val colors = LocalMeetAiColors.current
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, if (isPaused) colors.warning else colors.primary, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isPaused) colors.warning else Color(0xFFEF4444).copy(alpha = alphaAnim),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPaused) "REKAMAN DIJEDA (PAUSED)" else "SEDANG MEREKAM RAPAT",
                        color = if (isPaused) colors.warning else Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "AAC 16kHz • 32kbps",
                    color = colors.textDim,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = timeFormatted,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            WaveformVisualizer(
                amplitude = if (isPaused) 0 else amplitude,
                isRecording = !isPaused,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { if (isPaused) onResumeClick() else onPauseClick() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPaused) "Lanjut" else "Jeda",
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onStopClick,
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Selesai (Stop)",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Card Item Rapat dengan Status Badge Rapi Tanpa Wrap Aneh
 */
@Composable
private fun MeetingItemCard(
    meeting: MeetingItem,
    onClick: () -> Unit,
    onExportPdf: () -> Unit,
    onRetryUpload: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMeetAiColors.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Tanggal & Status Badge Rapi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${meeting.dateFormatted} • ${meeting.durationFormatted}",
                    color = colors.textDim,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                StatusBadge(status = meeting.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = meeting.title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = meeting.summarySnippet,
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                Spacer(modifier = Modifier.height(12.dp))
                // Toolbar Aksi Cepat (Single Line, No Wrap)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = colors.primary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buka Dokumen", color = colors.textPrimary, fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = onExportPdf,
                        modifier = Modifier.weight(0.9f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.surface, shape = CircleShape)
                            .border(1.dp, colors.cardBorder, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = colors.textDim, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (meeting.status == ProcessingStatus.FAILED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onRetryUpload,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Coba Upload Ulang", color = Color(0xFFEF4444), fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Status Badge Rapi Tanpa Wrap Teks Vertikal
 */
@Composable
private fun StatusBadge(status: ProcessingStatus) {
    val colors = LocalMeetAiColors.current

    when (status) {
        ProcessingStatus.COMPLETED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colors.success.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("4 Dokumen Siap", color = colors.success, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        ProcessingStatus.AI_PROCESSING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colors.warning.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = colors.warning
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Menganalisis...", color = colors.warning, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        ProcessingStatus.UPLOADING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colors.info.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = colors.info
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mengunggah...", color = colors.info, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        ProcessingStatus.RECORDED_LOCAL -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colors.cardBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Audio Lokal", color = colors.textDim, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
        ProcessingStatus.FAILED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colors.error.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gagal", color = colors.error, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DocumentChip(name: String, ready: Boolean) {
    val colors = LocalMeetAiColors.current

    Box(
        modifier = Modifier
            .background(
                if (ready) colors.primary.copy(alpha = 0.12f) else colors.surface,
                shape = RoundedCornerShape(6.dp)
            )
            .border(0.8.dp, if (ready) colors.primary.copy(alpha = 0.4f) else colors.cardBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = name,
            color = if (ready) colors.primary else colors.textDim,
            fontSize = 9.sp,
            fontWeight = if (ready) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/**
 * Tampilan Menu PDF Berbentuk File Manager Rapi
 */
@Composable
private fun PdfFileManagerView(
    meetings: List<MeetingItem>,
    onViewDocs: (MeetingItem) -> Unit,
    onExportPdf: (MeetingItem) -> Unit,
    onCopyText: (MeetingItem) -> Unit
) {
    val colors = LocalMeetAiColors.current
    var searchQuery by remember { mutableStateOf("") }

    val filtered = meetings.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.dateFormatted.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manajer Dokumen PDF Resmi",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Daftar notulen rapat siap diekspor menjadi PDF A4 atau dibagikan ke WhatsApp",
                color = colors.textDim,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Kolom Pencarian
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari file dokumen...", color = colors.textDim, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textDim) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.cardBorder,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.card,
                    unfocusedContainerColor = colors.card
                ),
                singleLine = true
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.textDim, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada dokumen PDF rapat", color = colors.textDim, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.title}.pdf",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.dateFormatted} • Format Standar A4",
                                    color = colors.textDim,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = colors.cardBorder, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onExportPdf(item) },
                                modifier = Modifier.weight(1.3f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kirim / Cetak PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { onCopyText(item) },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin", color = colors.textPrimary, fontSize = 11.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { onViewDocs(item) },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Lihat", color = colors.textPrimary, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Bottom Nav Bar Modern
 */
@Composable
private fun ModernBottomNavBar(
    currentTab: Int,
    onTabSelect: (Int) -> Unit,
    onCenterAiClick: () -> Unit,
    isRecording: Boolean
) {
    val colors = LocalMeetAiColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(0.8.dp, colors.cardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(72.dp)
        ) {
            NavigationBarItem(
                selected = currentTab == 0,
                onClick = { onTabSelect(0) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(22.dp)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    unselectedIconColor = colors.textDim,
                    indicatorColor = colors.primary.copy(alpha = 0.12f)
                )
            )

            NavigationBarItem(
                selected = currentTab == 1,
                onClick = { onTabSelect(1) },
                icon = { Icon(Icons.Default.Description, contentDescription = "Rapat", modifier = Modifier.size(22.dp)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    unselectedIconColor = colors.textDim,
                    indicatorColor = colors.primary.copy(alpha = 0.12f)
                )
            )

            // Tombol AI Tengah Sparkle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C)))
                        else Brush.linearGradient(listOf(colors.primary, colors.accent))
                    )
                    .clickable { onCenterAiClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.AutoAwesome,
                    contentDescription = "AI Rekam",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            NavigationBarItem(
                selected = currentTab == 3,
                onClick = { onTabSelect(3) },
                icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF File Manager", modifier = Modifier.size(22.dp)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    unselectedIconColor = colors.textDim,
                    indicatorColor = colors.primary.copy(alpha = 0.12f)
                )
            )

            NavigationBarItem(
                selected = currentTab == 4,
                onClick = { onTabSelect(4) },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(22.dp)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    unselectedIconColor = colors.textDim,
                    indicatorColor = colors.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

/**
 * Dialog Pengaturan Server Backend dengan Fitur Test Ping
 */
@Composable
private fun ServerSettingsDialog(
    currentUrl: String,
    pingState: PingState,
    onPingTest: (String) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMeetAiColors.current
    var urlText by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pengaturan Server Backend", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Masukkan alamat IP server backend MeetAI (FastAPI). Pastikan HP dan PC berada pada jaringan Wi-Fi yang sama.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Server URL", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.cardBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                // Tombol Test Ping
                Button(
                    onClick = { onPingTest(urlText.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(colors.primary, colors.cardBorder)))
                ) {
                    if (pingState is PingState.Pinging) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menguji Ping Server...", color = colors.textPrimary, fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tes Koneksi (Ping)", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Status Hasil Ping
                when (pingState) {
                    is PingState.Success -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.success.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(text = "✓ ${pingState.message}", color = colors.success, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    is PingState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.error.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(text = "✗ ${pingState.message}", color = colors.error, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(urlText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Simpan URL", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface
    )
}

/**
 * Dialog Penjadwalan Rapat & Sinkron Google Calendar
 */
@Composable
private fun ScheduleMeetingDialog(
    onSaveAndSync: (title: String, notes: String, timeMillis: Long, formattedDateTime: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMeetAiColors.current
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Jadwalkan Rapat & Google Calendar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Buat pengingat rapat internal. Jadwal ini akan otomatis disinkronkan ke aplikasi Google Calendar di HP Anda.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Rapat", fontSize = 11.sp) },
                    placeholder = { Text("Contoh: Rapat Evaluasi Anggaran Q3", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.cardBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Agenda", fontSize = 11.sp) },
                    placeholder = { Text("Poin-poin bahasan atau link meeting", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.cardBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 2) }
                    val formatted = SimpleDateFormat("EEEE, d MMMM yyyy (HH:mm)", Locale("id", "ID")).format(cal.time)
                    onSaveAndSync(title.trim(), notes.trim(), cal.timeInMillis, formatted)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Simpan & Buka Kalender", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface
    )
}

@Composable
private fun EmptyStateCard(onUploadClick: () -> Unit) {
    val colors = LocalMeetAiColors.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = colors.textDim, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Belum Ada Rapat Tersimpan", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Mulai rekam rapat atau upload rekaman audio untuk mengekstrak 4 dokumen terstruktur.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUploadClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload File Audio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
