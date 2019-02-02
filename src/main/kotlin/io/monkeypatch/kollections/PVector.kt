package io.monkeypatch.kollections

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
            val tailNode = Node(tail)
            if (fullCapacityReached()) {
                val newRoot = Node().apply {
                    data[0] = root
                    data[1] = newPath(shift, tailNode)
                }
                PVector(size + 1, shift + 5, newRoot, newTail)
            } else {
                val newRoot = pushTail(shift, root, tailNode)
                PVector(size + 1, shift, newRoot, newTail)
            }
        }

    private fun fullCapacityReached() = (size ushr 5) > (1 shl shift)

    private fun pushTail(level: Int, parent: Node, tailNode: Node): Node =
        Node(parent.data.clone()).apply {
            val subIndex = (size - 1).indexAtLevel(level)
            val nodeToInsert = if (level == 5) tailNode else {
                val child = parent.data[subIndex] as Node?
                child?.let { pushTail(level - 5, it, tailNode) } ?: newPath(level - 5, tailNode)
            }
            data[subIndex] = nodeToInsert
        }

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
                val newTail = tail.copyOf().also {
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
}

private fun newPath(shift: Int, node: Node): Node =
    if (shift == 0) node
    else Node().apply { data[0] = newPath(shift - 5, node) }

private fun <T> copyPath(i: Int, level: Int, node: Node, elem: T): Node =
    Node(
        if (level == 0) {
            node.data.copyOf().also { it[i.indexAtLeaf()] = elem }
        } else {
            val subIndex = i.indexAtLevel(level)
            node.data.copyOf().also {
                it[subIndex] = copyPath(i, level - 5, node.data[subIndex] as Node, elem)
            }
        }
    )

private fun Int.indexAtLeaf() = this and 0x01f

private fun Int.indexAtLevel(level: Int) = (this ushr level) and 0x01f

data class Node(
    val data: Array<Any?>
) {
    constructor() : this(arrayOfNulls(32))
}


val EMPTY_NODE = Node(Array(32) { null })
fun <T> emptyPersistentVector(): PVector<T> =
    PVector(0, 5, EMPTY_NODE, emptyArray())

fun main() {
    val end = (0..204800).fold(emptyPersistentVector<Int>()) { acc, i ->
        val after = acc + i
        //println(after)
        after
    }
    println(end[1024])
    println(end.update(42, 777)[42])
    println("Done")
}