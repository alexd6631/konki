package org.alexd.kollections

import org.alexd.konki.vector.PVector
import org.alexd.konki.vector.emptyPersistentVector
import org.openjdk.jmh.annotations.Benchmark

private val v = generateVector(1_000_000)

open class PVectorGenerationBenchmark {
    @Benchmark
    fun testGenerate() {
        generateVector(1_000_000)
    }

    @Benchmark
    fun testGenerateTransient() {
        generateVectorTransient(1_000_000)
    }

    @Benchmark
    fun testPop() {
        (1 .. 1_000_000).fold(v) { acc, _ -> acc.removeLast() }
    }

    /*@Benchmark
    fun testPopTransient() {
        (1 .. 1_000_000).fold(v.asTransient()) { acc, _ -> acc.pop() }
    }*/
}

open class PVectorFoldBenchmark {
    @Benchmark
    fun testFold() {
        v.fold(0) { acc, i -> acc + i }
    }
}

open class PVectorMapBenchmark {
    @Benchmark
    fun testMapWithFold() {
        v.map { it + 1 }
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

private fun generateVectorTransient(n: Int): PVector<Int> =
    emptyPersistentVector<Int>().asTransient {
        (0 until n).fold(it) { acc, i -> acc + i }
    }
