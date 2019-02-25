package org.alexd.konki.vector

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min


/**
 * A PersistentVector with "practically" constant time `plus`, `update`, `removeLast` methods.
 * Common functional operators have been implemented, with optimal iteration speed.
 * Other least common operators, are accessible through standard Iterable extensions.
 *
 * As the vector is persistent and immutable, each operator return a new "version" of the vector.
 *
 * The implementation was based on algorithm found in Clojure implementation
 * [https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/PersistentVector.java]
 */
@Suppress("UNCHECKED_CAST")
class PVector<out T> internal constructor(
    override val size: Int,
    internal val shift: Int,
    internal val root: Node,
    internal val tail: Array<Any?>
) : BaseVector<T>() {

    private val tailOffset
        get() = if (size < 32) 0 else (size - 1).ushr(5).shl(5)

    /**
     * Creates a new persistent vector, by appending `elem` to it.
     *
     * This operation is "practically" linear time.
     * It is O(n) 31/32 of times and O(log_32(n)) 1/32 time.
     */
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

    override fun arrayFor(i: Int): Array<Any?> =
        if (i >= tailOffset) tail
        else {
            var node = root
            var level = shift
            while (level > 0) {
                node = node.subNode(i.indexAtLevel(level))
                level -= 5
            }
            node.data
        }

    /**
     * Creates a new persistent vector, by modifying the element at index `i`
     *
     * @throws IndexOutOfBoundsException
     */
    fun update(i: Int, elem: @UnsafeVariance T): PVector<T> =
        if (i in 0 until size) {
            if (i >= tailOffset) {
                val newTail = tail.withCopy {
                    it[i.indexAtLeaf()] = elem as Any?
                }
                PVector(size, shift, root, newTail)
            } else {
                PVector(
                    size,
                    shift,
                    copyPath(i, shift, root, elem),
                    tail
                )
            }
        } else throw IndexOutOfBoundsException()

    /**
     * Creates a new persistent vector, by removing the last element
     */
    fun removeLast(): PVector<T> = when (size) {
        0 -> error("Cannot removeLast empty vector")
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

    fun asTransient(): TVect<T> = TVector(this)

    inline fun asTransient(block: (TVect<T>) -> TVect<T>): PVect<T> =
        asTransient().let(block).persistent()

    private inline fun forEachInline(f: (T) -> Unit) {
        var i = 0
        while (i < size) {
            val array = arrayFor(i)
            for (e in array) {
                f(e as T)
            }
            i += array.size
        }
    }

    private inline fun <R> foldInline(initial: R, f: (R, T) -> R): R {
        var acc = initial
        forEachInline { acc = f(acc, it) }
        return acc
    }

    private inline fun forEachIndexedInline(f: (index: Int, T) -> Unit) {
        var i = 0
        while (i < size) {
            val array = arrayFor(i)
            for (e in array) {
                f(i++, e as T)
            }

        }
    }

    private inline fun <R> foldIndexedInline(initial: R, f: (index:Int, R, T) -> R): R {
        var acc = initial
        forEachIndexedInline { index, t -> acc = f(index, acc, t) }
        return acc
    }

    fun forEach(f: (T) -> Unit): Unit = forEachInline(f)

    fun forEachIndexed(f: (index: Int, T) -> Unit): Unit = forEachIndexedInline(f)

    fun <R> fold(initial: R, f: (R, T) -> R): R = foldInline(initial, f)

    fun <U> map(f: (T) -> U): PVector<U> =
        withEmptyTransient {
            foldInline(it) { acc, e -> acc + f(e) }
        }

    fun <U> mapNotNull(f: (T) -> U?): PVector<U> =
        withEmptyTransient {
            foldInline(it) { acc, e -> f(e)?.let(acc::plus) ?: acc }
        }

    fun <U> mapIndexed(f: (index:Int, T) -> U): PVector<U> =
        withEmptyTransient {
            foldIndexedInline(it) { i, acc, e -> acc + f(i, e) }
        }

    fun <U> mapIndexedNotNull(f: (index: Int, T) -> U?): PVector<U> =
        withEmptyTransient {
            foldIndexedInline(it) { i, acc, e -> f(i, e)?.let(acc::plus) ?: acc }
        }

    fun filter(f: (T) -> Boolean): PVector<T> =
        withEmptyTransient {
            foldInline(it) { acc, e -> if (f(e)) acc + e else acc }
        }

    fun filterNot(f: (T) -> Boolean): PVector<T> =
        withEmptyTransient {
            foldInline(it) { acc, e -> if (!f(e)) acc + e else acc }
        }

    fun filterIndexed(f: (index: Int, T) -> Boolean): PVector<T> =
        withEmptyTransient {
            foldIndexedInline(it) { i, acc, e -> if (f(i, e)) acc + e else acc }
        }

    fun <U> flatMap(f: (T) -> Iterable<U>): PVector<U> =
        withEmptyTransient {
            foldInline(it) { acc, e -> f(e).fold(acc) { a, u -> a + u } }
        }

    private inline fun <U, R> zipInline(other: Iterable<U>, transform: (T, U) -> R): PVector<R> =
        withEmptyTransient {
            var vect = it
            val first = iterator()
            val second = other.iterator()
            while (first.hasNext() && second.hasNext()) {
                vect += (transform(first.next(), second.next()))
            }
            vect
        }

    fun <U, R> zip(other: Iterable<U>, transform: (T, U) -> R): PVector<R> = zipInline(other, transform)

    infix fun <U> zip(other: Iterable<U>): PVector<Pair<T, U>> = zipInline(other) { a, b -> a to b }

    operator fun plus(other: Iterable<@UnsafeVariance T>) = asTransient {
        other.fold(it) { acc, t -> acc + t }
    }

    operator fun plus(other: PVector<@UnsafeVariance T>) = asTransient {
        other.fold(it) { acc, t -> acc + t }
    }

    fun take(n: Int): PVector<T> =
        withEmptyTransient {
            rangedSequence(end = min(size, n)).fold(it) { acc, t -> acc + t }
        }

    fun drop(n: Int): PVector<T> =
        withEmptyTransient {
            rangedSequence(start = max(0, n)).fold(it) { acc, t -> acc + t }
        }

    override fun toString() = "PVector(${joinToString(", ")})"
}

private val NODEDIT = AtomicBoolean(false)

internal val EMPTY_NODE = Node(NODEDIT, Array(32) { null })