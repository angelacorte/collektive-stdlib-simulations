package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.operators.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.field.Field.Companion.fold

context(EnvironmentVariables)
fun <ID : Comparable<ID>, Type> Aggregate<ID>.firstGossip(
    initial: Type,
    selector: (Type, Type) -> Boolean,
): Type {
    data class GossipValue<ID : Comparable<ID>, Type>(val value: Type, val path: List<ID>)
    val local = GossipValue(initial, listOf(localId))
    return share(local) { gossip ->
        val result = gossip.fold(local) { current, next ->
            when {
                selector(current.value, next.value) || localId in next.path -> current
                else -> next.copy(path = next.path + localId)
            }
        }
        set("neighbors-size", gossip.neighbors.size)
        set("path-length", (result.path + localId).size)
        result
    }.value
}
