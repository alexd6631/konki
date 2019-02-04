package io.monkeypatch.kollections

import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Kotlin port of PersistentVector based on Clojure implementation
 *
 * https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/PersistentVector.java
 */
@Suppress("UNCHECKED_CAST")
data class PVector<out T>(
    val size: Int,
    private val shift: Int,
    private val root: Node,
    private val tail: Array<Any?>
) {
    private val tailOffset
        get() = if (size < 32) 0 else (size - 1).ushr(5).shl(5)

    operator fun plus(elem: @UnsafeVariance T): PVector<T> =
        if (size - tailOffset < 32) {
            val newTail = tail + (elem as Any?)
            PVector(size + 1, shift, root, newTail)
        } else {
            val newTail = arrayOf(elem as Any?)
            val tailNode = Node(root.edit, tail)
            if (fullCapacityReached()) {
                val newRoot = Node(root.edit).apply {
                    data[0] = root
                    data[1] = newPath(root.edit, shift, tailNode)
                }
                PVector(size + 1, shift + 5, newRoot, newTail)
            } else {
                val newRoot = pushTail(shift, root, tailNode)
                PVector(size + 1, shift, newRoot, newTail)
            }
        }

    private fun fullCapacityReached() = (size ushr 5) > (1 shl shift)

    private fun pushTail(level: Int, parent: Node, tailNode: Node): Node =
        Node(parent.edit, parent.data.withCopy { newData ->
            val subIndex = (size - 1).indexAtLevel(level)

            newData[subIndex] = if (level == 5) tailNode else {
                val child = parent.subNodeOrNull(subIndex)
                child?.let { pushTail(level - 5, it, tailNode) }
                    ?: newPath(root.edit, level - 5, tailNode)
            }
        })

    operator fun get(i: Int): T =
        if (i in 0 until size) arrayFor(i)[i.indexAtLeaf()] as T
        else throw IndexOutOfBoundsException()

    fun getOrNull(i: Int): T? =
        if (i in 0 until size) arrayFor(i)[i.indexAtLeaf()] as T
        else null

    private fun arrayFor(i: Int): Array<Any?> =
        if (i >= tailOffset) tail
        else {
            var node = root
            var level = shift
            while (level > 0) {
                node = node.data[i.indexAtLevel(level)] as Node
                level -= 5
            }
            node.data
        }

    fun update(i: Int, elem: @UnsafeVariance T): PVector<T> =
        if (i in 0 until size) {
            if (i >= tailOffset) {
                val newTail = tail.withCopy {
                    it[i.indexAtLeaf()] = elem as Any?
                }
                PVector(size, shift, root, newTail)
            } else {
                PVector(size, shift, copyPath(i, shift, root, elem), tail)
            }
        } else throw IndexOutOfBoundsException()

    fun asSequence(start: Int = 0, end: Int = size) = Sequence {
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
    }

    fun <R> fold(initial: R, f: (R, T) -> R): R {
        var i = 0
        var acc = initial
        while (i < size) {
            val array = arrayFor(i)
            for (e in array) {
                acc = f(acc, e as T)
            }
            i += array.size
        }
        return acc
    }

    fun pop(): PVector<T> = when (size) {
        0 -> error("Cannot pop empty vector")
        1 -> emptyPersistentVector()
        else -> {
            if (size - tailOffset > 1) {
                PVector(size - 1, shift, root, tail.copyOf(tail.size - 1))
            } else {
                val newTail = arrayFor(size - 2)
                val newRoot = popTail(shift, root) ?: EMPTY_NODE

                if (shift > 5 && newRoot.data[1] == null) {
                    PVector(size - 1, shift - 5, newRoot.subNode(0), newTail)
                } else {
                    PVector(size - 1, shift, newRoot, newTail)
                }
            }
        }
    }

    private fun popTail(level: Int, node: Node): Node? {
        val subIndex = (size - 2).indexAtLevel(level)
        return when {
            level > 5 -> {
                val newChild = popTail(level - 5, node.subNode(subIndex))
                if (newChild == null && subIndex == 0) null
                else Node(root.edit, node.data.withCopy { it[subIndex] = newChild })
            }
            subIndex == 0 -> null
            else -> Node(root.edit, node.data.withCopy { it[subIndex] = null })
        }
    }
}

private fun newPath(edit: AtomicBoolean, shift: Int, node: Node): Node =
    if (shift == 0) node
    else Node(edit).apply { data[0] = newPath(edit, shift - 5, node) }

private fun <T> copyPath(i: Int, level: Int, node: Node, elem: T): Node =
    Node(node.edit, node.data.withCopy { newData ->
        if (level == 0) {
            newData[i.indexAtLeaf()] = elem
        } else {
            val subIndex = i.indexAtLevel(level)
            newData[subIndex] = copyPath(i, level - 5, node.subNode(subIndex), elem)
        }
    })

private fun Int.indexAtLeaf() = this and 0x01f

private fun Int.indexAtLevel(level: Int) = (this ushr level) and 0x01f

private inline fun <T> Array<T>.withCopy(block: (Array<T>) -> Unit): Array<T> = copyOf().also(block)


data class Node(
    val edit: AtomicBoolean,
    val data: Array<Any?>
) {
    constructor(edit: AtomicBoolean) : this(edit, arrayOfNulls(32))

    fun subNode(i: Int) = data[i] as Node
    fun subNodeOrNull(i: Int) = data[i] as Node?
}

private val NODEDIT = AtomicBoolean(false)
private val EMPTY_NODE = Node(NODEDIT, Array(32) { null })

fun <T> emptyPersistentVector(): PVector<T> =
    PVector(0, 5, EMPTY_NODE, emptyArray())
