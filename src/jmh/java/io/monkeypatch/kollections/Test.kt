package io.monkeypatch.kollections

import org.openjdk.jmh.annotations.Benchmark

open class Foo {

    @Benchmark
    fun simpleTest() {
        generateVectors(1_000)
    }
}

private fun generateVectors(n: Int): List<PVector<Int>> {
    val testRange = 0 until n
    val vectors = mutableListOf<PVector<Int>>()
    testRange.fold(emptyPersistentVector<Int>()) { acc, i ->
        vectors.add(acc)
        acc + i
    }
    return vectors
}