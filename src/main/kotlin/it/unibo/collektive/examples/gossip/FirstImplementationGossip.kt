package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.stdlib.collapse.fold

object FirstImplementationGossip {
    inline fun <reified ID : Comparable<ID>, Type> Aggregate<ID>.firstGossip(
        env: EnvironmentVariables,
        initial: Type,
        crossinline selector: (Type, Type) -> Boolean,
    ): Type {
        val local = GossipValue(initial, listOf(localId))
        return share(local) { gossip ->
            val result =
                gossip.neighbors.fold(local) { current, next ->
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

    data class GossipValue<ID : Comparable<ID>, Type>(
        val value: Type,
        val path: List<ID>,
    )
}
