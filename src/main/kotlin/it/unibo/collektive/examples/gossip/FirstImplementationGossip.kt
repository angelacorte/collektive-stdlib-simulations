package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.stdlib.fields.fold

fun <ID : Comparable<ID>, Type> Aggregate<ID>.firstGossip(
    env: EnvironmentVariables,
    initial: Type,
    selector: (Type, Type) -> Boolean,
): Type {
    data class GossipValue<ID : Comparable<ID>, Type>(val value: Type, val path: List<ID>)
    val local = GossipValue(initial, listOf(localId))
    return share(local) { gossip ->
        val result = gossip.fold(local) { current, next ->
            when {
                selector(current.value, next.value.value) || localId in next.value.path -> current
                else -> next.value.copy(path = next.value.path + localId)
            }
        }
        env["neighbors-size"] = gossip.neighbors.size
        env["path-length"] = (result.path + localId).size
        result
    }.value
}
