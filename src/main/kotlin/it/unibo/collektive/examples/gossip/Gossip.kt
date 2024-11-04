package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.operators.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.field.Field.Companion.foldWithId

context(EnvironmentVariables)
fun <ID : Comparable<ID>, Value> Aggregate<ID>.gossipMax(
    initial: Value,
    selector: Comparator<Value>,
): Value {
    /**
     * The best value exchanged in the gossip algorithm.
     * It contains the [best] value evaluated yet,
     * the [local] value of the node and the [path] of nodes through which it has passed.
     */
    data class GossipValue<ID : Comparable<ID>, Value>(
        val best: Value,
        val local: Value,
        val path: List<ID> = emptyList(),
    ) {
        fun base(id: ID) = GossipValue(local, local, listOf(id))
    }
    val local = GossipValue<ID, Value>(best = initial, local = initial)
    return share(local) { gossip ->
        val neighbors = gossip.neighbors.toSet()
        val result = gossip.foldWithId(local) { current, id, next ->
            val valid = next.path.asReversed().asSequence().drop(1).none { it == localId || it in neighbors }
            val actualNext = if (valid) next else next.base(id)
            val candidateValue = selector.compare(current.best, actualNext.best)
            when {
                candidateValue > 0 -> current
                candidateValue == 0 -> listOf(current, next).minBy { it.path.size }
                else -> actualNext
            }
        }
        set("neighbors-size", neighbors.size)
        set("path-length", (result.path + localId).size)
        GossipValue(result.best, initial, result.path + localId)
    }.best
}

/**
 * A gossip algorithm that computes whether any device is experiencing a certain [condition].
 */
context(EnvironmentVariables)
fun <ID : Comparable<ID>> Aggregate<ID>.isHappeningGossip(
    condition: () -> Boolean,
): Boolean = gossipMax(condition()) { first, second -> first.compareTo(second) }

