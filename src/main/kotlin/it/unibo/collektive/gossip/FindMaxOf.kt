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
