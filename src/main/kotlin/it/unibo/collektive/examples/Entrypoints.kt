package it.unibo.collektive.examples

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.AbsoluteTime
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.examples.gossip.firstGossip
import it.unibo.collektive.examples.gossip.secondGossip
import it.unibo.collektive.examples.timeReplication.sharedTimer
import it.unibo.collektive.examples.timeReplication.timeReplicated
import it.unibo.collektive.stdlib.NonSelfStabilizingGossip.nonSelfStabilizingGossip
import it.unibo.collektive.stdlib.SelfStabilizingGossip.gossipMax
import it.unibo.collektive.stdlib.SelfStabilizingGossip.isHappeningAnywhere
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Entrypoint for the gossip simulation that uses the gossipMax function defined into Collektive's DSl.
 */
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

/**
 * Entrypoint for the simulation of the first implementation of the gossip algorithm with Collektive.
 */
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

/**
 * Entrypoint for the simulation of the second implementation of the gossip algorithm with Collektive.
 */
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

/**
 * Entrypoint for the simulation of the `isHappening` gossip function defined into Collektive's DSl.
 */
fun Aggregate<Int>.isHappeningGossipEntrypoint(
    timeSensor: TimeSensor,
) = isHappeningAnywhere {
    (timeSensor.getTimeAsDouble() % 100 < 50) && (localId % 2 == 0)
}

/**
 * Entrypoint for the simulation of the `sharingTime` function defined into Collektive's DSl.
 */
fun Aggregate<Int>.sharingTime(absoluteTime: AbsoluteTime) =
    sharedTimer(5.toDuration(DurationUnit.SECONDS), absoluteTime.getDeltaTime())

/**
 * Entrypoint for the simulation of the `timeReplicated` function defined into Collektive's DSl,
 * it replicates the non-self-stabilizing gossip algorithm defined into Collektive's DSl.
 */
fun Aggregate<Int>.timeRep(
    absoluteTime: AbsoluteTime,
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
) = timeReplicated(
    absoluteTime = absoluteTime,
    process = {
        nonSelfStabilizingGossip(
            randomFromTimeElapsed(timeSensor, randomGenerator)
                .also { env["local-value"] = it }
        ) { first, second -> if (first <= second) first else second }
            .also { env["best-value"] = it }
    },
    default = 42,
    timeToLive = 5.toDuration(DurationUnit.SECONDS),
    maxReplicas = 7,
)