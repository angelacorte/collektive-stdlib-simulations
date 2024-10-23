package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.operators.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.field.Field.Companion.fold

context(TimeSensor, EnvironmentVariables)
fun Aggregate<Int>.isHappeningGossipEntrypoint() = isHappeningGossip {
    getTimeAsDouble() >= 20
}.also { set("value", it) }

context(TimeSensor)
fun Aggregate<Int>.gossipEntrypoint() =
    when {
        getTimeAsDouble() > 20 -> gossip(-localId) { first, second -> first <= second }
        else -> gossip(localId) { first, second -> first <= second }
    }

data class GossipValue<ID: Comparable<ID>, Value>(val value: Value, val path: List<ID> = emptyList())

fun <ID : Comparable<ID>, Value> Aggregate<ID>.gossip(
    initial: Value,
    selector: (Value, Value) -> Boolean,
): Value {
    val local = GossipValue(initial, emptyList<ID>())
    return share(local) { gossip ->
        gossip.fold(local) { current, next ->
            val selected = when {
                selector(current.value, next.value) || localId in next.path -> current
                else -> next
            }
            selected.copy(path = selected.path + localId)
        }
    }.value
}

fun <ID: Comparable<ID>> Aggregate<ID>.isHappeningGossip(
    condition: () -> Boolean,
): Boolean = gossip(condition()) { _, _ -> condition() }
