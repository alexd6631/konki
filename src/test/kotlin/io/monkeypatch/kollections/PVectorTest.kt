package io.monkeypatch.kollections

import io.kotlintest.properties.Gen
import io.kotlintest.properties.assertAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

const val nVectors = 10000//1_000_000
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

                v.seq.forEachIndexed { index, i ->
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
            val res = (1..n).fold(fullVector) { acc, i ->
                acc.pop().also { v ->
                    v.size shouldBe n - i
                    v.seq.forEachIndexed { index, i ->
                        i shouldBe index
                    }
                }
            }
            res.size shouldBe 0
        }

        "test first and last" {
            val v = generateVector(100000)
            v.first() shouldBe 0
            v.last() shouldBe 99999
        }

        "testMap" {
            val v = generateVector(10000)
            val v2 = v.map { it + 1 }
            v2.size shouldBe 10000
            v2.seq.forEachIndexed { index, i ->
                i shouldBe index + 1
            }
        }

        "testFilter" {
            val v = generateVector(10000)
            val v2 = v.filter { it % 2 == 0 }
            v2.size shouldBe 5000
            v2.seq.forEachIndexed { index, i ->
                i shouldBe index * 2
            }
        }

        "testFlatMap" {
            val v = generateVector(10000)
            val v2 = v.flatMap { listOf(it, -it) }
            v2.size shouldBe 20000
            v2.seq.forEachIndexed { index, i ->
                if (index % 2 == 0) {
                    i shouldBe index / 2
                } else {
                    i shouldBe -(index / 2)
                }
            }
        }
    }
}


internal class TVectorTest : StringSpec() {
    init {

        "test append and get" {
            val n = 100000
            val v = emptyPersistentVector<Int>().withTransient {
                (0 until n).fold(it) { acc, i -> acc + i }
            }

            v.size shouldBe n
            for (j in 0 until n) {
                v[j] shouldBe j
            }
        }

        "test update" {
            val vectors = generateVectors(nVectors)

            assertAll(nIterations, Gen.choose(10, nVectors)) { i ->
                val k = i / 2
                val oldVect = vectors[i]
                val v = oldVect.withTransient {
                    it.update(k, 42).update(i - 1, 84)
                }

                v.size shouldBe i
                for (j in 0 until i) {
                    when (j) {
                        k -> v[j] shouldBe 42
                        i - 1 -> v[j] shouldBe 84
                        else -> v[j] shouldBe j
                    }
                }

                for (j in 0 until i) {
                    oldVect[j] shouldBe j
                }
            }
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
