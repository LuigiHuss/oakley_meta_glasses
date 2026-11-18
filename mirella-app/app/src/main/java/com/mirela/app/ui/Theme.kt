package com.mirela.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mirela.app.R

/** Paleta Aurora Líquida: preto fosco, prata, magenta, violeta e azul-noite. */
object M {
  val Bg = Color(0xFF060609)
  val Text = Color(0xFFECECF2)
  val Muted = Color(0xFFA6A6B4)
  val Dim = Color(0xFF7E7E8C)
  val Pink = Color(0xFFF06BB8)
  val Magenta = Color(0xFFC23E9A)
  val Violet = Color(0xFF7A2FD0)
  val VioletLight = Color(0xFF7B46F5)
  val Deep = Color(0xFF2A0F4A)
  val Night = Color(0xFF160B3A)
  val Blue = Color(0xFF3C50DC)
  val Green = Color(0xFF8FF0C6)
  val Coral = Color(0xFFFF7A9C)
  val Lilac = Color(0xFFE6DCFF)
}

val Jura = FontFamily(Font(R.font.jura_light, FontWeight.Light), Font(R.font.jura_medium, FontWeight.Medium))
val Outfit = FontFamily(Font(R.font.outfit_regular, FontWeight.Normal))

val MagBrush = Brush.linearGradient(listOf(M.Magenta, M.Violet, M.Deep))
val VioBrush = Brush.linearGradient(listOf(M.VioletLight, Color(0xFF4B1FB0), M.Night))
val BluBrush = Brush.linearGradient(listOf(M.Blue.copy(alpha = 0.6f), Color(0xFF14194A)))
val SphereBrushColors = listOf(M.Pink, Color(0xFF8A3BE0), Color(0xFF25104F))

@Composable
fun MirelaTheme(content: @Composable () -> Unit) {
  MaterialTheme(
      colorScheme =
          darkColorScheme(
              background = M.Bg,
              surface = M.Bg,
              primary = M.Pink,
              onPrimary = Color.White,
              onBackground = M.Text,
              onSurface = M.Text,
          ),
      content = content,
  )
}
