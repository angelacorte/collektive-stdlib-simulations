package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.stdlib.fields.fold
import kotlin.math.max

/**
 * A collection of self-stabilizing gossip algorithms.
 */
object SelfStabilizingGossip {
    /*
     * The best value exchanged in the gossip algorithm.
     * It contains the [best] value evaluated yet
     * and the [path] of nodes through which it has passed.
     */
    data class GossipValue<ID : Comparable<ID>, Value>(
        @JvmField
        val best: Value,
//        val local: Value,
        @JvmField
        val path: List<ID> = emptyList(),
    ) {
        fun base(local: Value, id: ID) = GossipValue(local, listOf(id))
        fun addHop(id: ID) = GossipValue(best, path + id)
    }

    /**
     * Self-stabilizing gossip-max.
     * Spreads across all (aligned) devices the current maximum [Value] of [local],
     * as computed by [comparator].
     */
    inline fun <reified ID : Comparable<ID>, Value> Aggregate<ID>.gossip(
        local: Value,
        comparator: Comparator<Value>,
    ): Value {
        val localGossip = GossipValue<ID, Value>(best = local)
        return share(localGossip) { gossip ->
            val neighbors = gossip.neighbors.toSet()
            val result = gossip.fold(localGossip) { current, (id, next) ->
                val valid = next.path.asReversed().asSequence().drop(1).none { it == localId || it in neighbors }
                val actualNext = if (valid) next else next.base(local, id)
                val candidateValue = comparator.compare(current.best, actualNext.best)
                when {
                    candidateValue > 0 -> current
                    candidateValue == 0 -> listOf(current, next).minBy { it.path.size }
                    else -> actualNext
                }
            }
            result.addHop(localId)
        }.best
    }

    inline fun <reified ID : Comparable<ID>, Value> Aggregate<ID>.gossip(
        env: EnvironmentVariables,
        local: Value,
        crossinline selector: (Value, Value) -> Value = { first, _ -> first }, // Default to identity function
    ): Value {
        val localGossip = GossipValue<ID, Value>(best = local)
        return share(localGossip) { gossip ->
            val neighbors = gossip.neighbors.toSet()
            val result = gossip.fold(localGossip) { current, (id, next) ->
                val valid = next.path.asReversed().asSequence().drop(1).none { it == localId || it in neighbors }
                val actualNext = if (valid) next else next.base(local, id)
                val candidateValue = selector(current.best, actualNext.best)
                when {
                    current.best == actualNext.best -> listOf(current, actualNext).minBy { it.path.size }
                    candidateValue == current.best -> current
                    else -> actualNext
                }
            }
            val newres = result.addHop(localId)
            env["neighbors-size"] = gossip.neighbors.size
            env["path"] = newres.path
            env["path-length"] = (newres.path).size
            newres
        }.best
    }
}