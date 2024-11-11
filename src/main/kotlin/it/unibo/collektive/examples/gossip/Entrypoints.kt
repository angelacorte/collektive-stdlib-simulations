package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.AbsoluteTime
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.stdlib.NonSelfStabilizingGossip.nonSelfStabilizingGossip
import it.unibo.collektive.stdlib.SelfStabilizingGossip.gossipMax
import it.unibo.collektive.stdlib.SelfStabilizingGossip.isHappeningAnywhere
import java.time.Duration
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.toKotlinDuration

context(TimeSensor, EnvironmentVariables)
fun Aggregate<Int>.isHappeningGossipEntrypoint() = isHappeningAnywhere {
    (getTimeAsDouble() % 100 < 50) && (localId % 2 == 0)
}

context(AbsoluteTime)
fun Aggregate<Int>.sharingTime() = sharedTimer(5.0, getDeltaTime().toDouble(SECONDS))

context(TimeSensor, EnvironmentVariables, RandomGenerator)
fun Aggregate<Int>.timeReplication() = timeReplicated(
    timeSensor = this@TimeSensor,
    process = {
      nonSelfStabilizingGossip(
          randomFromTimeElapsed().also { set("local-value", it) }
      ) { first, second -> if (first <= second) first else second }
    },
    default = 42,
    timeToLive = Duration.ofSeconds(1L).toKotlinDuration(),
    maxReplicas = 7,
)

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
