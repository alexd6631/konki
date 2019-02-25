package org.alexd.kollections

import io.kotlintest.properties.Gen
import io.kotlintest.properties.assertAll
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import org.alexd.konki.vector.PVector
import org.alexd.konki.vector.emptyPersistentVector
import org.alexd.konki.vector.persistentVectorOf

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

        "test rangedSequence" {
            val vectors = generateVectors(nVectors)

            assertAll(nIterations, Gen.choose(0, nVectors)) { i ->
                val v = vectors[i]

                v.forEachIndexed { index, j ->
                    j shouldBe index
                }
            }
        }

        "test fold" {
            val range = 0 until nVectors
            val v = range.fold(emptyPersistentVector<Int>()) { acc, i -> acc + i }

            val out = v.fold(mutableListOf<Int>()) { acc, i -> acc.apply { add(i) } }
            out shouldBe range.toList()
        }

        "test removeLast" {
            val n = 10000
            val fullVector = generateVector(n)
            val res = (1..n).fold(fullVector) { acc, i ->
                acc.removeLast().also { v ->
                    v.size shouldBe n - i
                    v.forEachIndexed { index, i ->
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

        "test map" {
            val v = generateVector(10000)
            val v2 = v.map { it + 1 }
            v2.size shouldBe 10000
            v2.forEachIndexed { index, i ->
                i shouldBe index + 1
            }
        }

        "test filter" {
            val v = generateVector(10000)
            val v2 = v.filter { it % 2 == 0 }
            v2.size shouldBe 5000
            v2.forEachIndexed { index, i ->
                i shouldBe index * 2
            }
        }

        "test flatMap" {
            val v = generateVector(10000)
            val v2 = v.flatMap { listOf(it, -it) }
            v2.size shouldBe 20000
            v2.forEachIndexed { index, i ->
                if (index % 2 == 0) {
                    i shouldBe index / 2
                } else {
                    i shouldBe -(index / 2)
                }
            }
        }

        "test flatMap on persistent vector" {
            val v = generateVector(10000)
            val v2 = v.flatMap { persistentVectorOf(it, -it) }
            v2.size shouldBe 20000
            v2.forEachIndexed { index, i ->
                if (index % 2 == 0) {
                    i shouldBe index / 2
                } else {
                    i shouldBe -(index / 2)
                }
            }
        }

        "test concat" {
            val v1 = persistentVectorOf(1, 2, 3)
            val v2 = persistentVectorOf(4, 5, 6)
            val v = v1 + v2

            v.size shouldBe 6
            v.forEachIndexed { index, i ->
                i shouldBe index + 1
            }
        }

        "test concat iterable" {
            val v1 = persistentVectorOf(1, 2, 3)
            val v = v1 + listOf(4, 5, 6)

            v.size shouldBe 6
            v.forEachIndexed { index, i ->
                i shouldBe index + 1
            }
        }

        "test take" {
            val v1 = generateVector(100)
            val v = v1.take(10)

            v.size shouldBe 10
            v.forEachIndexed { index, i ->
                i shouldBe index
            }
        }

        "test drop" {
            val v1 = generateVector(100)
            val v = v1.drop(10)

            v.size shouldBe 90
            v.forEachIndexed { index, i ->
                i shouldBe index + 10
            }
        }

        "test take (out of bounds)" {
            val v1 = generateVector(5)
            val v = v1.take(10)
            v1.size shouldBe 5
            v.size shouldBe 5
            v.forEachIndexed { index, i ->
                i shouldBe index
            }
        }

        "test toString" {
            val v = generateVector(10)
            v.toString() shouldBe "PVector(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)"
        }

        "test equals/hasCode on vectors" {
            val v1 = persistentVectorOf(1, 2, 3)
            val v2 = persistentVectorOf(1, 2, 3)
            val v3 = persistentVectorOf(4, 5, 6)
            val v4 = persistentVectorOf(1, 2)

            v1 shouldBe v2
            v1 shouldBe v2.asTransient()
            v1 shouldNotBe v3
            v1 shouldNotBe v4

            v1.hashCode() shouldBe v2.hashCode()
            v1.hashCode() shouldNotBe v3.hashCode()
            v1.hashCode() shouldNotBe v4.hashCode()
        }

        "test equals/hasCode with lists" {
            val v1 = persistentVectorOf(1, 2, 3)
            val v2 = listOf(1, 2, 3)
            val v3 = listOf(4, 5, 6)
            val v4 = listOf(1, 2)

            v1 shouldBe v2
            v1 shouldNotBe v3
            v1 shouldNotBe v4

            v1.hashCode() shouldBe v2.hashCode()
            v1.hashCode() shouldNotBe v3.hashCode()
            v1.hashCode() shouldNotBe v4.hashCode()
        }

        "test zip" {
            val v1 = persistentVectorOf(1, 2, 3)
            val v2 = persistentVectorOf(4, 5, 6)

            val v = v1 zip v2
            v shouldBe persistentVectorOf(1 to 4, 2 to 5, 3 to 6)
        }

        "test zip (unequal size)" {
            val v1 = persistentVectorOf(1, 2, 3)
            val v2 = persistentVectorOf(4, 5)

            val v = v1 zip v2
            v shouldBe persistentVectorOf(1 to 4, 2 to 5)
        }

        "test zip with transform" {
            val v1 = persistentVectorOf(1, 2, 3)
            val v2 = persistentVectorOf(4, 5, 6)

            val v = v1.zip(v2) { a, b -> a * b }
            v shouldBe persistentVectorOf(4, 10, 18)
        }
    }
}


internal fun generateVectors(n: Int): List<PVector<Int>> {
    val testRange = 0 until n
    val vectors = mutableListOf<PVector<Int>>()
    testRange.fold(emptyPersistentVector<Int>()) { acc, i ->
        vectors.add(acc)
        acc + i
    }
    return vectors
}


internal fun generateVector(n: Int): PVector<Int> =
    (0 until n).fold(emptyPersistentVector()) { acc, i -> acc + i }
