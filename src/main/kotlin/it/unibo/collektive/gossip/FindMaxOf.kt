package it.unibo.collektive.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.share
import it.unibo.collektive.aggregate.values
import it.unibo.collektive.stdlib.collapse.fold

object FindMaxOf {
    data class GossipValue<ID : Comparable<ID>, Value>(
        @JvmField
        val best: Value,
        @JvmField
        val path: List<ID> = emptyList(),
    ) {
        fun addHop(id: ID) = GossipValue(best, path + id)
    }

    inline fun <reified ID : Comparable<ID>, Value> Aggregate<ID>.findMaxOf(
        local: Value,
        comparator: Comparator<in Value>,
    ): Value {
        val localGossip = GossipValue<ID, Value>(best = local)
        return share(localGossip) { gossip ->
            gossip.neighbors.values.fold(localGossip) { current, neighbor ->
                val comparatorResult = comparator.compare(neighbor.best, current.best)
                when {
                    localId in neighbor.path -> current // Ignore paths that loop back
                    comparatorResult == 0 -> // If values tie, select based on path
                        when {
                            neighbor.path.size == current.path.size ->
                                // If paths are of equal length, compare their last element
                                // (they necessarily come from different neighbors)
                                listOf(neighbor, current).minBy { it.path.last() }
                            // Select the shortest path
                            else -> listOf(neighbor, current).minBy { it.path.size }
                        }
                    // Pick the best value according to the comparator
                    comparatorResult < 0 -> neighbor
                    else -> current
                }
            }.addHop(localId)
        }.best
    }
}

inline fun <T, R> Iterable<T>.maxByWith(
    comparator: Comparator<in R>,
    selector: (T) -> R
): T {
    val iterator = iterator()
    if (!iterator.hasNext()) throw NoSuchElementException()
    var maxElem = iterator.next()
    var maxValue = selector(maxElem)
    while (iterator.hasNext()) {
        val e = iterator.next()
        val v = selector(e)
        if (comparator.compare(maxValue, v) < 0) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

//
///**
// * Provides a no-loop gossip algorithm implementation.
// * This algorithm ensures nodes propagate and compute the "best" value while
// * avoiding looping paths based on their neighbors and the defined evaluation criteria.
// */
//object NoLoopAgainstNeighborsGossip {
//    /*
//     * The best value exchanged in the gossip algorithm.
//     * It contains the [best] value evaluated yet
//     * and the [path] of nodes through which it has passed.
//     */
//    data class GossipValue<ID : Comparable<ID>, Value>(
//        @JvmField
//        val best: Value,
//        @JvmField
//        val path: List<ID> = emptyList(),
//    ) {
//        fun base(
//            local: Value,
//            id: ID,
//        ) = GossipValue(local, listOf(id))
//
//        fun addHop(id: ID) = GossipValue(best, path + id)
//    }
//
//    /**
//     * Implements a no-loop gossip algorithm to propagate and compute the "best" value while avoiding looping paths.
//     *
//     * @param local The local value of the node to be compared with propagated values during gossip.
//     * @param selector A selection strategy between two values.
//     * Defaults to an identity function that prefers the first value.
//     * @return The "best" value found after executing the gossip algorithm given the selector.
//     */
//    inline fun <reified ID : Comparable<ID>, Value> Aggregate<ID>.findMaxOf(
//        local: Value,
//        crossinline selector: (Value, Value) -> Value = { first, _ -> first }, // Default to identity function
//    ): Value {
//        val localGossip = GossipValue<ID, Value>(best = local)
//        return share(localGossip) { gossip ->
//            val neighbors = gossip.neighbors.ids.set
//            val result =
//                gossip.all
//                    .fold(localGossip) { current, (id, next) ->
//                        val nextIsValidPath =
//                            next.path
//                                .asReversed()
//                                .asSequence()
//                                .drop(1) // remove the entry of my neighbor
//                                .none { it == localId || it in neighbors }
//                        val actualNext = if (nextIsValidPath) next else GossipValue(local, listOf(id))
//                        val candidateValue = selector(current.best, actualNext.best)
//                        when {
//                            current.best == actualNext.best -> listOf(current, actualNext).minBy { it.path.size }
//                            candidateValue == current.best -> current
//                            else -> actualNext
//                        }
//                    }.addHop(localId)
//            result
//        }.best
//    }
//}
