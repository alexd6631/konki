package org.alexd.kollections

import io.kotlintest.properties.Gen
import io.kotlintest.properties.assertAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.alexd.konki.vector.emptyPersistentVector

internal class TVectorTest : StringSpec() {
    init {
        "test append and get" {
            val n = 100000
            val v = emptyPersistentVector<Int>().asTransient {
                (0 until n).fold(it) { acc, i -> acc + i }
            }

            v.size shouldBe n
            for (j in 0 until n) {
                v[j] shouldBe j
            }
        }

        "test iterator" {
            val n = 100000
            val v = emptyPersistentVector<Int>().asTransient {
                (0 until n).fold(it) { acc, i -> acc + i }
            }

            v.size shouldBe n
            var m = 0
            v.forEach { i ->
                i shouldBe m
                m += 1
            }
            m shouldBe n
        }

        "test update" {
            val vectors = generateVectors(nVectors)

            assertAll(
                nIterations,
                Gen.choose(10, nVectors)
            ) { i ->
                val k = i / 2
                val oldVect = vectors[i]
                val v = oldVect.asTransient {
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

        "test removeLast" {
            val n = 10000
            val fullVector = generateVector(n).asTransient()
            val res = (1..n).fold(fullVector) { acc, i ->
                acc.removeLast().also { v ->
                    val m = n - i
                    v.size shouldBe m
                    fullVector.size shouldBe m
                    v.forEachIndexed { index, i ->
                        i shouldBe index
                    }
                }
            }
            res.size shouldBe 0
        }

        "test toString" {
            val v = generateVector(10).asTransient()
            v.toString() shouldBe "TVector(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)"
        }
    }
}