package com.mirela.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Superfície de vidro: leve gradiente branco, borda fina e brilho no topo. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)): Modifier =
    this.clip(shape)
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f))))
        .border(1.dp, Color.White.copy(alpha = 0.12f), shape)

@Composable
fun T(
    text: String,
    size: TextUnit = 14.sp,
    color: Color = M.Text,
    family: FontFamily = Outfit,
    weight: FontWeight = FontWeight.Normal,
    spacing: TextUnit = 0.sp,
    align: TextAlign? = null,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
  Text(
      text = text,
      fontSize = size,
      color = color,
      fontFamily = family,
      fontWeight = weight,
      letterSpacing = spacing,
      textAlign = align,
      lineHeight = lineHeight,
      modifier = modifier,
  )
}

@Composable
fun SectionLabel(text: String) = T(text.uppercase(), size = 11.sp, color = M.Muted, family = Jura, weight = FontWeight.Medium, spacing = 2.5.sp)

@Composable
fun GradientCard(
    brush: Brush,
    modifier: Modifier = Modifier,
    radius: Dp = 30.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(radius)
  Column(
      modifier =
          modifier
              .shadow(24.dp, shape, ambientColor = M.Violet, spotColor = M.Magenta)
              .clip(shape)
              .background(brush)
              .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
              .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
              .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      content = content,
  )
}

/** Botão em pílula: branco brilhante (principal) ou vidro (secundário). */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) {
  val base = modifier.height(54.dp)
  val styled =
      if (primary)
          base.clip(CircleShape)
              .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD9D9E3))))
      else base.glass(CircleShape)
  Box(
      modifier = styled.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp),
      contentAlignment = Alignment.Center,
  ) {
    T(
        text,
        size = 15.sp,
        family = Jura,
        weight = FontWeight.Medium,
        color = if (primary) Color(0xFF141418) else M.Text,
    )
  }
}

/** Seletor Pessoal / Profissional. */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
  Row(modifier = Modifier.fillMaxWidth().glass(CircleShape).padding(5.dp)) {
    options.forEach { (label, value) ->
      val on = value == selected
      Box(
          modifier =
              Modifier.weight(1f)
                  .height(42.dp)
                  .clip(CircleShape)
                  .then(
                      if (on) Modifier.background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD9D9E3))))
                      else Modifier)
                  .clickable { onSelect(value) },
          contentAlignment = Alignment.Center,
      ) {
        T(label, size = 14.sp, family = Jura, weight = FontWeight.Medium, color = if (on) Color(0xFF141418) else M.Muted)
      }
    }
  }
}

@Composable
fun StatusDot(color: Color) =
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))

@Composable
fun Banner(text: String, error: Boolean) {
  if (text.isBlank()) return
  Box(
      Modifier.fillMaxWidth()
          .glass(RoundedCornerShape(18.dp))
          .background(if (error) M.Coral.copy(alpha = 0.10f) else Color.Transparent)
          .padding(horizontal = 16.dp, vertical = 12.dp)) {
        T(text, size = 13.sp, color = if (error) Color(0xFFFFD0DD) else M.Lilac, lineHeight = 18.sp)
      }
}
