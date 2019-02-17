package io.monkeypatch.kollections.vector

import java.util.concurrent.atomic.AtomicBoolean


@Suppress("UNCHECKED_CAST")
class TVector<T> internal constructor(
    @Volatile private var _size: Int,
    @Volatile private var shift: Int,
    @Volatile private var root: Node,
    @Volatile private var tail: Array<Any?>
) : BaseVector<T>() {

    internal constructor(vector: PVector<T>) : this(
        vector.size,
        vector.shift,
        editableRoot(vector.root),
        editableTail(vector.tail)
    )

    override val size get() = _size

    private val tailOffset
        get() = if (_size < 32) 0 else (_size - 1).ushr(5).shl(5)

    private inline fun <T> ensuringEditable(block: () -> T): T =
        if (root.edit.get()) block() else throw IllegalAccessException("Transient used after persisted")

    private fun ensureEditable(node: Node) =
        if (node.edit == root.edit) node else Node(root.edit, node.data.copyOf())

    private inline fun ensuringEditable(node: Node, block: Node.() -> Unit): Node =
        ensureEditable(node).apply(block)

    fun persistent(): PVector<T> = ensuringEditable {
        root.edit.set(false)
        val trimmedTail = tail.copyOf(_size - tailOffset)
        PVector(_size, shift, root, trimmedTail)
    }

    operator fun plus(elem: T): TVector<T> = ensuringEditable {
        if (_size - tailOffset < 32) {
            tail[_size.indexAtLeaf()] = elem
        } else {
            val tailNode = Node(root.edit, tail)
            tail = arrayOfNulls<Any?>(32).also { it[0] = elem }
            if (fullCapacityReached()) {
                root = Node(root.edit).apply {
                    data[0] = root
                    data[1] = newPath(root.edit, shift, tailNode)
                }
                shift += 5
            } else {
                root = pushTail(shift, root, tailNode)
            }
        }
        _size += 1
        this
    }

    private fun pushTail(level: Int, parent: Node, tailNode: Node): Node =
        ensuringEditable(parent) {
            val subIndex = (_size - 1).indexAtLevel(level)

            data[subIndex] = if (level == 5) tailNode else {
                val child = parent.subNodeOrNull(subIndex)
                child?.let { pushTail(level - 5, it, tailNode) }
                    ?: newPath(root.edit, level - 5, tailNode)
            }
        }

    private fun fullCapacityReached() = (_size ushr 5) > (1 shl shift)


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

    private fun editableArrayFor(i: Int): Array<Any?> =
        if (i >= tailOffset) tail
        else {
            var node = root
            var level = shift
            while (level > 0) {
                node = ensureEditable(node.subNode(i.indexAtLevel(level)))
                level -= 5
            }
            node.data
        }


    fun update(i: Int, elem: T): TVector<T> = ensuringEditable {
        if (i in 0 until size) {
            if (i >= tailOffset) {
                tail[i.indexAtLeaf()] = elem as Any?
            } else {
                root = doUpdate(shift, root, i, elem)
            }
            this
        } else throw IndexOutOfBoundsException()
    }

    private fun doUpdate(level: Int, node: Node, i: Int, elem: T): Node = ensuringEditable(node) {
        if (level == 0) {
            data[i.indexAtLeaf()] = elem
        } else {
            val subIndex = i.indexAtLevel(level)
            data[subIndex] = doUpdate(level - 5, node.subNode(subIndex), i, elem)
        }
    }

    fun removeLast() = ensuringEditable {
        when (size) {
            0 -> error("Cannot removeLast empty vector")
            1 -> {
                _size = 0
            }
            else -> {
                if ((size - 1).indexAtLeaf() == 0) {
                    val newTail = editableArrayFor(size - 2)
                    val newRoot = popTail(shift, root) ?: Node(root.edit)
                    if (shift > 5 && newRoot.data[1] == null) {
                        root = ensureEditable(newRoot.data[0] as Node)
                        shift -= 5
                    } else {
                        root = newRoot
                    }
                    tail = newTail
                }
                _size -= 1
            }
        }
        this
    }

    private fun popTail(level: Int, node: Node): Node? = ensuringEditable(node) {
        val subIndex = (size - 2).indexAtLevel(level)
        when {
            level > 5 -> {
                val newChild = popTail(level - 5, node.subNode(subIndex))
                if (newChild == null && subIndex == 0) return null
                else {
                    data[subIndex] = newChild
                }
            }
            subIndex == 0 -> return null
            else -> data[subIndex] = null
        }
    }

    override fun toString() = "TVector(${joinToString(", ")})"
}

private fun editableRoot(node: Node) =
    Node(AtomicBoolean(true), node.data.copyOf())

private fun editableTail(tail: Array<Any?>): Array<Any?> = tail.copyOf(32)