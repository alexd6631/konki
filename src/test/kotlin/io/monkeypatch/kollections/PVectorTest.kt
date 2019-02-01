package io.monkeypatch.kollections

import io.kotlintest.properties.Gen
import io.kotlintest.properties.assertAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlin.random.Random

const val nVectors = 1_000_000
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

            assertAll(nIterations, Gen.choose(0, nVectors)) { i ->
                val k = Random.nextInt(i)
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

        "test native fold" {
            val vectors = generateVectors(nVectors)
            val v = vectors.last()

            v.fold(0) { acc, i -> acc + i }
        }

        "testFold" {
            val vectors = generateVectors(nVectors)
            val v = vectors.last()

            v.asSequence().fold(0) { acc, i -> acc + i}
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