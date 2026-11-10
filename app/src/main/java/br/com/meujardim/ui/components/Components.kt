package br.com.meujardim.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.meujardim.data.db.Plant
import br.com.meujardim.ui.theme.Garden
import br.com.meujardim.util.relativeLabel
import coil.compose.AsyncImage
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

/** Par de cores (fundo, destaque) escolhido de forma estável a partir do nome. */
fun accentFor(seed: String): Pair<Color, Color> {
    val palette = listOf(
        Garden.GreenLight to Garden.Green,
        Garden.YellowLight to Color(0xFFB8860B),
        Garden.CoralLight to Garden.CoralDark,
        Garden.LilacLight to Garden.LilacDark,
        Garden.SkyLight to Color(0xFF2B7BB5)
    )
    return palette[abs(seed.hashCode()) % palette.size]
}

/** Foto da planta ou, sem foto, uma ilustração de folhas no tom do nome. */
@Composable
fun PlantAvatar(
    photoPath: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    corner: Dp = 18.dp
) {
    val shape = RoundedCornerShape(corner)
    if (photoPath != null && File(photoPath).exists()) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = "Foto de $name",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape)
        )
    } else {
        val (bg, fg) = accentFor(name)
        Box(modifier.size(size).clip(shape).background(bg), contentAlignment = Alignment.Center) {
            LeafIllustration(color = fg, modifier = Modifier.fillMaxSize(0.62f))
        }
    }
}

/** Ilustração simples de muda com duas folhas, desenhada em Canvas. */
@Composable
fun LeafIllustration(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawLeaf(Offset(w * 0.48f, h * 0.78f), w * 0.46f, -38f, color)
        drawLeaf(Offset(w * 0.52f, h * 0.78f), w * 0.52f, 34f, color.copy(alpha = 0.75f))
        drawRect(color, topLeft = Offset(w * 0.47f, h * 0.55f), size = Size(w * 0.06f, h * 0.4f))
    }
}

/** Folha em forma de gota alongada, partindo de [base] e girada em [angle] graus. */
fun DrawScope.drawLeaf(base: Offset, length: Float, angle: Float, color: Color) {
    rotate(angle, pivot = base) {
        val path = Path().apply {
            moveTo(base.x, base.y)
            cubicTo(base.x - length * 0.42f, base.y - length * 0.35f, base.x - length * 0.3f, base.y - length * 0.85f, base.x, base.y - length)
            cubicTo(base.x + length * 0.3f, base.y - length * 0.85f, base.x + length * 0.42f, base.y - length * 0.35f, base.x, base.y)
            close()
        }
        drawPath(path, color)
    }
}

/** Folhas decorativas para cabeçalhos, discretas e no canto. */
@Composable
fun LeafDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(Garden.Yellow.copy(alpha = 0.35f), radius = h * 0.42f, center = Offset(w * 0.86f, h * 0.18f))
        drawLeaf(Offset(w * 0.98f, h * 1.02f), h * 0.95f, -32f, Garden.Leaf.copy(alpha = 0.22f))
        drawLeaf(Offset(w * 0.92f, h * 1.05f), h * 0.7f, -62f, Garden.Green.copy(alpha = 0.18f))
        drawLeaf(Offset(w * 1.02f, h * 0.95f), h * 0.6f, 8f, Garden.Lilac.copy(alpha = 0.3f))
    }
}

@Composable
fun StatCard(
    label: String,
    value: Int,
    icon: ImageVector,
    background: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = accent)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Garden.Ink.copy(alpha = 0.75f))
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Situação da rega: atrasada, hoje ou em N dias. */
@Composable
fun WateringStatusPill(plant: Plant, today: LocalDate = LocalDate.now()) {
    val (bg, fg, text) = when {
        plant.isOverdue(today) -> Triple(Garden.CoralLight, Garden.CoralDark, "Atrasada · ${plant.nextWateringDate.relativeLabel(today)}")
        plant.isDue(today) -> Triple(Garden.YellowLight, Color(0xFF8A6500), "Regar hoje")
        else -> Triple(Garden.GreenLight, Garden.GreenDark, "Próxima ${plant.nextWateringDate.relativeLabel(today)}")
    }
    Pill(text, bg, fg)
}

@Composable
fun Pill(text: String, background: Color, content: Color, icon: ImageVector? = null) {
    Surface(color = background, shape = CircleShape) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = content, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Linha de planta usada no dashboard e nas listas. */
@Composable
fun PlantRow(
    plant: Plant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWater: (() -> Unit)? = null,
    today: LocalDate = LocalDate.now()
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PlantAvatar(plant.photoPath, plant.name, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(plant.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(plant.species, plant.room).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                WateringStatusPill(plant, today)
            }
            if (onWater != null) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onWater, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Rounded.WaterDrop, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reguei")
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(Garden.GreenLight),
            contentAlignment = Alignment.Center
        ) { LeafIllustration(Garden.Green, Modifier.size(56.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}
