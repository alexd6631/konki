package io.monkeypatch.kollections

import io.kotlintest.properties.Gen
import io.kotlintest.properties.assertAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

const val nVectors = 1000//1_000_000
const val nIterations = 100

internal class PVectorTest : StringSpec() {

    init {
        "test append and get" {
            val vectors = generateVectors(nVectors)

            assertAll(nIterations, Gen.choose(0, nVectors)) { i ->
                val v = vectors[i]
                v.size shouldBe i
                for (j in 0 until i) {
                    v[j] shouldBe j
                }
            }
        }

        "test update" {
            val vectors = generateVectors(nVectors)

            assertAll(nIterations, Gen.choose(10, nVectors)) { i ->
                val k = i / 2
                //println(i)
                val v = vectors[i].update(k, 42).update(i - 1, 84)
                v.size shouldBe i
                for (j in 0 until i) {
                    when (j) {
                        k -> v[j] shouldBe 42
                        i - 1 -> v[j] shouldBe 84
                        else -> v[j] shouldBe j
                    }
                }
            }
        }

        "test asSequence" {
            val vectors = generateVectors(nVectors)

            assertAll(nIterations, Gen.choose(0, nVectors)) { i ->
                val v = vectors[i]

                v.asSequence().forEachIndexed { index, i ->
                    i shouldBe index
                }
            }
        }

        "test fold" {
            val range = 0 until nVectors
            val v = range.fold(emptyPersistentVector<Int>()) { acc, i -> acc + i }

            val out = v.fold(mutableListOf<Int>()) { acc, i -> acc.apply { add(i) } }
            out shouldBe range.toList()
        }

        "test pop" {
            val n = 10000
            val fullVector = generateVector(n)
            val res = (1 .. n).fold(fullVector) { acc, i ->
                acc.pop().also { v ->
                    v.size shouldBe n - i
                    v.asSequence().forEachIndexed { index, i ->
                        i shouldBe index
                    }
                }
            }
            res.size shouldBe 0
        }
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


private fun generateVector(n: Int): PVector<Int> =
    (0 until n).fold(emptyPersistentVector()) { acc, i -> acc + i }
