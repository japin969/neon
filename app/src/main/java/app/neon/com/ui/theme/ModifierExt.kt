package app.neon.com.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncyClickScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(enabled = enabled) {
            pressed = true
            onClick()
            pressed = false
        }
}
