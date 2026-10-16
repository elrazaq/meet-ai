package com.enterprise.meetai

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.enterprise.meetai.ui.HomeScreen
import com.enterprise.meetai.ui.MeetingViewModel
import com.enterprise.meetai.ui.components.ProminentDisclosureDialog
import com.enterprise.meetai.ui.theme.MeetAiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MeetingViewModel by viewModels()

    // ActivityResultLauncher untuk memilih file audio dari penyimpanan HP
    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importAudioFile(it, this)
            Toast.makeText(this, "File audio berhasil dipilih. Memulai ekstraksi 4 dokumen AI...", Toast.LENGTH_SHORT).show()
        }
    }

    // ActivityResultLauncher untuk izin mikrofon & notifikasi
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (micGranted) {
            Toast.makeText(this, "Izin mikrofon aktif. Memulai perekaman rapat.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Izin mikrofon dibutuhkan untuk merekam rapat internal.", Toast.LENGTH_LONG).show()
            viewModel.stopRecording()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MeetAiTheme {
                val showDisclosure by viewModel.showDisclosure.collectAsState()

                HomeScreen(
                    viewModel = viewModel,
                    onRequestPermissionAndRecord = {
                        checkAndRequestMicrophonePermission()
                    },
                    onPickAudioFile = {
                        pickAudioLauncher.launch("audio/*")
                    }
                )

                // Dialog Prominent Disclosure sesuai Kepatuhan Kebijakan Google Play Store
                if (showDisclosure) {
                    ProminentDisclosureDialog(
                        onDismiss = {
                            viewModel.setShowDisclosure(false)
                        },
                        onAccept = {
                            viewModel.setShowDisclosure(false)
                            launchSystemPermissions()
                        }
                    )
                }
            }
        }
    }

    private fun checkAndRequestMicrophonePermission() {
        val hasMic = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMic) {
            viewModel.setShowDisclosure(true)
        } else {
            checkNotificationPermissionOnly()
        }
    }

    private fun launchSystemPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun checkNotificationPermissionOnly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }
}
