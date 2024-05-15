package top.sunrt233.toys.yusandbox.simulate

import android.util.Log
import com.google.common.math.BigDecimalMath
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.math.BigDecimal
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.roundToLong
import kotlin.math.sqrt

class Simulator(private var delta: Float = 0.01f, private val expectedTPS: Double) {
    companion object {
        const val G = 6.67e-11 * 1e12
    }

    private val simObjectStoreProvider = SimObjectStoreProviderImpl(this)

    private val simObjects = ArrayList<SimObject>()
    private val objForceMap = ConcurrentHashMap<SimObject, Vec3>()
    private var totalStep = 0
    private var currentTPS = 0.0
    private var status = SimulatorStatus.IDLE

    // 每tick计算的最大耗时（期望值），单位为纳秒
    private val maxTimePerTick = ((1 / expectedTPS) * 1e9).roundToLong()

    val simulationInfo = SimulationInfo()

    lateinit var executor: ListeningScheduledExecutorService
    private lateinit var simulationFuture: ListenableFuture<*>

    private var SimObject.force: Vec3
        get() {
            if (objForceMap.containsKey(this)) return objForceMap[this]!!
            else {
                val force = Vec3.ZERO
                objForceMap[this] = force
                return force
            }
        }
        set(value) {
            objForceMap[this] = value
        }

    fun addObject(obj: SimObject) {
        if (!simObjects.contains(obj)) simObjects.add(obj)
    }

    fun addObjects(vararg obj: SimObject) {
        simObjects.forEach {
            addObject(it)
        }
    }

    fun start() {
        if (status != SimulatorStatus.IDLE) return
        resume()
    }

    fun stop() {
        if (status == SimulatorStatus.IDLE) return
        if (simulationFuture.isCancelled) return
        simulationFuture.cancel(true)
        status = SimulatorStatus.STOPPED
    }

    fun reset() {
        stop()
        simObjects.clear()
        objForceMap.clear()
        totalStep = 0
        currentTPS = 0.0
        status = SimulatorStatus.IDLE
    }

    fun pause() {
        if (status != SimulatorStatus.RUNNING || simulationFuture.isCancelled) return
        simulationFuture.cancel(true)
        status = SimulatorStatus.PAUSE
    }

    fun resume() {
        if (status == SimulatorStatus.RUNNING) return

        executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor())
        simulationFuture = executor.submit {
            status = SimulatorStatus.RUNNING
            simulate()
        }

        val callback = object : FutureCallback<Any> {
            override fun onSuccess(result: Any?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(t: Throwable) {

                if (t !is CancellationException) Log.e("Simulation", "出现错误，模拟终止", t)
                status = SimulatorStatus.STOPPED
            }
        }
        Futures.addCallback(
            simulationFuture, callback, MoreExecutors.directExecutor()
        )

    }

    fun getSimulateFrame(): List<SimObject> {
        return simObjects
    }

    private fun simulate() {
        var tickStartTime: Long
        var tickEndTime: Long
        var duration: Long

        while (true) {
            tickStartTime = System.nanoTime()

            updateForces()
            updateVelocities()
            updatePositions()

            tickEndTime = System.nanoTime()
            duration = tickEndTime - tickStartTime

            if (duration < maxTimePerTick) {
                Thread.sleep(((maxTimePerTick - duration) / 1e6).roundToLong())
                tickEndTime = System.nanoTime()
                duration = tickEndTime - tickStartTime
            }
            currentTPS = (1.0*1e9 / duration)

            totalStep++
        }
    }

    fun setDelta(newDelta: Float) {
        if (newDelta < 0f) {
            this.delta = 0f
        } else {
            this.delta = newDelta
        }
    }

    private fun calForce(obj1: SimObject, obj2: SimObject): Vec3 {
        val r = (obj2.position - obj1.position)
        val norm = r.norm
        if (norm < 4f) return Vec3.ZERO
        return r.normalized * ((G * obj1.mass * obj2.mass / (norm * norm)).toFloat())
    }

    private fun updateForces() {
        simObjects.forEach { obj ->
            obj.force = Vec3(0f, 0f, 0f)
            simObjects.forEach { other ->
                if (obj != other) obj.force += calForce(obj, other)
            }
        }
    }

    private fun updateVelocities() {
        simObjects.forEach { obj ->
            obj.velocity += obj.force * (1 / obj.mass) * delta
        }
    }

    private fun updatePositions() {
        simObjects.forEach { obj ->
            obj.position += obj.velocity * delta
        }
    }

    inner class SimulationInfo {
        val tps: Double
            get() = currentTPS
        val status: SimulatorStatus
            get() = this@Simulator.status
        val totalStep: Int
            get() = this@Simulator.totalStep
        val delta: Float
            get() = this@Simulator.delta
        val objectsCount: Int
            get() = this@Simulator.simObjects.size
    }

    enum class SimulatorStatus {
        IDLE, RUNNING, PAUSE, STOPPED,
    }
}

open class SimObject(var mass: Float, var position: Vec3, var velocity: Vec3) {
    companion object {
        val VOID = SimObject(Float.NaN, Vec3.ZERO, Vec3.ZERO)
    }
}

class Vec3(val x: Float, val y: Float, val z: Float) {
    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }

    val normalized: Vec3
        get() {
            return this * (1f / norm)
        }

    val norm: Float
        get() {
            return sqrt(x * x + y * y + z * z)
        }

    operator fun plus(other: Vec3): Vec3 {
        return Vec3(this.x + other.x, this.y + other.y, this.z + other.z)
    }

    operator fun minus(other: Vec3): Vec3 {
        return Vec3(this.x - other.x, this.y - other.y, this.z - other.z)
    }

    operator fun times(scale: Float): Vec3 {
        return Vec3(this.x * scale, this.y * scale, this.z * scale)
    }

    override fun toString(): String {
        return "[x: $x, y: $y, z: $z]"
    }

}