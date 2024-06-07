package dev.turtle.turtlelib.util

/**
* Case insensitive mutable map.
* */
class CIMutableMap<V>: MutableMap<String, V> {
    private val map = mutableMapOf<String, V>()

    override val entries: MutableSet<MutableMap.MutableEntry<String, V>>
        get() = map.entries
    override val keys: MutableSet<String>
        get() = map.keys
    override val size: Int
        get() = map.size
    override val values: MutableCollection<V>
        get() = map.values

    override fun containsKey(key: String) = map.containsKey(key.uppercase())
    override fun containsValue(value: V) = map.containsValue(value)
    override fun get(key: String) = map[key.uppercase()]
    override fun isEmpty() = map.isEmpty()

    override fun clear() {
        map.clear()
    }

    override fun put(key: String, value: V): V? {
        return map.put(key.uppercase(), value)
    }

    override fun putAll(from: Map<out String, V>) {
        from.forEach { (key, value) -> map[key.uppercase()] = value }
    }

    override fun remove(key: String): V? {
        return map.remove(key.uppercase())
    }
}