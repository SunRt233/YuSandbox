package top.sunrt233.toys.yusandbox

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.sunrt233.toys.yusandbox.renderer.SimCanvas
import top.sunrt233.toys.yusandbox.renderer.SimCanvasState
import top.sunrt233.toys.yusandbox.renderer.color
import top.sunrt233.toys.yusandbox.renderer.rememberSimCanvasDragState
import top.sunrt233.toys.yusandbox.simulate.SimObject
import top.sunrt233.toys.yusandbox.simulate.Simulator
import top.sunrt233.toys.yusandbox.simulate.Vec3
import top.sunrt233.toys.yusandbox.ui.theme.GravitySimulatorTheme
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    val colorMap = HashMap<SimObject, Color>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            window.decorView.apply {
                systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
            val simulator by remember {
                mutableStateOf(Simulator(delta = 0.01f, expectedTPS = 100.0))
            }.apply {
                initSimulationObjects(this.value)
            }
            GravitySimulatorTheme {
                Main(simulator)
            }
        }

    }



    @Composable
    fun Main(simulator: Simulator) {
        val delay: Long = 16
        val simCanvasState = rememberSimCanvasDragState()

        Surface(modifier = Modifier.fillMaxSize()) {
            var simulationFrame by remember {
                mutableStateOf(simulator.getSimulateFrame())
            }
            var frames by remember {
                mutableStateOf(0)
            }

            LaunchedEffect(key1 = Unit) {
                CoroutineScope(Dispatchers.Default).launch {
                    var lastSteps = 0
                    while (true) {
                        val currentSteps = simulator.simulationInfo.totalStep
                        if (currentSteps != lastSteps) {
                            simulationFrame = simulator.getSimulateFrame()
                            frames++
                            lastSteps = currentSteps
                            delay(delay)
                        }
                    }
                }
            }

            Log.d("Drag", "${simCanvasState.dragOffset}")

            ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                val (canvas, simInfoBox, bottomBar) = createRefs()
                val startTime = System.nanoTime()
                SimCanvas(
                    modifier = Modifier.constrainAs(canvas) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                    simulationFrame = simulationFrame,
                    frames = frames,
                    simCanvasState = simCanvasState
                )
                val endTime = System.nanoTime()
                InfoBox(modifier = Modifier.constrainAs(simInfoBox) {
                    top.linkTo(parent.top, 16.dp)
                    start.linkTo(parent.start, 16.dp)
                }, simulator = simulator, duration = endTime - startTime,simCanvasState.anchorPositon)

                BottomBar(modifier = Modifier.constrainAs(bottomBar) {
                    bottom.linkTo(parent.bottom, 16.dp)
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    width = Dimension.fillToConstraints
                }, simulator = simulator, simCanvasState = simCanvasState)

            }
        }
    }



    @Composable
    fun BottomBar(modifier: Modifier, simulator: Simulator, simCanvasState: SimCanvasState) {
        var isSpeedUp = false
        var isSlowDown = false

        LaunchedEffect("Unit") {
            CoroutineScope(Dispatchers.Default).launch {
                var t = 0f
                while (true) {
                    if (isSpeedUp) {
                        t += 0.01f
                        simulator.setDelta(simulator.simulationInfo.delta + 0.05f * (1f - exp(-t)))
                        delay(10)
                    } else if (isSlowDown) {
                        t += 0.01f
                        simulator.setDelta(simulator.simulationInfo.delta - 0.05f * (1f - exp(-t)))
                        delay(10)
                    } else {
                        t = 0f
                    }
                }
            }
        }

        ConstraintLayout(modifier = modifier) {
            val (start, middle, end) = createRefs()
            Card(modifier = Modifier.constrainAs(start) {
                this.start.linkTo(parent.start)
                this.bottom.linkTo(parent.bottom)
            }, elevation = CardDefaults.cardElevation(4.dp)) {
                val btnModifier = Modifier.size(48.dp)

                Row() {
                    IconButton(modifier = Modifier.then(btnModifier), onClick = { /*TODO*/ }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                    }
                    if (simulator.simulationInfo.status == Simulator.SimulatorStatus.RUNNING) {
                        IconButton(modifier = Modifier.then(btnModifier), onClick = {
                            simulator.pause()
                        }) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = "暂停模拟")
                        }
                    } else {
                        IconButton(modifier = Modifier.then(btnModifier), onClick = {
                            simulator.resume()
                        }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "继续模拟"
                            )
                        }
                    }

                    LongPressableBtn(modifier = Modifier.then(btnModifier),
                        enabled = simulator.simulationInfo.status == Simulator.SimulatorStatus.RUNNING,
                        onReleased = {
                            isSlowDown = false
                        },
                        onPressed = {
                            isSlowDown = true
                        }) {
                        Icon(imageVector = Icons.Default.FastRewind, contentDescription = null)
                    }
                    LongPressableBtn(modifier = Modifier.then(btnModifier),
                        enabled = simulator.simulationInfo.status == Simulator.SimulatorStatus.RUNNING,
                        onReleased = {
                            isSpeedUp = false
                        },
                        onPressed = {
                            isSpeedUp = true
                        }) {
                        Icon(imageVector = Icons.Default.FastForward, contentDescription = null)
                    }

                    IconButton(onClick = {
                        CoroutineScope(Dispatchers.Default).launch {
                            simulator.reset()
                            simulator.setDelta(0.01f)
                            initSimulationObjects(simulator)
                        }
                        simCanvasState.dragOffset = Offset.Zero
                        simCanvasState.shouldFollowSelectedObj = false
                        simCanvasState.hasSelectedObj = false
                    }) {
                        Icon(imageVector = Icons.Filled.Restore, contentDescription = null)
                    }
                }
            }

            Card(modifier = Modifier.constrainAs(end) {
                this.end.linkTo(parent.end)
                this.bottom.linkTo(parent.bottom)
            }, elevation = CardDefaults.cardElevation(4.dp)) {
                Column(verticalArrangement = Arrangement.Bottom) {
                    IconButton(onClick = {
                        CoroutineScope(Dispatchers.Default).launch {
                            simulator.addObject(generateRandomObject(
                                20f, x = 400f, y = 400f, vX = 2f, vY = 2f
                            ).apply {
                                this.color = Color(
                                    red = Random.nextFloat(),
                                    green = Random.nextFloat(),
                                    blue = Random.nextFloat()
                                )
                            })
                        }
                    }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    }

                    IconButton(onClick = {
                        simCanvasState.shouldFollowSelectedObj = true
                    }, enabled = simCanvasState.hasSelectedObj) {
                        Icon(imageVector = Icons.Filled.Fullscreen, contentDescription = null)
                    }
                }
            }
        }
    }

    private fun generateRandomObject(
        mass: Float, x: Float, y: Float, z: Float = 0f, vX: Float, vY: Float, vZ: Float = 0f
    ): SimObject {
        return SimObject(
            Random.nextFloat() * mass, Vec3(
                -x / 2 + x * Random.nextFloat(),
                -y / 2 + y * Random.nextFloat(),
                -z / 2 + z * Random.nextFloat()
            ), Vec3(
                -vX / 2 + vX * Random.nextFloat(),
                -vY / 2 + vY * Random.nextFloat(),
                -vZ / 2 + vZ * Random.nextFloat()
            )
        )
    }

    private fun initSimulationObjects(simulator: Simulator) {
        simulator.addObject(
            SimObject(
            mass = 100f, position = Vec3(0f, 100f, 0f), velocity = Vec3(
                1f, 0f, 0f
            ) * (sqrt(Simulator.G * 100.0 / 100.0) / 2).toFloat()
        ).apply {
            this.color = Color(
                red = Random.nextFloat(), green = Random.nextFloat(), blue = Random.nextFloat()
            )
        })
        simulator.addObject(
            SimObject(
            mass = 100f, position = Vec3(0f, -100f, 0f), velocity = Vec3(
                1f, 0f, 0f
            ) * (-sqrt(Simulator.G * 100.0 / 100.0) / 2).toFloat()
        ).apply {
            this.color = Color(
                red = Random.nextFloat(), green = Random.nextFloat(), blue = Random.nextFloat()
            )
        })
        simulator.addObject(
            SimObject(
            mass = 10f + (-8f + 18 * Random.nextFloat()),
            position = Vec3(-400f, -160f, 0f),
            velocity = Vec3(1.2f, 1f, 0f) * 2f + Vec3(
                -1f + 2 * Random.nextFloat(), -1f + 2 * Random.nextFloat(), 0f
            )
        ).apply {
            this.color = Color(
                red = Random.nextFloat(), green = Random.nextFloat(), blue = Random.nextFloat()
            )
        })
    }

}

