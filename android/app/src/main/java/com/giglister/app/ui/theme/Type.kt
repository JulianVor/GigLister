package com.giglister.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.giglister.app.R

private val display = FontFamily(Font(R.font.archivo_black))
private val meta = FontFamily(Font(R.font.barlow_condensed))
val GigTypography = Typography(
    displayMedium = TextStyle(fontFamily = display, fontSize = 42.sp, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontFamily = display, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = display, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = display, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = display, fontSize = 19.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = meta, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = .4.sp),
    labelMedium = TextStyle(fontFamily = meta, fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = meta, fontSize = 12.sp, lineHeight = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp)
)
