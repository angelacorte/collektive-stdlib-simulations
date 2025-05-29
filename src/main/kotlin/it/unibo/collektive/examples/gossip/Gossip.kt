package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.Field.Companion.foldWithId
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.aggregate.Field.Companion.foldWithId

/**
 * A collection of self-stabilizing gossip algorithms.
 */
object SelfStabilizingGossip {
    /**
     * Self-stabilizing gossip-max.
     * Spreads across all (aligned) devices the current maximum [Value] of [local],
     * as computed by [comparator].
     */
    fun <ID : Comparable<ID>, Value> Aggregate<ID>.gossip(
        env: EnvironmentVariables,
        local: Value,
        comparator: Comparator<Value>,
    ): Value {
        /*
         * The best value exchanged in the gossip algorithm.
         * It contains the [best] value evaluated yet,
         * the [local] value of the node, and the [path] of nodes through which it has passed.
         */
        data class GossipValue<ID : Comparable<ID>, Value>(
            val best: Value,
            val local: Value,
            val path: List<ID> = emptyList(),
        ) {
            fun base(id: ID) = GossipValue(local, local, listOf(id))
        }

        val localGossip = GossipValue<ID, Value>(best = local, local = local)
        return share(localGossip) { gossip ->
            val neighbors = gossip.neighbors.toSet()
            val result = gossip.foldWithId(localGossip) { current, id, next ->
                val valid = next.path.asReversed().asSequence().drop(1).none { it == localId || it in neighbors }
                val actualNext = if (valid) next else next.base(id)
                val candidateValue = comparator.compare(current.best, actualNext.best)
                when {
                    candidateValue > 0 -> current
                    candidateValue == 0 -> listOf(current, next).minBy { it.path.size }
                    else -> actualNext
                }
            }
            env["neighbors-size"] = gossip.neighbors.size
            env["path-length"] = (result.path + localId).size
            GossipValue(result.best, local, result.path + localId)
        }.best
    }
}