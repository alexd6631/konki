# Konki #

A persistent collections library for Kotlin. 

`Kon'ki` (根気) is a japanese word for patience / perseverance.

## Why using persistent collection ? ##

Kotlin has already a nice support for immutable lists and a lot of built-in
extensions to enable functional programming.

However, Kotlin immutable list are "read-only" views over mutable ArrayList.
This offer nice random-access performance. But operation such as appending or
updating an immutable list, will need to perform a full copy the list.

Persistent collection, on the contrary, allow to amortize common operations such
as appending or updating thanks to structural sharing, while being immutable.

Persistent collection, comes often with their transient counterpart, which
trades off local mutability for performance.

## Why another persistent collection library ##

There is a lot of good and mature options for Persistent collections on the JVM
:
  * [PCollections](https://pcollections.org)
  * [Paguro](https://github.com/GlenKPeterson/Paguro)
  * [Vavr](http://www.vavr.io)
  * [Kotlinx.collections.immutable](https://github.com/Kotlin/kotlinx.collections.immutable/pull/20)
    the project seems inactive at the time of writing, but with the following
    pull-request, we should see pure Kotlin implementation of collections:
    https://github.com/Kotlin/kotlinx.collections.immutable/pull/20

`Kon'ki` is a personal project to explore :

  * Algorithmic foundation of persistent collections.
  * Performance and bench-marking of Kotlin construct, such as inline methods,
    sequence generator, etc ...
  * API design and implementation of common functional operators
  * Property-based testing

Right now, only the persistent vector and its transient version is implemented,
with reasonable test coverage, and careful attention to operator performance.
