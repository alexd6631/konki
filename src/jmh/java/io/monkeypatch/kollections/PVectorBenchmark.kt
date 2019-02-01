package io.monkeypatch.kollections

import org.openjdk.jmh.annotations.Benchmark

private val v = generateVector(1_000_000)

open class GenerationBenchmark {
    @Benchmark
    fun testGenerate1() {
        (0 until 1_000_000).fold(emptyPersistentVector<Int>()) { acc, i -> acc + i }
    }
}

open class PVectorBenchmark {

    @Benchmark
    fun testFold() {
        v.fold(0) { acc, i -> acc + i }
    }
}


private fun generateVector(n: Int): PVector<Int> =
    (0 until n).fold(emptyPersistentVector()) { acc, i -> acc + i }

private fun generateVectors(n: Int): List<PVector<Int>> {
    val testRange = 0 until n
    val vectors = mutableListOf<PVector<Int>>()
    testRange.fold(emptyPersistentVector<Int>()) { acc, i ->
        vectors.add(acc)
        acc + i
    }
    return vectors
}