package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.AbsoluteTime
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.examples.timeReplication.sharedTimer
import it.unibo.collektive.examples.timeReplication.timeReplicated
import it.unibo.collektive.stdlib.NonSelfStabilizingGossip.nonSelfStabilizingGossip
import it.unibo.collektive.stdlib.SelfStabilizingGossip.gossipMax
import it.unibo.collektive.stdlib.SelfStabilizingGossip.isHappeningAnywhere
import kotlin.time.DurationUnit
import kotlin.time.toDuration

context(TimeSensor, EnvironmentVariables)
fun Aggregate<Int>.isHappeningGossipEntrypoint() = isHappeningAnywhere {
    (getTimeAsDouble() % 100 < 50) && (localId % 2 == 0)
}

fun Aggregate<Int>.sharingTime(absoluteTime: AbsoluteTime) =
    sharedTimer(5.toDuration(DurationUnit.SECONDS), absoluteTime.getDeltaTime())

//context(AbsoluteTime, EnvironmentVariables, RandomGenerator, TimeSensor)
fun Aggregate<Int>.timeReplication(
    absoluteTime: AbsoluteTime,
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
) = timeReplicated(
    timeSensor = absoluteTime,
    process = {
      nonSelfStabilizingGossip(
          randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it }
      ) { first, second -> if (first <= second) first else second }
    },
    default = 42,
    timeToLive = 5.toDuration(DurationUnit.SECONDS),
    maxReplicas = 7,
)

fun Aggregate<Int>.gossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
) = gossipMax(
    randomFromTimeElapsed(timeSensor, randomGenerator)
        .also { env["local-value"] = it }
) { first, second ->
    second.compareTo(first)
}.also { env["best-value"] = it }

fun Aggregate<Int>.secondGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
) = secondGossip(
    env,
    randomFromTimeElapsed(timeSensor, randomGenerator)
        .also { env["local-value"] = it }
) { first, second ->
    second.compareTo(first)
}.also { env["best-value"] = it }

fun Aggregate<Int>.firstGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
) = firstGossip(
    env,
    randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it } )
{ first, second ->
    first <= second
}.also { env["best-value"] = it }
