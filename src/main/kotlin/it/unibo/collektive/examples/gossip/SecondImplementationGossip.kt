package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.operators.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.field.Field.Companion.foldWithId

context(EnvironmentVariables)
fun <ID : Comparable<ID>, Value> Aggregate<ID>.secondGossip(
    initial: Value,
    selector: Comparator<Value>,
): Value {
    data class GossipValue<ID : Comparable<ID>, Value>(
        val best: Value,
        val local: Value,
        val path: List<ID> = emptyList(),
    ) {
        fun base(id: ID) = GossipValue(local, local, listOf(id))
    }
    val local = GossipValue<ID, Value>(initial, initial)
    return share(local) { gossip ->
        val result = gossip.foldWithId(local) { current, id, next ->
            val actualNext = if (localId in next.path) next.base(id) else next
            val candidateValue = selector.compare(current.best, actualNext.best)
            when {
                candidateValue > 0 -> current
                candidateValue == 0 -> listOf(current, next).minBy { it.path.size }
                else -> actualNext
            }
        }
        set("path-length", (result.path + localId).size)
        GossipValue(result.best, initial, result.path + localId)
    }.best
}