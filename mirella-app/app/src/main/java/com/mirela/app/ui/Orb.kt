package com.mirela.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.mirela.app.Phase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Esfera da Mirella: anel graduado girando, esfera de vidro e onda que reage à voz. */
@Composable
fun MirelaOrb(phase: Phase, level: Float, modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "orb")
  val ring by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "ring")
  val ringFast by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "ringFast")
  val wave by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "wave")
  val waveFast by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(800, easing = LinearEasing)), label = "waveFast")
  val breathe by transition.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "breathe")

  val targetAmp =
      when (phase) {
        Phase.LISTENING -> 0.35f + level * 0.65f
        Phase.SPEAKING -> 0.85f
        Phase.PROCESSING -> 0.25f
        Phase.CAPTURING -> 0.45f
        Phase.ERROR -> 0.08f
        Phase.IDLE -> 0.15f
      }
  val amp by animateFloatAsState(targetAmp, tween(220), label = "amp")
  val busy = phase == Phase.PROCESSING || phase == Phase.CAPTURING
  val ringAngle = if (busy) ringFast else ring
  val wavePhase = if (busy) waveFast else wave

  Canvas(modifier) {
    val c = center
    val r = size.minDimension / 2f

    rotate(ringAngle, c) {
      for (i in 0 until 72) {
        val a = Math.toRadians(i * 5.0)
        val r1 = r * 0.90f
        val r2 = if (i % 6 == 0) r * 0.99f else r * 0.95f
        drawLine(
            color = Color.White.copy(alpha = 0.38f),
            start = Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
            end = Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
        )
      }
      drawCircle(Color.White, radius = 7f, center = Offset(c.x + r * 0.82f * cos(-0.9).toFloat(), c.y + r * 0.82f * sin(-0.9).toFloat()))
    }
    drawCircle(Color.White.copy(alpha = 0.12f), radius = r * 0.84f, center = c, style = Stroke(1.5f))

    val orbR = r * 0.64f * breathe
    drawCircle(M.Pink.copy(alpha = 0.25f), radius = orbR * 1.12f, center = c)
    drawCircle(
        Brush.radialGradient(SphereBrushColors, center = Offset(c.x, c.y + orbR * 0.2f), radius = orbR * 1.15f),
        radius = orbR,
        center = c,
    )
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = Offset(c.x - orbR * 0.3f, c.y - orbR * 0.45f), radius = orbR * 0.7f),
        radius = orbR,
        center = c,
    )

    fun wavePath(height: Float, shift: Float): Path {
      val p = Path()
      val width = orbR * 1.5f
      val x0 = c.x - width / 2f
      for (k in 0..96) {
        val t = k / 96f
        val envelope = sin(PI * t).toFloat()
        val y = c.y + height * envelope * sin(shift + t * 4f * PI.toFloat())
        val x = x0 + width * t
        if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
      }
      return p
    }
    drawPath(wavePath(orbR * 0.42f * amp, wavePhase), Color.White, style = Stroke(5f, cap = StrokeCap.Round))
    drawPath(wavePath(orbR * 0.26f * amp, wavePhase + 1.3f), Color.White.copy(alpha = 0.55f), style = Stroke(3f, cap = StrokeCap.Round))
  }
}
