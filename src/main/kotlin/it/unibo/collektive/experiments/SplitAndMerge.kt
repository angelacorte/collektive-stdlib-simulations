package it.unibo.collektive.experiments

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.examples.gossip.NoLoopAgainstNeighborsGossip.noLoopAgainstNeighborsGossip
import it.unibo.collektive.stdlib.processes.timeReplicated
import it.unibo.collektive.stdlib.spreading.nonStabilizingGossip
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Split and Merge experiments by using self-stabilizing gossip that avoids loops against neighbors.
 */
fun Aggregate<Int>.splitAndMergeSelfStabGossip(
    env: EnvironmentVariables,
    timeSensor: TimeSensor,
) = noLoopAgainstNeighborsGossip(
    local = localId,
    selector = selectorBasedOnTime(timeSensor),
).also { env["best-value"] = it }

/**
 * Split and merge experiments by using non stabilizing gossip.
 */
fun Aggregate<Int>.splitAndMergeNonStabGossip(
    env: EnvironmentVariables,
    timeSensor: TimeSensor,
) = nonStabilizingGossip(
    value = localId,
    reducer = selectorBasedOnTime(timeSensor),
).also { env["best-value"] = it }

/**
 * Split and Merge experiments by using time replicated non stabilizing gossip.
 */
@OptIn(ExperimentalTime::class)
fun Aggregate<Int>.splitAndMergeTimeRepGossip(
    env: EnvironmentVariables,
    device: CollektiveDevice<*>,
    timeSensor: TimeSensor,
): Int {
    val currentTime: Instant = Instant.fromEpochSeconds(device.currentTime.toDouble().toLong())
    return timeReplicated(
        currentTime = currentTime,
        maxReplicas = 4,
        timeToSpawn = 3.seconds,
        process = { splitAndMergeNonStabGossip(env, timeSensor) },
    ).also { env["best-value"] = it }
}

fun selectorBasedOnTime(timeSensor: TimeSensor): (Int, Int) -> Int =
    if (timeSensor.getTimeAsDouble() <= 400.0) ::maxOf else ::minOf