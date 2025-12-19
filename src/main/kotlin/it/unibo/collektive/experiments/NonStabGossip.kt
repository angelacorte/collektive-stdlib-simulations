package it.unibo.collektive.experiments

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.examples.randomFromTimeElapsed
import it.unibo.collektive.stdlib.spreading.nonStabilizingGossip
import org.apache.commons.math3.random.RandomGenerator

/**
 * Split and merge experiments by using non stabilizing gossip.
 */
fun Aggregate<Int>.nonStabGossip(
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
    selector: (Double, Double) -> Double,
): Double = nonStabilizingGossip(
    value = randomFromTimeElapsed(timeSensor, randomGenerator),
    reducer = selector,
)

fun Aggregate<Int>.minNonStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double =
    nonStabGossip(randomGenerator, timeSensor, ::minOf).also { env["best-value"] = it }

fun Aggregate<Int>.maxNonStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double =
    nonStabGossip(randomGenerator, timeSensor, ::maxOf).also { env["best-value"] = it }

fun Aggregate<Int>.avgNonStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double =
    nonStabGossip(randomGenerator, timeSensor, TODO("Not yet implemented")).also { env["best-value"] = it }

fun Aggregate<Int>.setUnionNonStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double =
    nonStabGossip(randomGenerator, timeSensor, TODO("Not yet implemented")).also { env["best-value"] = it }

