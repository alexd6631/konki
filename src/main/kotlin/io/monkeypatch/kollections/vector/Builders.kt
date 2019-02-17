package io.monkeypatch.kollections.vector

fun <T> emptyPersistentVector(): PVector<T> =
    PVector(0, 5, EMPTY_NODE, emptyArray())

fun <T> emptyTransientVector(): TVector<T> = emptyPersistentVector<T>().asTransient()

fun <T> persistentVectorOf(vararg items: T) = items.asIterable().toPersistentVector()

inline fun <T> withEmptyTransient(block: (TVect<T>) -> TVect<T>): PVector<T> =
    emptyPersistentVector<T>().asTransient(block)

fun <T> Iterable<T>.toPersistentVector() = emptyPersistentVector<T>() + this