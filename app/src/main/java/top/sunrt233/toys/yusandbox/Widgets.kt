package top.sunrt233.toys.yusandbox

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import top.sunrt233.toys.yusandbox.simulate.Simulator
import top.sunrt233.toys.yusandbox.simulate.Vec3
import java.util.Locale

@Composable
fun InfoBox(
    modifier: Modifier = Modifier, simulator: Simulator, duration: Long, anchorPosition: Vec3
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = String.format(
                Locale.ENGLISH, "TPS: %.2f\tFPS: %.2f", simulator.simulationInfo.tps, 1e9 / duration
            ), fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Total Step: ${simulator.simulationInfo.totalStep}",
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Delta: ${simulator.simulationInfo.delta}", fontFamily = FontFamily.Monospace
        )
        Text(
            text = String.format(
                Locale.ENGLISH,
                "Anchor Position:\n\t[x: %.2f\ty: %.2f\tz: %.2f]",
                anchorPosition.x,
                anchorPosition.y,
                anchorPosition.z
            ), fontFamily = FontFamily.Monospace
        )
        Text(text = "Simulator Status: ${simulator.simulationInfo.status.name}\nObjects: ${simulator.simulationInfo.objectsCount}")
    }
}


@Composable
fun LongPressableBtn(
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isEnabled by rememberUpdatedState(newValue = enabled)
    Box(modifier = modifier
        .minimumInteractiveComponentSize()
        .size(40.dp)
        .clip(CircleShape)
        .pointerInput(Unit) {
            detectTapGestures(onPress = {
                val press = PressInteraction.Press(it)
                if (isEnabled) {
                    interactionSource.emit(press)
                    onPressed()
                }
                tryAwaitRelease()
                if (isEnabled) {
                    interactionSource.emit(PressInteraction.Release(press))
                    onReleased()
                }
            })
        }
        .indication(
            interactionSource = interactionSource,
            indication = rememberRipple(bounded = false, radius = 40.dp / 2),
        ), contentAlignment = Alignment.Center) {
        val contentColor =
            if (enabled) LocalContentColor.current else LocalContentColor.current.copy(0.38f)
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}