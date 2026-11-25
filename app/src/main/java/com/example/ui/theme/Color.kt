package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Crisp Modern Fintech — Emerald & Slate Palette
val EmeraldPrimaryLight = Color(0xFF059669)
val EmeraldOnPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFD1FAE5)
val EmeraldOnContainerLight = Color(0xFF064E3B)

val SlateSecondaryLight = Color(0xFF334155)
val SlateOnSecondaryLight = Color(0xFFFFFFFF)
val SlateContainerLight = Color(0xFFE2E8F0)
val SlateOnContainerLight = Color(0xFF0F172A)

val TealTertiaryLight = Color(0xFF0D9488)
val TealOnTertiaryLight = Color(0xFFFFFFFF)
val TealContainerLight = Color(0xFFCCFBF1)
val TealOnContainerLight = Color(0xFF134E4A)

val AlabasterBackground = Color(0xFFF8FAFC)
val CrispWhiteSurface = Color(0xFFFFFFFF)
val SlateSurfaceVariant = Color(0xFFF1F5F9)
val SlateInk = Color(0xFF0F172A)
val SlateMuted = Color(0xFF475569)
val SlateOutline = Color(0xFFCBD5E1)
val SlateOutlineVariant = Color(0xFFE2E8F0)

val RoseErrorLight = Color(0xFFE11D48)
val RoseErrorContainerLight = Color(0xFFFFE4E6)
val RoseOnErrorContainerLight = Color(0xFF881337)

// Dark Theme Tokens (Slate-Obsidian & Luminous Emerald)
val EmeraldPrimaryDark = Color(0xFF10B981)
val EmeraldOnPrimaryDark = Color(0xFF022C22)
val EmeraldContainerDark = Color(0xFF064E3B)
val EmeraldOnContainerDark = Color(0xFFA7F3D0)

val SlateSecondaryDark = Color(0xFF94A3B8)
val SlateOnSecondaryDark = Color(0xFF0F172A)
val SlateContainerDark = Color(0xFF1E293B)
val SlateOnContainerDark = Color(0xFFE2E8F0)

val TealTertiaryDark = Color(0xFF2DD4BF)
val TealOnTertiaryDark = Color(0xFF042F2E)
val TealContainerDark = Color(0xFF115E59)
val TealOnContainerDark = Color(0xFF99F6E4)

val ObsidianBackground = Color(0xFF0B1120)
val ObsidianSurface = Color(0xFF111827)
val ObsidianSurfaceVariant = Color(0xFF1E293B)
val CrispOffWhiteInk = Color(0xFFF1F5F9)
val SlateMutedDark = Color(0xFF94A3B8)
val SlateOutlineDark = Color(0xFF334155)
val SlateOutlineVariantDark = Color(0xFF1E293B)

val RoseErrorDark = Color(0xFFFB7185)
val RoseErrorContainerDark = Color(0xFF4C0519)
val RoseOnErrorContainerDark = Color(0xFFFFE4E6)

@Immutable
data class VerdantExtendedColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val cardBorder: Color,
    val chartPalette: List<Color>
)

val LightExtendedColors = VerdantExtendedColors(
    income = Color(0xFF059669),
    incomeContainer = Color(0xFFD1FAE5),
    expense = Color(0xFFE11D48),
    expenseContainer = Color(0xFFFFE4E6),
    warning = Color(0xFFD97706),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF78350F),
    heroStart = Color(0xFF064E3B),
    heroEnd = Color(0xFF059669),
    cardBorder = Color(0xFFE2E8F0),
    chartPalette = listOf(
        Color(0xFF059669),
        Color(0xFF0F172A),
        Color(0xFF0D9488),
        Color(0xFFD97706),
        Color(0xFFE11D48),
        Color(0xFF3B82F6),
        Color(0xFF6366F1),
        Color(0xFF64748B)
    )
)

val DarkExtendedColors = VerdantExtendedColors(
    income = Color(0xFF34D399),
    incomeContainer = Color(0xFF064E3B),
    expense = Color(0xFFFB7185),
    expenseContainer = Color(0xFF4C0519),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF451A03),
    onWarningContainer = Color(0xFFFEF3C7),
    heroStart = Color(0xFF064E3B),
    heroEnd = Color(0xFF047857),
    cardBorder = Color(0xFF1E293B),
    chartPalette = listOf(
        Color(0xFF34D399),
        Color(0xFF38BDF8),
        Color(0xFF2DD4BF),
        Color(0xFFFBBF24),
        Color(0xFFFB7185),
        Color(0xFF818CF8),
        Color(0xFFA78BFA),
        Color(0xFF94A3B8)
    )
)

val LocalVerdantColors = staticCompositionLocalOf { LightExtendedColors }
