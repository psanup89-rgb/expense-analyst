package com.expenseanalyst.core.theme

import androidx.compose.ui.graphics.Color

// ── Ledger palette ─────────────────────────────────────────────────────────────
// Warm black and ivory carry the interface; lime survives as the single brand accent.
// Source of truth: the "Ledger — final" design canvas, board 4 (Colour & type).

// Dark (default)
val LedgerGround = Color(0xFF11100E)          // page background
val LedgerSurface = Color(0xFF161512)         // sheets, icon background
val LedgerPanel = Color(0xFF191815)           // cards
val LedgerPanelHigh = Color(0xFF1F1E1A)
val LedgerPanelHighest = Color(0xFF26241F)
val LedgerLine = Color(0xFF2E2C27)            // card borders
val LedgerHairline = Color(0xFF45423A)        // badge rings, dividers
val LedgerIvory = Color(0xFFEEE9DF)           // primary text, glyphs
val LedgerMuted = Color(0xFFA39E93)           // secondary text
val LedgerAccent = Color(0xFFD6F25C)          // brand, actions, newest entry
val LedgerOnAccent = Color(0xFF1A2200)
val LedgerAccentContainer = Color(0xFF2E3510)

val LedgerSpend = Color(0xFFE8866B)           // money out (terracotta)
val LedgerReceived = Color(0xFFB9D88A)        // money in (sage)
val LedgerReview = Color(0xFFE2B764)          // needs attention (amber)
val LedgerPayment = Color(0xFFA99AC0)         // card / bill settlement (muted violet)
val LedgerTransfer = Color(0xFF9FA8AE)        // own-account transfer (slate)

// Light ("paper")
val PaperGround = Color(0xFFF6F2EA)
val PaperSurface = Color(0xFFFBF8F2)
val PaperPanel = Color(0xFFFFFCF6)
val PaperPanelHigh = Color(0xFFF0EBE1)
val PaperPanelHighest = Color(0xFFE8E2D6)
val PaperLine = Color(0xFFE2DCCF)
val PaperHairline = Color(0xFFCFC7B7)
val PaperInk = Color(0xFF1B1A17)
val PaperMuted = Color(0xFF6B665C)
val PaperAccent = Color(0xFF5B7300)           // lime darkened to pass contrast on paper
val PaperOnAccent = Color(0xFFFFFFFF)
val PaperAccentContainer = Color(0xFFE6F2B8)

val PaperSpend = Color(0xFFB0472C)
val PaperReceived = Color(0xFF4E7A2E)
val PaperReview = Color(0xFF9A6A0E)
val PaperPayment = Color(0xFF6A5A8E)
val PaperTransfer = Color(0xFF5C666C)
