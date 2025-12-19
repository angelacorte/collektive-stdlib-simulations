package it.unibo.collektive.experiments

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.stdlib.processes.timeReplicated
import org.apache.commons.math3.random.RandomGenerator
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


/**
 * Split and Merge experiments by using time replicated non stabilizing gossip.
 */
@OptIn(ExperimentalTime::class)
fun Aggregate<Int>.timeRepGossip(
    randomGenerator: RandomGenerator,
    device: CollektiveDevice<*>,
    timeSensor: TimeSensor,
    selector: (Double, Double) -> Double,
): Double {
    val currentTime: Instant = device.getTimeAsInstant()
    return timeReplicated(
        currentTime = currentTime,
        maxReplicas = 4,
        timeToSpawn = 3.seconds,
        process = { nonStabGossip(randomGenerator, timeSensor, selector) }
    )
}

fun Aggregate<Int>.minTimeRepGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    device: CollektiveDevice<*>,
    timeSensor: TimeSensor,
): Double =
    timeRepGossip(randomGenerator, device, timeSensor, ::minOf).also { env["best-value"] = it }

fun Aggregate<Int>.maxTimeRepGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    device: CollektiveDevice<*>,
    timeSensor: TimeSensor,
): Double =
    timeRepGossip(randomGenerator, device, timeSensor, ::maxOf).also { env["best-value"] = it }

fun Aggregate<Int>.avgTimeRepGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    device: CollektiveDevice<*>,
    timeSensor: TimeSensor,
): Double =
    timeRepGossip(randomGenerator, device, timeSensor, TODO("Not yet implemented")).also { env["best-value"] = it }

fun Aggregate<Int>.setUnionTimeRepGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    device: CollektiveDevice<*>,
    timeSensor: TimeSensor,
): Double =
    timeRepGossip(randomGenerator, device, timeSensor, TODO("Not yet implemented")).also { env["best-value"] = it }

