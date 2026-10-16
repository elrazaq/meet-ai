package com.enterprise.meetai.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// PALET WARNA KORPORAT: EXECUTIVE SLATE & SOFT TEAL
// Lebih kalem, lembut, dan elegan di mata
// ==========================================

// Mode Gelap (Dark Mode) - Deep Slate
val SlateDarkBg = Color(0xFF0B0F19)           // Background utama gelap lembut
val SlateDarkSurface = Color(0xFF131B2E)      // Surface card & bar
val SlateDarkCard = Color(0xFF1B2438)         // Card isi
val SlateDarkBorder = Color(0xFF28354D)       // Border halus

// Mode Terang (Light Mode) - Soft Platinum
val SlateLightBg = Color(0xFFF6F8FC)          // Background terang lembut tidak silau
val SlateLightSurface = Color(0xFFFFFFFF)     // Surface putih bersih
val SlateLightCard = Color(0xFFFFFFFF)        // Card putih
val SlateLightBorder = Color(0xFFE2E8F0)      // Border abu terang

// Aksen Utama: Soft Steel Teal & Deep Cyan (Bukan ungu)
val ExecutiveTeal = Color(0xFF0EA5E9)         // Sky Blue / Teal modern
val ExecutiveTealDark = Color(0xFF0284C7)     // Versi sedikit lebih pekat untuk light mode
val ExecutiveCyanGlow = Color(0xFF38BDF8)     // Highlight lembut
val SageAccent = Color(0xFF14B8A6)            // Teal sage sejuk

// Status Colors
val EmeraldSuccess = Color(0xFF10B981)
val AmberPending = Color(0xFFF59E0B)
val RoseError = Color(0xFFF43F5E)
val BlueInfo = Color(0xFF3B82F6)

// Tipografi Gelap
val TextDarkPrimary = Color(0xFFF8FAFC)
val TextDarkSecondary = Color(0xFF94A3B8)
val TextDarkDim = Color(0xFF64748B)

// Tipografi Terang
val TextLightPrimary = Color(0xFF0F172A)
val TextLightSecondary = Color(0xFF475569)
val TextLightDim = Color(0xFF94A3B8)

// Compatibility aliases agar tidak mematahkan kode lama yang mengimpor nama lama
val BgDark = SlateDarkBg
val SurfaceDark = SlateDarkSurface
val CardDark = SlateDarkCard
val CardBorder = SlateDarkBorder
val PurplePrimary = ExecutiveTeal
val PurpleAccent = ExecutiveTealDark
val PurpleGlow = ExecutiveCyanGlow
val PinkSparkle = SageAccent
val TextWhite = TextDarkPrimary
val TextMuted = TextDarkSecondary
val TextDim = TextDarkDim
val WaveformActive = ExecutiveTeal
val WaveformInactive = Color(0xFF334155)
