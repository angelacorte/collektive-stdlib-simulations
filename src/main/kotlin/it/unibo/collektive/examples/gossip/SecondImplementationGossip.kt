package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables

object SecondImplementationGossip {
    inline fun <reified ID : Comparable<ID>, Value> Aggregate<ID>.secondGossip(
        env: EnvironmentVariables,
        initial: Value,
        selector: Comparator<Value>,
    ): Value {
        val local = GossipValue<ID, Value>(initial, initial)
        return share(local) { gossip ->
            val result =
                gossip.neighbors.list.fold(local) { current, (id, next) ->
                    val actualNext = if (localId in next.path) next.base(id) else next
                    val candidateValue = selector.compare(current.best, actualNext.best)
                    when {
                        candidateValue > 0 -> current
                        candidateValue == 0 -> listOf(current, next).minBy { it.path.size }
                        else -> actualNext
                    }
                }
            env["neighbors-size"] = gossip.neighbors.size
            env["path"] = result.path.joinToString("->")
            env["path-length"] = result.path.size
            GossipValue(result.best, initial, result.path + localId)
        }.best
    }

    data class GossipValue<ID : Comparable<ID>, Value>(
        @JvmField
        val best: Value,
        @JvmField
        val local: Value,
        @JvmField
        val path: List<ID> = emptyList(),
    ) {
        fun base(id: ID) = GossipValue(local, local, listOf(id))
    }
}
