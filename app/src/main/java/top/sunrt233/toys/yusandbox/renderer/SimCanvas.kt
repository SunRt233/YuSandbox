package top.sunrt233.toys.yusandbox.renderer

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import top.sunrt233.toys.yusandbox.simulate.SimObject
import top.sunrt233.toys.yusandbox.simulate.Vec3
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun drawObject() {

}

fun drawHistory() {

}

class SimCanvasState(initOffset: Offset) {
    var dragOffset by mutableStateOf(initOffset)
    var hasSelectedObj by mutableStateOf(false)
    var shouldFollowSelectedObj by mutableStateOf(false)
    var selectPointOffset by mutableStateOf(Offset.Zero)
    var anchorPositon by mutableStateOf(Vec3.ZERO)
//        var viewAnchorOffset by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberSimCanvasDragState(offset: Offset = Offset.Zero): SimCanvasState {
    val offsetState = rememberUpdatedState(newValue = offset)
    return remember {
        SimCanvasState(offsetState.value)
    }
}

class SimCanvasScope:Density {

}

@Composable
fun SimCanvas(
    modifier: Modifier,
    simulationFrame: List<SimObject>,
    frames: Int,
    maxHistory: Int = 1000,
    simCanvasState: SimCanvasState = rememberSimCanvasDragState(offset = Offset.Zero)
) {
    val history by remember {
        mutableStateOf(HashMap<SimObject, ArrayDeque<Pair<Offset, Int>>>())
    }
    var selectedObj by remember {
        mutableStateOf(SimObject.VOID)
    }

    var selectedUndone by remember {
        mutableStateOf(false)
    }
    val boardBuf = 24.dp

    Box(modifier = modifier
        .pointerInput(Unit) {
            detectDragGestures(onDrag = { change, dragAmount ->
                simCanvasState.dragOffset += dragAmount
            })

        }
        .pointerInput(Unit) {
            detectTapGestures(onPress = {
                Log.d("SimCanvas", it.toString())
                simCanvasState.selectPointOffset = it
                selectedUndone = true
                tryAwaitRelease()
            })
        }) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val anchorOffset: Offset = if(simCanvasState.hasSelectedObj && simCanvasState.shouldFollowSelectedObj) {
                simCanvasState.anchorPositon = selectedObj.position
                Offset(
                    selectedObj.position.x.dp.toPx(), -selectedObj.position.y.dp.toPx()
                ) * 0.75f
            } else {
                simCanvasState.anchorPositon = Vec3.ZERO
                simCanvasState.shouldFollowSelectedObj = false
                Offset.Zero
            }

            simulationFrame.forEach { simObj ->

                // 绘制历史路径
                if (!history.containsKey(simObj)) {
                    // 分配历史路径队列
                    history[simObj] = ArrayDeque(maxHistory)
                } else {
                    drawPoints(
                        points = history[simObj]!!.map { this.center + it.first + simCanvasState.dragOffset - anchorOffset },
                        pointMode = PointMode.Polygon,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Butt,
                        color = simObj.color,
                        alpha = 0.75f,
                    )
                }

                // 原始的物体位置偏移量
                val offset = Offset(
                    simObj.position.x.dp.toPx(), -simObj.position.y.dp.toPx()
                ) * 0.75f
                // 变换到绘图空间的物体位置偏移量

                val translatedOffset =
                    this.center + offset + simCanvasState.dragOffset - anchorOffset
                // 根据质量生成绘制半径
                val r = (min(max(simObj.mass, 2f), 20f)).dp.toPx() * 0.75f
//                    Log.d("SimCanvas","offset: $offset translatedOffset: $translatedOffset")
//                    Log.d("SimCanvas","size ${this.size.width} ${this.size.height}")
//                    val xInRange = this.size.width + boardBuf.toPx() >= translatedOffset.x && translatedOffset.x >= 0 - boardBuf.toPx()
//                    val yInRange = this.size.height + boardBuf.toPx() >= translatedOffset.y && translatedOffset.y >= 0 - boardBuf.toPx()
//                    if (xInRange && yInRange) {
                // 变换后位置偏移量与选中位置偏移量的差
                val d = translatedOffset - simCanvasState.selectPointOffset

                // 若物体选择未结束，判断此物体是否应被选中
                if (selectedUndone && sqrt(d.x * d.x + d.y * d.y) <= r + 12.dp.toPx()) {
                    selectedObj = simObj
                    selectedUndone = false
                    simCanvasState.hasSelectedObj = true
                }

                // 绘制物体（质点）
                drawCircle(
                    color = simObj.color, radius = r, center = translatedOffset
                )
//                    } else {
//                        Log.d("SimCanvas","超出屏幕")
//                    }

                // 绘制选中边框
                if (selectedObj == simObj) {
                    drawRect(
                        color = simObj.color,
                        topLeft = translatedOffset - Offset(r + 4.dp.toPx(), r + 4.dp.toPx()),
                        size = Size(2 * (r + 4.dp.toPx()), 2 * (r + 4.dp.toPx())),
                        style = Stroke(2.dp.toPx())
                    )
                }

                // 更新历史路径信息
                history[simObj]?.let {
                    if (it.size >= maxHistory || (it.isNotEmpty() && frames - it.first().second >= 60 * 4)) {
                        it.removeFirst()
                    }
                }
                history[simObj]?.addLast(Pair(offset, frames))

            }
            // 遍历完一个物理模拟帧的全部物体后结束物体选择，无论是否有物体被选中
            selectedUndone = false
            simCanvasState.selectPointOffset = Offset.Zero
        }

    }

}

var SimObject.color: Color
    get() = colorMap.getOrDefault(this, Color.Red)
    set(value) {
        colorMap[this] = value
    }
