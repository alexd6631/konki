package io.monkeypatch.kollections.vector

import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UNCHECKED_CAST")
abstract class BaseVector<out T> : Collection<T> {
    protected abstract fun arrayFor(i: Int): Array<Any?>

    operator fun get(i: Int): T =
        if (i in 0 until size) arrayFor(i)[i.indexAtLeaf()] as T
        else throw IndexOutOfBoundsException()

    fun getOrNull(i: Int): T? =
        if (i in 0 until size) arrayFor(i)[i.indexAtLeaf()] as T
        else null

    fun first(): T = this[0]

    fun last(): T = this[size - 1]

    fun rangedSequence(start: Int = 0, end: Int = size) = Sequence { rangedIterator(start, end) }

    fun rangedIterator(start: Int = 0, end: Int = size): Iterator<T> =
        object : Iterator<T> {
            var i = start
            var base = i - i % 32
            var array = arrayFor(i)

            override fun hasNext(): Boolean = i < end

            override fun next(): T {
                if (i - base == 32) {
                    array = arrayFor(i)
                    base += 32
                }
                return (array[i.indexAtLeaf()] as T).also { i += 1 }
            }
        }

    override fun iterator(): Iterator<T> = rangedIterator()

    override fun equals(other: Any?): Boolean {
        if (other is Collection<*>) {
            if (size != other.size) return false
            val otherIterator = other.iterator()
            forEach {
                if (it != otherIterator.next()) return false
            }
            return true
        }
        return super.equals(other)
    }

    override fun hashCode(): Int =
        fold(1) { hash, t -> 31 * hash + (t?.hashCode() ?: 0) }

    override fun contains(element: @UnsafeVariance T): Boolean {
        forEach { if (it == element) return true }
        return false
    }

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean =
        elements.all { it in this }

    override fun isEmpty(): Boolean = size == 0
}

internal fun newPath(edit: AtomicBoolean, shift: Int, node: Node): Node =
    if (shift == 0) node
    else Node(edit).apply {
        data[0] = newPath(edit, shift - 5, node)
    }

internal fun <T> copyPath(i: Int, level: Int, node: Node, elem: T): Node =
    Node(node.edit, node.data.withCopy { newData ->
        if (level == 0) {
            newData[i.indexAtLeaf()] = elem
        } else {
            val subIndex = i.indexAtLevel(level)
            newData[subIndex] = copyPath(i, level - 5, node.subNode(subIndex), elem)
        }
    })

internal fun Int.indexAtLeaf() = this and 0x01f

internal fun Int.indexAtLevel(level: Int) = (this ushr level) and 0x01f

internal inline fun <T> Array<T>.withCopy(block: (Array<T>) -> Unit): Array<T> = copyOf().also(block)

internal class Node(
    val edit: AtomicBoolean,
    val data: Array<Any?>
) {
    constructor(edit: AtomicBoolean) : this(edit, arrayOfNulls(32))

    fun subNode(i: Int) = data[i] as Node
    fun subNodeOrNull(i: Int) = data[i] as Node?
}

internal typealias TVect<T> = TVector<@UnsafeVariance T>
internal typealias PVect<T> = PVector<@UnsafeVariance T>
