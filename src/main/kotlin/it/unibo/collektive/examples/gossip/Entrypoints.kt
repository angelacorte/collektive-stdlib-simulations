package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor

context(TimeSensor, EnvironmentVariables)
fun Aggregate<Int>.isHappeningGossipEntrypoint() = isHappeningGossip {
    (getTimeAsDouble() % 100 < 50) && (localId % 2 == 0)
}

context(EnvironmentVariables, TimeSensor, RandomGenerator)
fun Aggregate<Int>.gossipEntrypoint() = gossipMax(
    randomFromTimeElapsed()
        .also { set("local-value", it) }
) { first, second ->
    second.compareTo(first)
}.also { set("best-value", it) }

context(EnvironmentVariables, TimeSensor, RandomGenerator)
fun Aggregate<Int>.secondGossipEntrypoint() = secondGossip(
    randomFromTimeElapsed()
        .also { set("local-value", it) }
) { first, second ->
    second.compareTo(first)
}.also { set("best-value", it) }

context(EnvironmentVariables, TimeSensor, RandomGenerator)
fun Aggregate<Int>.firstGossipEntrypoint() = firstGossip(
    randomFromTimeElapsed().also { set("local-value", it) } )
{ first, second ->
    first <= second
}.also { set("best-value", it) }
