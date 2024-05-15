package top.sunrt233.toys.yusandbox.simulate

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

interface SimObjectStoreProvider<T : SimObject> : Iterable<T> {
    /**
     * @param simObject 待添加的物体
     * @return 添加物体后，返回物体在模拟世界中的唯一编号
     */
    fun addObject(simObject: T): Int

    /**
     * @param id 物体在模拟世界中的唯一ID
     * @return 从世界移除的物体
     */
    fun removeObject(id: Int): T
    fun getObject(id: Int): T
    fun size(): Int
    fun clearAll()

    operator fun SimObjectStoreProvider<T>.get(id: Int): T
    operator fun SimObjectStoreProvider<T>.contains(simObject: T): Boolean

}

class SimObjectStoreProviderImpl(simulator: Simulator) : SimObjectStoreProvider<SimObject> {

    companion object {
        const val INVALID_ID = -1
    }

    private val nextIdAtomic = AtomicInteger(0)
    private val nextId: Int
        get() = nextIdAtomic.getAndIncrement()

    private val idObjectMap = ConcurrentHashMap<Int, SimObject>()
    private val objectIdMap = ConcurrentHashMap<SimObject, Int>()


    private val SimObject.idInner: Int
        get() = objectIdMap.getOrDefault(this, INVALID_ID)

    override fun addObject(simObject: SimObject): Int {

        if (idObjectMap.containsValue(simObject)) return simObject.idInner
        else return nextId.apply {
            objectIdMap[simObject] = this
            idObjectMap[this] = simObject
        }
    }

    override fun removeObject(id: Int): SimObject {
        val obj = idObjectMap.remove(id)
        if (obj != null) {
            objectIdMap.remove(obj)
            return obj
        } else return SimObject.VOID
    }

    override fun getObject(id: Int): SimObject {
        return idObjectMap.getOrDefault(id, SimObject.VOID)
    }

    override fun size(): Int {
        return idObjectMap.size
    }

    override fun clearAll() {
        objectIdMap.clear()
        idObjectMap.clear()
    }

    override fun SimObjectStoreProvider<SimObject>.contains(simObject: SimObject): Boolean {
        return idObjectMap.containsValue(simObject)
    }

    override fun SimObjectStoreProvider<SimObject>.get(id: Int): SimObject {
        return idObjectMap.getOrDefault(id, SimObject.VOID)
    }

    override fun iterator(): Iterator<SimObject> {
        return idObjectMap.values.iterator()
    }

}


