package org.alexd.konki.vector

/**
 * Creates an empty persistent vector
 */
fun <T> emptyPersistentVector(): PVector<T> =
    PVector(0, 5, EMPTY_NODE, emptyArray())

fun <T> emptyTransientVector(): TVector<T> =
    emptyPersistentVector<T>().asTransient()

/**
 * Creates a persistent vector containing `items`
 */
fun <T> persistentVectorOf(vararg items: T): PVector<T> =
    items.asIterable().toPersistentVector()

/**
 * Build a persistent vector, by mutating an empty transient vector
 */
inline fun <T> withEmptyTransient(block: (TVect<T>) -> TVect<T>): PVector<T> =
    emptyPersistentVector<T>().asTransient(block)

/**
 * Converts this iterable to a persistent vector
 */
fun <T> Iterable<T>.toPersistentVector(): PVector<T> =
    emptyPersistentVector<T>() + this